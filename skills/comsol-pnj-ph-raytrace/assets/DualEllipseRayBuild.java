import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;
public class DualEllipseRayBuild {
 static final String ROOT=outputRoot();
 static String outputRoot() {
  String configured=System.getenv("PNJPH_PROJECT_ROOT");
  if(configured==null || configured.trim().isEmpty())
   throw new IllegalStateException("Set PNJPH_PROJECT_ROOT to a new task output directory before running this recipe.");
  File root=new File(configured).getAbsoluteFile();
  for(String sub:new String[]{"models","scripts","logs","validation","figures","data"}) {
   File dir=new File(root,sub);
   if(!dir.isDirectory() && !dir.mkdirs())
    throw new IllegalStateException("Cannot create output directory: "+dir);
  }
  return root.getPath();
 }
 public static Model run() throws IOException {
  Model m=ModelUtil.create("Model");m.label("Dual ellipse ray tracing - core 60 deg - shell 60 deg");m.modelPath(ROOT);
  String[][] pars={{"lambda_ref","532[nm]"},{"n_bg","1"},{"n_core","1.5"},{"n_shell","1.29"},{"a_shell","6*lambda_ref"},{"b_shell","a_shell/2"},{"s_core","0.6"},{"a_core","s_core*a_shell"},{"b_core","a_core/2"},{"theta_core","60[deg]"},{"theta_shell","60[deg]"},{"Eamp","1[V/m]"},{"Lleft","8*lambda_ref"},{"Lright","20*lambda_ref"},{"Ly","7*lambda_ref"},{"h_max","lambda_ref/21"},{"Nrays","201"},{"Nsecondary","2000"},{"x_release","-Lleft+0.1*lambda_ref"},{"y_release","Ly-0.1*lambda_ref"},{"I_inc","0.5*n_bg*epsilon0_const*c_const*Eamp^2"},{"Psrc_2d","I_inc*2*y_release"},{"Qray0","Psrc_2d/Nrays"},{"ray_cutoff","1e-4"}};
  for(String[] p:pars)m.param().set(p[0],p[1]);
  m.param().descr("Psrc_2d","Plane-wave power per unit z thickness over numerical release aperture; derived from Eamp.");
  m.param().descr("ray_cutoff","Secondary reflected rays below this fraction of an initial ray are not released.");
  m.component().create("comp1",true);GeomSequence g=m.component("comp1").geom().create("geom1",2);g.lengthUnit("um");
  g.create("box","Rectangle");g.feature("box").set("pos",new String[]{"-Lleft","-Ly"});g.feature("box").set("size",new String[]{"Lleft+Lright","2*Ly"});
  for(String t:new String[]{"shell","core"}){g.create(t,"Ellipse");g.feature(t).set("semiaxes",new String[]{"a_"+t,"b_"+t});g.feature(t).set("rot","theta_"+t);g.feature(t).set("selresult",true);}
  g.run();
  m.component("comp1").selection().create("shellOnly","Difference");m.component("comp1").selection("shellOnly").set("entitydim",2);m.component("comp1").selection("shellOnly").set("add",new String[]{"geom1_shell_dom"});m.component("comp1").selection("shellOnly").set("subtract",new String[]{"geom1_core_dom"});
  m.component("comp1").selection().create("airDomains","Complement");m.component("comp1").selection("airDomains").set("entitydim",2);m.component("comp1").selection("airDomains").set("input",new String[]{"geom1_shell_dom"});
  String[][] faces={{"left","-Lleft-0.001[um]","-Lleft+0.001[um]","-Ly-0.001[um]","Ly+0.001[um]"},{"right","Lright-0.001[um]","Lright+0.001[um]","-Ly-0.001[um]","Ly+0.001[um]"},{"top","-Lleft-0.001[um]","Lright+0.001[um]","Ly-0.001[um]","Ly+0.001[um]"},{"bottom","-Lleft-0.001[um]","Lright+0.001[um]","-Ly-0.001[um]","-Ly+0.001[um]"}};
  for(String[] f:faces){m.component("comp1").selection().create(f[0],"Box");m.component("comp1").selection(f[0]).set("entitydim",1);m.component("comp1").selection(f[0]).set("condition","inside");m.component("comp1").selection(f[0]).set("xmin",f[1]);m.component("comp1").selection(f[0]).set("xmax",f[2]);m.component("comp1").selection(f[0]).set("ymin",f[3]);m.component("comp1").selection(f[0]).set("ymax",f[4]);}
  m.component("comp1").selection().create("outerBoundaries","Union");m.component("comp1").selection("outerBoundaries").set("entitydim",1);m.component("comp1").selection("outerBoundaries").set("input",new String[]{"left","right","top","bottom"});
  String[] labels={"Background n=1","Shell n=1.29","Core n=1.5"},sels={"airDomains","shellOnly","geom1_core_dom"},ns={"n_bg","n_shell","n_core"};
  for(int i=0;i<3;i++){String tag="mat"+(i+1);m.component("comp1").material().create(tag,"Common");m.component("comp1").material(tag).label(labels[i]);m.component("comp1").material(tag).selection().named(sels[i]);m.component("comp1").material(tag).propertyGroup().create("RefractiveIndex","RefractiveIndex","Refractive_index");m.component("comp1").material(tag).propertyGroup("RefractiveIndex").set("n",new String[]{ns[i]});m.component("comp1").material(tag).propertyGroup("RefractiveIndex").set("ki",new String[]{"0"});}
  m.component("comp1").physics().create("gop","GeometricalOptics","geom1");
  m.component("comp1").physics("gop").prop("IntensityComputation").set("IntensityComputation","ComputePower");
  m.component("comp1").physics("gop").prop("MaximumSecondary").set("MaximumSecondary","Nsecondary");
  m.component("comp1").physics("gop").prop("UseGeometryNormals").set("UseGeometryNormals",true);
  m.component("comp1").physics("gop").prop("CountReflections").set("CountReflections",true);
  m.component("comp1").physics("gop").prop("StoreRayStatusData").set("StoreRayStatusData",true);
  m.component("comp1").physics("gop").prop("ComputeOpticalPathLength").set("ComputeOpticalPathLength",true);
  m.component("comp1").physics("gop").prop("ExteriorUnmeshedProperties").set("next","n_bg");
  m.component("comp1").physics("gop").feature("op1").set("lambda0","lambda_ref");
  m.component("comp1").physics("gop").feature("mp1").set("n_mat","from_mat");
  m.component("comp1").physics("gop").feature("matd1").set("Qth","ray_cutoff*Qray0");
  m.component("comp1").physics("gop").create("relg1","ReleaseGrid",-1);
  m.component("comp1").physics("gop").feature("relg1").set("x0",new String[]{"x_release","range(-y_release,2*y_release/(Nrays-1),y_release)"});
  m.component("comp1").physics("gop").feature("relg1").set("L0",new String[]{"1","0","0"});
  m.component("comp1").physics("gop").feature("relg1").set("Psrc","Psrc_2d");
  m.component("comp1").physics("gop").feature("relg1").set("InitialPolarizationType","FullyPolarized");
  m.component("comp1").physics("gop").feature("relg1").set("axy0",1);m.component("comp1").physics("gop").feature("relg1").set("az0",0);
  m.component("comp1").physics("gop").create("wall1","Wall",1);m.component("comp1").physics("gop").feature("wall1").selection().named("outerBoundaries");m.component("comp1").physics("gop").feature("wall1").set("WallCondition","Disappear");
  MeshSequence mesh=m.component("comp1").mesh().create("mesh1");mesh.feature("size").set("custom",true);mesh.feature("size").set("hmax","h_max");mesh.feature("size").set("hmin","h_max/8");mesh.feature("size").set("hgrad",1.2);mesh.create("ftri1","FreeTri");mesh.run();
  m.study().create("std1");m.study("std1").label("Ray tracing - core and shell fixed 60 deg");m.study("std1").create("rt","RayTracing");
  m.study("std1").feature("rt").set("timestepspec","specifylength");m.study("std1").feature("rt").set("llist","range(0[um],0.05[um],25[um])");m.study("std1").feature("rt").set("charvel","c_const");m.study("std1").feature("rt").set("usertol",true);m.study("std1").feature("rt").set("rtol","1e-6");
  m.study("std1").createAutoSequences("all");m.save(ROOT+"\\models\\DualEllipse_Core60_Shell60_RayTracing.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\settings.txt")){
   for(String[] p:pars)out.println(p[0]+"="+m.param().get(p[0]));
   for(String s:sels)out.println(s+"="+Arrays.toString(m.component("comp1").selection(s).entities(2)));
   out.println("outerBoundaries="+Arrays.toString(m.component("comp1").selection("outerBoundaries").entities(1)));
   out.println("Ray properties lambda0="+m.component("comp1").physics("gop").feature("op1").getString("lambda0"));
   out.println("Settings complete; calculation will be executed through COMSOL MCP.");
  }
  System.out.println("RAY_SETTINGS_SAVED");return m;
 }
 public static void main(String[] args) throws IOException {run();}
}
