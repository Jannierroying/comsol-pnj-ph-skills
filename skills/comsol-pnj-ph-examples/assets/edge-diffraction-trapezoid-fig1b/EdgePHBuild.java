import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Xu et al., J. Phys. D 57 295104 (2024), Fig. 1(b); build without computing. */
public class EdgePHBuild {
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
  Model m=ModelUtil.create("Model");m.label("Xu2024_Fig1b_EdgeDiffraction_PH");m.modelPath(ROOT);
  String[][] ps={{"lambda_ref","1[um]"},{"L","3*lambda_ref"},{"alpha","atan(1/3)"},
   {"n_particle","1.46"},{"n_bg","1"},{"Eamp","1[V/m]"},
   {"wedge","L*tan(alpha)"},{"Xmin","-L-wedge-2*lambda_ref"},{"Xmax","8*lambda_ref"},
   {"Yhalf","L/2+2*lambda_ref"},{"t_pml","lambda_ref"},
   {"h_air","lambda_ref/45"},{"h_particle","h_air/n_particle"}};
  for(String[] p:ps)m.param().set(p[0],p[1]);
  m.param().set("alpha","18.43494882292201[deg]");
  m.param().descr("L","Paper Fig1(e): top horizontal side and vertical height, NOT longest bottom side");
  m.param().descr("alpha","Paper Fig1(b): tan(alpha)=1/3; angle between incident sloping face and vertical");
  m.param().descr("Eamp","Paper normalized incident |E0|^2=1; physical amplitude chosen as 1 V/m");
  m.component().create("comp1",true);
  GeomSequence g=m.component("comp1").geom().create("geom1",2);g.lengthUnit("um");
  g.create("outer","Rectangle");g.feature("outer").set("pos",new String[]{"Xmin-t_pml","-Yhalf-t_pml"});
  g.feature("outer").set("size",new String[]{"Xmax-Xmin+2*t_pml","2*Yhalf+2*t_pml"});
  g.feature("outer").setIndex("layer","t_pml",0);
  for(String s:new String[]{"left","right","top","bottom"})g.feature("outer").set("layer"+s,true);
  g.create("particle","Polygon");g.feature("particle").set("source","vectors");
  g.feature("particle").set("x",new String[]{"-L-wedge","0","0","-L"});
  g.feature("particle").set("y",new String[]{"-L/2","-L/2","L/2","L/2"});
  g.feature("particle").set("selresult",true);g.run();
  m.component("comp1").selection().create("physical","Box");
  m.component("comp1").selection("physical").set("entitydim",2);
  m.component("comp1").selection("physical").set("condition","inside");
  m.component("comp1").selection("physical").set("xmin","Xmin-0.001[um]");
  m.component("comp1").selection("physical").set("xmax","Xmax+0.001[um]");
  m.component("comp1").selection("physical").set("ymin","-Yhalf-0.001[um]");
  m.component("comp1").selection("physical").set("ymax","Yhalf+0.001[um]");
  for(String s:new String[]{"pmlDomains","airDomains"}){
   m.component("comp1").selection().create(s,"Complement");
   m.component("comp1").selection(s).set("entitydim",2);
   m.component("comp1").selection(s).set("input",new String[]{s.equals("pmlDomains")?"physical":"geom1_particle_dom"});
  }
  int[] physical=m.component("comp1").selection("physical").entities(2);
  int[] particle=m.component("comp1").selection("geom1_particle_dom").entities(2);
  int[] pml=m.component("comp1").selection("pmlDomains").entities(2);
  if(physical.length!=2 || particle.length!=1 || pml.length!=8)
   throw new IllegalStateException("Unexpected domain selections: physical="+Arrays.toString(physical)+", particle="+Arrays.toString(particle)+", PML="+Arrays.toString(pml));
  m.component("comp1").coordSystem().create("pml1","PML");
  m.component("comp1").coordSystem("pml1").selection().named("pmlDomains");
  m.component("comp1").coordSystem("pml1").set("ScalingType","Cartesian");
  m.component("comp1").variable().create("var1");
  m.component("comp1").variable("var1").set("k_bg","2*pi*n_bg*freq/c_const");
  m.component("comp1").physics().create("ewfd","ElectromagneticWavesFrequencyDomain","geom1");
  m.component("comp1").physics("ewfd").prop("components").set("components","inplane");
  m.component("comp1").physics("ewfd").prop("ShapeProperty").set("order_electricfield","2");
  m.component("comp1").physics("ewfd").feature("wee1").set("DisplacementFieldModel","RelativePermittivity");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("SolveFor","scatteredField");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("Eb",new String[]{"0","Eamp*exp(-i*k_bg*x)","0"});
  for(int i=0;i<2;i++){
   String tag="mat"+(i+1),n=i==0?"n_bg":"n_particle";
   m.component("comp1").material().create(tag,"Common");
   m.component("comp1").material(tag).label(i==0?"Air and PML":"Lossless dielectric trapezoid, n=1.46");
   m.component("comp1").material(tag).selection().named(i==0?"airDomains":"geom1_particle_dom");
   m.component("comp1").material(tag).propertyGroup("def").set("relpermittivity",new String[]{n+"^2"});
   m.component("comp1").material(tag).propertyGroup("def").set("relpermeability",new String[]{"1"});
   m.component("comp1").material(tag).propertyGroup("def").set("electricconductivity",new String[]{"0"});
  }
  MeshSequence mesh=m.component("comp1").mesh().create("mesh1");
  mesh.feature("size").set("custom",true);mesh.feature("size").set("hmax","h_air");
  mesh.feature("size").set("hmin","h_air/8");mesh.feature("size").set("hgrad",1.15);
  mesh.create("sizeParticle","Size");mesh.feature("sizeParticle").selection().named("geom1_particle_dom");
  mesh.feature("sizeParticle").set("custom",true);
  mesh.feature("sizeParticle").set("hmaxactive",true);mesh.feature("sizeParticle").set("hmax","h_particle");
  mesh.feature("sizeParticle").set("hminactive",true);mesh.feature("sizeParticle").set("hmin","h_particle/8");
  mesh.feature("sizeParticle").set("hgradactive",true);mesh.feature("sizeParticle").set("hgrad",1.15);
  mesh.create("ftri1","FreeTri");mesh.feature("ftri1").selection().named("physical");
  mesh.create("map1","Map");mesh.feature("map1").selection().named("pmlDomains");mesh.run();
  double[][] v=mesh.getVertex();int[][] tri=mesh.getElem("tri");int[] edom=mesh.getElemEntity("tri");
  double maxEdge=0,maxParticle=0;
  for(int k=0;k<tri[0].length;k++)for(int j=0;j<3;j++){
   int a=tri[j][k],b=tri[(j+1)%3][k];double edge=Math.hypot(v[0][a]-v[0][b],v[1][a]-v[1][b]);
   maxEdge=Math.max(maxEdge,edge);if(edom[k]==particle[0])maxParticle=Math.max(maxParticle,edge);
  }
  double expected=m.param().evaluate("Xmin-t_pml")*1e6;
  if(Math.abs(Arrays.stream(v[0]).min().getAsDouble()-expected)>1e-6)throw new IllegalStateException("Mesh coordinate-unit mismatch");
  double limit=m.param().evaluate("lambda_ref")*1e6/30;
  if(maxEdge>=limit || maxParticle>=limit/m.param().evaluate("n_particle"))
   throw new IllegalStateException("Actual edges fail wavelength limits: all="+maxEdge+", particle="+maxParticle);
  m.study().create("std1");m.study("std1").label("Xu Fig1b - trapezoid PH, plane-wave incidence");
  m.study("std1").create("wave","Wavelength");m.study("std1").feature("wave").set("plist","lambda_ref");
  m.study("std1").createAutoSequences("all");
  for(String sol:m.sol().tags())for(String s:m.sol(sol).feature().tags())
   if(m.sol(sol).feature(s).getType().equals("Stationary"))for(String d:m.sol(sol).feature(s).feature().tags())
    if(m.sol(sol).feature(s).feature(d).getType().equals("Direct")){
     m.sol(sol).feature(s).feature(d).set("linsolver","pardiso");
     m.sol(sol).feature(s).feature(d).set("ooc","on");m.sol(sol).feature(s).feature(d).set("oocmemory",2048);
    }
  m.save(ROOT+"\\models\\Xu2024_Fig1b_EdgeDiffraction_PH.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\settings_and_mesh.txt")){
   out.println("BUILD ONLY; target study computation follows through COMSOL MCP.");
   out.println("Source: DOI 10.1088/1361-6463/ad4160; Figs1(b),(e); Section2; single case.");
   for(String[] p:ps)out.println(p[0]+"="+m.param().get(p[0]));
   out.println("Polygon x={-L-L*tan(alpha),0,0,-L}; y={-L/2,-L/2,L/2,L/2}, um units");
   out.println("L is top-side width and height; bottom-side width L+L*tan(alpha)=4um.");
   out.println("Physical="+Arrays.toString(physical)+"; particle="+Arrays.toString(particle)+"; PML="+Arrays.toString(pml));
   out.println("PML=Cartesian; eight exterior domains; thickness 1um; not source-paper specified extent.");
   out.println("In-plane Ey/Hz; E={0,Eamp*exp(-i*k_bg*x),0}; propagation +x, exp(+i*omega*t).");
   out.println("Second-order edge elements (supplement; source does not state order)");
   out.println("Particle max-size active="+mesh.feature("sizeParticle").getBoolean("hmaxactive"));
   out.println("Triangle count="+tri[0].length+"; PML quads="+mesh.getElem("quad")[0].length);
   out.println("Maximum triangle edge [um]="+maxEdge+"; limit lambda/30="+limit);
   out.println("Maximum particle triangle edge [um]="+maxParticle+"; supplemented lambda/(30n)="+limit/m.param().evaluate("n_particle"));
   out.println("One wavelength; one geometry; no parametric sweep; no results analysis.");
  }
  System.out.println("BUILD_OK triangles="+tri[0].length+" max_edge_um="+maxEdge+" particle_edge_um="+maxParticle);
  return m;
 }
 public static void main(String[] args) throws IOException {run();}
}
