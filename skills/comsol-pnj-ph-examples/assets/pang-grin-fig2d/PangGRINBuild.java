import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Pang et al. Opt. Lett. 50(16), 4882 (2025), Fig. 2(d), air, theta=0. */
public class PangGRINBuild {
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
  Model m=ModelUtil.create("Model");m.label("Pang2025_Fig2d_GRIN_Air");m.modelPath(ROOT);
  String[][] ps={{"lambda_ref","633[nm]"},{"R","7.5[um]"},{"n_bg","1"},
   {"n1","1.72"},{"delta_n","0.3"},{"n2","n1-delta_n"},{"theta","0[deg]"},
   {"Eamp","1[V/m]"},{"Lleft","R+2*lambda_ref"},{"Lright","R+6*lambda_ref"},
   {"Ly","R+2*lambda_ref"},{"t_pml","lambda_ref"},{"h_max","lambda_ref/32"}};
  for(String[] p:ps)m.param().set(p[0],p[1]);
  m.param().descr("n1","Paper n1: lower surface y=-R, maximum refractive index");
  m.param().descr("n2","Paper n2: upper surface y=+R, minimum refractive index");
  m.param().descr("theta","Fig. 2(d): index decrease along +y, no rotation or parameter sweep");
  m.param().descr("Eamp","Supplement: incident electric-field amplitude; squared field equals normalized intensity numerically for 1 V/m");
  m.component().create("comp1",true);
  GeomSequence g=m.component("comp1").geom().create("geom1",2);g.lengthUnit("um");
  g.create("outer","Rectangle");g.feature("outer").set("pos",new String[]{"-Lleft-t_pml","-Ly-t_pml"});
  g.feature("outer").set("size",new String[]{"Lleft+Lright+2*t_pml","2*Ly+2*t_pml"});
  g.feature("outer").setIndex("layer","t_pml",0);
  for(String side:new String[]{"left","right","top","bottom"})g.feature("outer").set("layer"+side,true);
  g.create("particle","Circle");g.feature("particle").set("r","R");g.feature("particle").set("selresult",true);g.run();
  m.component("comp1").selection().create("physical","Box");
  m.component("comp1").selection("physical").set("entitydim",2);m.component("comp1").selection("physical").set("condition","inside");
  m.component("comp1").selection("physical").set("xmin","-Lleft-0.001[um]");
  m.component("comp1").selection("physical").set("xmax","Lright+0.001[um]");
  m.component("comp1").selection("physical").set("ymin","-Ly-0.001[um]");
  m.component("comp1").selection("physical").set("ymax","Ly+0.001[um]");
  for(String s:new String[]{"pmlDomains","airDomains"}){
   m.component("comp1").selection().create(s,"Complement");m.component("comp1").selection(s).set("entitydim",2);
   m.component("comp1").selection(s).set("input",new String[]{s.equals("pmlDomains")?"physical":"geom1_particle_dom"});
  }
  int[] pml=m.component("comp1").selection("pmlDomains").entities(2);
  if(pml.length!=8)throw new IllegalStateException("Expected eight Cartesian PML domains: "+Arrays.toString(pml));
  m.component("comp1").coordSystem().create("pml1","PML");m.component("comp1").coordSystem("pml1").selection().named("pmlDomains");
  m.component("comp1").coordSystem("pml1").set("ScalingType","Cartesian");
  m.component("comp1").variable().create("var1");
  m.component("comp1").variable("var1").set("k_bg","2*pi*n_bg*freq/c_const");
  m.component("comp1").variable("var1").set("n_grin","n1-delta_n*(y+R)/(2*R)","Paper Eq. (1), theta=0: continuous downward increasing index, used only inside cylinder");
  m.component("comp1").physics().create("ewfd","ElectromagneticWavesFrequencyDomain","geom1");
  m.component("comp1").physics("ewfd").prop("components").set("components","inplane");
  m.component("comp1").physics("ewfd").prop("ShapeProperty").set("order_electricfield","1");
  m.component("comp1").physics("ewfd").feature("wee1").set("DisplacementFieldModel","RelativePermittivity");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("SolveFor","scatteredField");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("Eb",new String[]{"0","Eamp*exp(-i*k_bg*x)","0"});
  for(int i=0;i<2;i++){
   String tag="mat"+(i+1),index=i==0?"n_bg":"n_grin";
   m.component("comp1").material().create(tag,"Common");m.component("comp1").material(tag).label(i==0?"Air including PML":"Continuous GRIN cylinder: n(y)");
   m.component("comp1").material(tag).selection().named(i==0?"airDomains":"geom1_particle_dom");
   m.component("comp1").material(tag).propertyGroup("def").set("relpermittivity",new String[]{index+"^2"});
   m.component("comp1").material(tag).propertyGroup("def").set("relpermeability",new String[]{"1"});
   m.component("comp1").material(tag).propertyGroup("def").set("electricconductivity",new String[]{"0"});
  }
  MeshSequence mesh=m.component("comp1").mesh().create("mesh1");
  mesh.feature("size").set("custom",true);mesh.feature("size").set("hmax","h_max");
  mesh.feature("size").set("hmin","h_max/8");mesh.feature("size").set("hgrad",1.15);
  mesh.create("ftri1","FreeTri");mesh.feature("ftri1").selection().named("physical");
  mesh.create("map1","Map");mesh.feature("map1").selection().named("pmlDomains");mesh.run();
  double[][] v=mesh.getVertex();int[][] tri=mesh.getElem("tri");double maxEdge=0;
  for(int k=0;k<tri[0].length;k++)for(int j=0;j<3;j++){
   int a=tri[j][k],b=tri[(j+1)%3][k];maxEdge=Math.max(maxEdge,Math.hypot(v[0][a]-v[0][b],v[1][a]-v[1][b]));
  }
  double expectedXmin=-(m.param().evaluate("Lleft")+m.param().evaluate("t_pml"))*1e6;
  if(Math.abs(Arrays.stream(v[0]).min().getAsDouble()-expectedXmin)>1e-5)throw new IllegalStateException("Unexpected mesh coordinate units");
  double limit=m.param().evaluate("lambda_ref")*1e6/20;
  if(maxEdge>=limit)throw new IllegalStateException("Actual triangle edge "+maxEdge+" um exceeds paper limit "+limit);
  m.study().create("std1");m.study("std1").label("Pang Fig2d - single GRIN case");
  m.study("std1").create("wave","Wavelength");m.study("std1").feature("wave").set("plist","lambda_ref");
  m.study("std1").createAutoSequences("all");
  for(String s:m.sol().tags())for(String f:m.sol(s).feature().tags()){
   if(m.sol(s).feature(f).getType().equals("Stationary"))for(String d:m.sol(s).feature(f).feature().tags()){
    if(m.sol(s).feature(f).feature(d).getType().equals("Direct")){
     m.sol(s).feature(f).feature(d).set("linsolver","pardiso");
     m.sol(s).feature(f).feature(d).set("ooc","on");
     m.sol(s).feature(f).feature(d).set("oocmemory",2048);
     System.out.println("DIRECT_SOLVER "+s+"/"+f+"/"+d+" PARDISO out-of-core 2048 MB");
    }
   }
  }
  m.result().create("pgField","PlotGroup2D");m.result("pgField").label("Fig2d GRIN - electric field squared");
  m.result("pgField").set("data","dset1");m.result("pgField").create("surf1","Surface");
  m.result("pgField").feature("surf1").set("expr","ewfd.normE^2");
  m.result("pgField").feature("surf1").set("colortable","Rainbow");
  m.result("pgField").feature("surf1").create("sel1","Selection");m.result("pgField").feature("surf1").feature("sel1").selection().set(m.component("comp1").selection("physical").entities(2));
  m.save(ROOT+"\\models\\Pang2025_Fig2d_GRIN.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\settings_and_mesh.txt")){
   out.println("BUILD ONLY; computation follows through COMSOL MCP.");out.println("Source: DOI 10.1364/OL.564490, Fig. 2(d), Eq. (1)");
   for(String[] q:ps)out.println(q[0]+"="+m.param().get(q[0]));
   out.println("n_grin="+m.component("comp1").variable("var1").get("n_grin"));
   out.println("Incident E={0,Eamp*exp(-i*k_bg*x),0}, propagation +x, time convention exp(+i*omega*t)");
   out.println("In-plane electric field, first-order edge elements (supplemented order; paper does not specify)");
   out.println("Triangle count="+tri[0].length);out.println("PML quad count="+mesh.getElem("quad")[0].length);
   out.println("Actual max triangle edge [um]="+maxEdge);out.println("Paper mesh upper limit lambda/20 [um]="+limit);
   out.println("PML domains="+Arrays.toString(pml));out.println("Cylinder domains="+Arrays.toString(m.component("comp1").selection("geom1_particle_dom").entities(2)));
   out.println("One wavelength, one geometry, no parametric scan. Surface=ewfd.normE^2, Rainbow.");
   out.println("Supplemented: computational extent, PML thickness, amplitude, lossless nonmagnetic material, element order, solver memory strategy.");
  }
  System.out.println("BUILD_OK triangles="+tri[0].length+" maxEdge_um="+maxEdge);return m;
 }
 public static void main(String[] args) throws IOException {run();}
}
