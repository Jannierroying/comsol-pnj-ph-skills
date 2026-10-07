import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Xu et al. Applied Optics 63(29),7735 (2024), Fig4, d=10lambda, phase=0. */
public class PNJChainBuild {
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
 static Model build(boolean control,String bottom,String top) {
  Model m=ModelUtil.create("Model");m.modelPath(ROOT);m.label(control?"EmptyAir_Port_Check":"Xu2024_Fig4_Coaxial_PNJ_Chain");
  String[][] ps={{"lambda_ref","532[nm]"},{"n_bg","1"},{"n_cyl",control?"1":"1.33"},
   {"rc","6.36*lambda_ref"},{"lc","4*lambda_ref"},{"gap","10*lambda_ref"},{"omega","9*lambda_ref"},
   {"delta_phi","0[deg]"},{"Epeak","1[V/m]"},{"Abeam","sqrt(2*exp(1))*Epeak/omega"},
   {"Rair","2.5*omega"},{"Zmin","-lc-lambda_ref"},{"Zmax","gap+lc+lambda_ref"},
   {"t_pml","lambda_ref"},{"h_air",control?"lambda_ref/12":"lambda_ref/45"},
   {"h_cyl","h_air/n_cyl"},{"Ipeak","0.5*n_bg*epsilon0_const*c_const*Epeak^2"},
   {"Pbeam","Ipeak*pi*exp(1)*omega^2/2*(1-exp(-2*(Rair/omega)^2)*(1+2*(Rair/omega)^2))"}};
  for(String[] p:ps)m.param().set(p[0],p[1]);
  m.param().descr("Epeak","Supplemented normalization: each incident beam peak field is 1 V/m");
  m.param().descr("gap","Paper d, distance between the facing exit surfaces, not cylinder center distance");
  m.param().descr("delta_phi","Relative mode phase of simultaneous upper and lower beams; fixed zero for Fig4");
  m.component().create("comp1",true);
  GeomSequence g=m.component("comp1").geom().create("geom1",2);g.axisymmetric(true);g.lengthUnit("um");
  g.create("outer","Rectangle");g.feature("outer").set("pos",new String[]{"0","Zmin-t_pml"});
  g.feature("outer").set("size",new String[]{"Rair+t_pml","Zmax-Zmin+2*t_pml"});
  g.feature("outer").setIndex("layer","t_pml",0);g.feature("outer").set("layerleft",false);
  for(String side:new String[]{"right","top","bottom"})g.feature("outer").set("layer"+side,true);
  for(String t:new String[]{"cylL","cylR"}){
   g.create(t,"Rectangle");g.feature(t).set("pos",new String[]{"0",t.equals("cylL")?"-lc":"gap"});
   g.feature(t).set("size",new String[]{"rc","lc"});g.feature(t).set("selresult",true);
  }
  g.run();
  m.component("comp1").selection().create("physical","Box");
  m.component("comp1").selection("physical").set("entitydim",2);m.component("comp1").selection("physical").set("condition","inside");
  m.component("comp1").selection("physical").set("xmin","-0.001[um]");m.component("comp1").selection("physical").set("xmax","Rair+0.001[um]");
  m.component("comp1").selection("physical").set("ymin","Zmin-0.001[um]");m.component("comp1").selection("physical").set("ymax","Zmax+0.001[um]");
  m.component("comp1").selection().create("cylinders","Union");m.component("comp1").selection("cylinders").set("entitydim",2);
  m.component("comp1").selection("cylinders").set("input",new String[]{"geom1_cylL_dom","geom1_cylR_dom"});
  for(String t:new String[]{"air","pml"}){
   m.component("comp1").selection().create(t,"Complement");m.component("comp1").selection(t).set("entitydim",2);
   m.component("comp1").selection(t).set("input",new String[]{t.equals("air")?"cylinders":"physical"});
  }
  for(String t:new String[]{"portBottom","portTop"}){
   String z=t.equals("portBottom")?"Zmin":"Zmax";
   m.component("comp1").selection().create(t,"Box");m.component("comp1").selection(t).set("entitydim",1);
   m.component("comp1").selection(t).set("condition","inside");m.component("comp1").selection(t).set("xmin","-0.001[um]");
   m.component("comp1").selection(t).set("xmax","Rair+0.001[um]");m.component("comp1").selection(t).set("ymin",z+"-0.001[um]");
   m.component("comp1").selection(t).set("ymax",z+"+0.001[um]");
   if(m.component("comp1").selection(t).entities(1).length!=1)throw new IllegalStateException("Port boundary selection invalid: "+t);
  }
  m.component("comp1").coordSystem().create("pml1","PML");m.component("comp1").coordSystem("pml1").selection().named("pml");
  m.component("comp1").coordSystem("pml1").set("ScalingType","Cylindrical");
  m.component("comp1").variable().create("var1");m.component("comp1").variable("var1").set("E_profile","Abeam*r*exp(-(r/omega)^2)");
  m.component("comp1").physics().create("ewfd","ElectromagneticWavesFrequencyDomain","geom1");
  m.component("comp1").physics("ewfd").prop("components").set("components","inplane");
  m.component("comp1").physics("ewfd").prop("outofplanewavenumber").set("mFloquet","0");
  m.component("comp1").physics("ewfd").prop("ShapeProperty").set("order_electricfield","1");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("SolveFor","fullField");
  m.component("comp1").physics("ewfd").prop("PortSweepSettings").set("useSweep",false);
  m.component("comp1").physics("ewfd").feature("wee1").set("DisplacementFieldModel","RelativePermittivity");
  for(int j=0;j<2;j++){
   String t="port"+(j+1);m.component("comp1").physics("ewfd").create(t,"Port",1);
   m.component("comp1").physics("ewfd").feature(t).selection().named(j==0?"portBottom":"portTop");
   m.component("comp1").physics("ewfd").feature(t).set("PortName",Integer.toString(j+1));
   m.component("comp1").physics("ewfd").feature(t).set("PortType","UserDefined");
   m.component("comp1").physics("ewfd").feature(t).set("PortSlit",true);
   m.component("comp1").physics("ewfd").feature(t).set("SlitType","DomainBacked");
   m.component("comp1").physics("ewfd").feature(t).set("PortOrientation",j==0?bottom:top);
   m.component("comp1").physics("ewfd").feature(t).set("PortExcitation","on");
   m.component("comp1").physics("ewfd").feature(t).set("InputType","E");
   m.component("comp1").physics("ewfd").feature(t).set("E0",new String[]{"E_profile","0","0"});
   m.component("comp1").physics("ewfd").feature(t).set("beta","2*pi*n_bg*freq/c_const");
   m.component("comp1").physics("ewfd").feature(t).set("Pin","Pbeam");
   m.component("comp1").physics("ewfd").feature(t).set("Thetap",j==0?"0[deg]":"delta_phi");
  }
  for(int j=0;j<2;j++){
   String t="mat"+(j+1),n=j==0?"n_bg":"n_cyl";m.component("comp1").material().create(t,"Common");
   m.component("comp1").material(t).label(j==0?"Air including PML":"Two finite-length lossless cylinders");
   m.component("comp1").material(t).selection().named(j==0?"air":"cylinders");
   m.component("comp1").material(t).propertyGroup("def").set("relpermittivity",new String[]{n+"^2"});
   m.component("comp1").material(t).propertyGroup("def").set("relpermeability",new String[]{"1"});
   m.component("comp1").material(t).propertyGroup("def").set("electricconductivity",new String[]{"0"});
  }
  MeshSequence mesh=m.component("comp1").mesh().create("mesh1");mesh.feature("size").set("custom",true);
  mesh.feature("size").set("hmax","h_air");mesh.feature("size").set("hmin","h_air/8");mesh.feature("size").set("hgrad",1.15);
  mesh.create("ftri1","FreeTri");mesh.feature("ftri1").selection().named("physical");
  mesh.feature("ftri1").create("sizeCyl","Size");mesh.feature("ftri1").feature("sizeCyl").selection().named("cylinders");
  mesh.feature("ftri1").feature("sizeCyl").set("custom",true);mesh.feature("ftri1").feature("sizeCyl").set("hmax","h_cyl");
  mesh.feature("ftri1").feature("sizeCyl").set("hmin","h_cyl/8");
  mesh.create("map1","Map");mesh.feature("map1").selection().named("pml");mesh.run();
  m.study().create("std1");m.study("std1").label(control?"Empty-air port direction validation":"Fig4 - d=10lambda, phase difference zero");
  m.study("std1").create("wave","Wavelength");m.study("std1").feature("wave").set("plist","lambda_ref");
  m.study("std1").createAutoSequences("all");
  for(String s:m.sol().tags())for(String f:m.sol(s).feature().tags())if(m.sol(s).feature(f).getType().equals("Stationary"))
   for(String d:m.sol(s).feature(f).feature().tags())if(m.sol(s).feature(f).feature(d).getType().equals("Direct")){
    m.sol(s).feature(f).feature(d).set("linsolver","pardiso");m.sol(s).feature(f).feature(d).set("ooc","on");m.sol(s).feature(f).feature(d).set("oocmemory",2048);
   }
  return m;
 }
 static double checkFlow(Model m,String label,PrintWriter out) {
  if(!Arrays.asList(m.result().numerical().tags()).contains("flowCheck"))m.result().numerical().create("flowCheck","Interp");
  m.result().numerical("flowCheck").set("data","dset1");m.result().numerical("flowCheck").set("expr",new String[]{"ewfd.Poavz","ewfd.normE^2"});
  double ring=m.param().evaluate("omega")/Math.sqrt(2)*1e6,lam=m.param().evaluate("lambda_ref")*1e6;
  m.result().numerical("flowCheck").setInterpolationCoordinates(new double[][]{{ring,ring,ring},{-4*lam,5*lam,14*lam}});
  double[][][] a=m.result().numerical("flowCheck").getData();double mean=0;
  for(int k=0;k<3;k++){out.println(label+" z_um="+new double[]{-4*lam,5*lam,14*lam}[k]+" Sz_W_m2="+a[0][0][k]+" E2_V2_m2="+a[1][0][k]);mean+=a[0][0][k]/3;}
  out.flush();return mean;
 }
 public static Model run() throws IOException {
  String b="ForwardPort",t="ForwardPort";Model m=build(true,b,t);
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\port_direction_check.txt")){
   out.println("Empty air n_cyl=n_bg=1, coarse validation mesh; only one port excited per check. Not a target-result model.");
   m.component("comp1").physics("ewfd").feature("port2").set("PortExcitation","off");m.study("std1").run();
   double sb=checkFlow(m,"bottom_Forward",out);
   if(sb<0.0002){b="ReversePort";m.component("comp1").physics("ewfd").feature("port1").set("PortOrientation",b);m.study("std1").run();sb=checkFlow(m,"bottom_Reverse",out);}
   if(!(sb>0.0002))throw new IllegalStateException("Bottom source does not propagate into +z");
   m.component("comp1").physics("ewfd").feature("port1").set("PortExcitation","off");m.component("comp1").physics("ewfd").feature("port2").set("PortExcitation","on");m.study("std1").run();
   double st=checkFlow(m,"top_Forward",out);
   if(st>-0.0002){t="ReversePort";m.component("comp1").physics("ewfd").feature("port2").set("PortOrientation",t);m.study("std1").run();st=checkFlow(m,"top_Reverse",out);}
   if(!(st<-0.0002))throw new IllegalStateException("Top source does not propagate into -z");
   out.println("VALIDATED bottom="+b+" top="+t+". Both enabled together in target model, no port sweep.");
  }
  ModelUtil.remove("Model");System.gc();m=build(false,b,t);
  MeshSequence mesh=m.component("comp1").mesh("mesh1");double[][] v=mesh.getVertex();int[][] tri=mesh.getElem("tri");double max=0;
  for(int k=0;k<tri[0].length;k++)for(int j=0;j<3;j++){int a=tri[j][k],c=tri[(j+1)%3][k];max=Math.max(max,Math.hypot(v[0][a]-v[0][c],v[1][a]-v[1][c]));}
  if(Math.abs(Arrays.stream(v[0]).max().getAsDouble()-(m.param().evaluate("Rair")+m.param().evaluate("t_pml"))*1e6)>1e-5)throw new IllegalStateException("Unexpected mesh units");
  double bound=m.param().evaluate("lambda_ref")*1e6/30;
  if(max>=bound)throw new IllegalStateException("Actual max triangle edge "+max+" um >= lambda/30 "+bound);
  m.result().create("pgField","PlotGroup2D");m.result("pgField").label("PNJ-chain - total electric field squared");m.result("pgField").set("data","dset1");
  m.result("pgField").create("surf1","Surface");m.result("pgField").feature("surf1").set("expr","ewfd.normE^2");
  m.result("pgField").feature("surf1").set("unit","V^2/m^2");m.result("pgField").feature("surf1").set("colortable","Rainbow");
  m.result("pgField").feature("surf1").create("sel1","Selection");m.result("pgField").feature("surf1").feature("sel1").selection().set(m.component("comp1").selection("physical").entities(2));
  m.save(ROOT+"\\models\\Xu2024_Fig4_PNJChain.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\settings_and_mesh.txt")){
   out.println("Target model built, not computed here. Empty-air excitation controls computed and verified separately.");
   out.println("Source DOI 10.1364/AO.539726, Fig4(a), Eq1. Axisymmetric r,z, m=0, Er/Ez, full field.");
   for(String p:m.param().varnames())out.println(p+"="+m.param().get(p));
   out.println("Bottom orientation="+b+"; top orientation="+t+"; both UserDefined DomainBacked slit ports excited simultaneously.");
   out.println("Beam shape="+m.component("comp1").variable("var1").get("E_profile")+"; E0={E_profile,0,0} in (r,phi,z)");
   out.println("Per-port power [W]="+m.param().evaluate("Pbeam"));
   out.println("Triangles="+tri[0].length+"; PML quads="+mesh.getElem("quad")[0].length+"; max triangle edge [um]="+max+"; paper limit [um]="+bound);
   for(String s:new String[]{"physical","cylinders","pml","portBottom","portTop"})out.println(s+"="+Arrays.toString(m.component("comp1").selection(s).entities(s.startsWith("port")?1:2)));
   out.println("Supplemented: domain size, PML thickness, port positions, peak amplitude normalization, first-order edge elements, solver memory settings.");
   out.println("No full convergence verification, no fitted hotspot metrics or optical forces.");
  }
  System.out.println("TARGET_MODEL_BUILT triangles="+tri[0].length+" maxEdge_um="+max+" bottom="+b+" top="+t);return m;
 }
 public static void main(String[] args)throws IOException{run();}
}
