import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Shi et al. OL 50,1755 (2025), Fig1 physical baseline; three-dimensional full-wave BEM migration. */
public class JanusLGBuild {
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
 // Auto-generated Gauss-Laguerre radial angular-spectrum nodes for l=2.
static final double[] SOURCE_T={0.087649410478927839,0.46269632891508083,1.1410577748312269,2.129283645098381,3.4370866338932067,5.0780186145497677,7.0703385350482346,9.4383143363919384,12.214223368866159,15.441527368781617,19.180156856753136,23.515905693991908,28.578729742882139,34.583398702286622,41.940452647688332,51.701160339543321};
static final double[] SOURCE_W={0.20615171495780069,0.33105785495088402,0.26579577764421436,0.13629693429637774,0.047328928694125222,0.01129990008033945,0.0018490709435263094,0.00020427191530827824,1.4844586873981333e-05,6.828319330871246e-07,1.8810248410796997e-08,2.8623502429738586e-10,2.1270790332240987e-12,6.2979670025178009e-15,5.0504737000355466e-18,4.161462370372804e-22};
 public static Model run() throws IOException {
  Model m=ModelUtil.create("Model");m.label("Shi2025_Janus_LG_PH_Fig1");m.modelPath(ROOT);
  String[][] params={{"lambda_ref","632.8[nm]"},{"R","1.5[um]"},{"z_sphere","2[um]"},
   {"n_bg","1.33"},{"n1","1.4"},{"n2","1.55"},{"w0","1.5[um]"},
   {"Eamp","1[V/m]"},{"xLG","0[um]"},{"ell_paper","2"},{"h_bnd","lambda_ref/(2.5*n2)"}};
  for(String[] p:params)m.param().set(p[0],p[1]);
  m.param().descr("ell_paper","Source phase exp(+i*2*phi) under paper time convention; conjugated for COMSOL");
  m.param().descr("n1","Fig1 upper hemisphere x>0, interface x=0");
  m.param().descr("n2","Fig1 lower hemisphere x<0, interface x=0");
  m.component().create("comp1",true);
  GeomSequence g=m.component("comp1").geom().create("geom1",3);g.lengthUnit("um");
  for(int i=1;i<=2;i++){
   String s="sph"+i,b="clip"+i,h="half"+i;
   g.create(s,"Sphere");g.feature(s).set("r","R");g.feature(s).set("pos",new String[]{"0","0","z_sphere"});
   g.create(b,"Block");g.feature(b).set("size",new String[]{"2*R","4*R","4*R"});
   g.feature(b).set("pos",new String[]{i==1?"0":"-2*R","-2*R","z_sphere-2*R"});
   g.create(h,"Intersection");g.feature(h).selection("input").set(new String[]{s,b});
   g.feature(h).set("selresult",true);
  }
  g.run();
  int[] d1=m.component("comp1").selection("geom1_half1_dom").entities(3);
  int[] d2=m.component("comp1").selection("geom1_half2_dom").entities(3);
  if(d1.length!=1||d2.length!=1||d1[0]==d2[0])throw new IllegalStateException("Two distinct Janus hemispheres required");
  for(int order=1;order<=3;order++){
   String expr=order==1?"if(s<0.0004,1-s/8+s^2/192-s^3/9216,2*besselj(1,sqrt(s))/sqrt(s))":
    order==2?"if(s<0.0004,1-s/12+s^2/384-s^3/23040,8*besselj(2,sqrt(s))/s)":
    "if(s<0.0004,1-s/16+s^2/640-s^3/46080,48*besselj(3,sqrt(s))/s^(1.5))";
   String f="j"+order+"c";m.func().create(f,"Analytic");m.func(f).set("funcname",f);
   m.func(f).set("args","s");m.func(f).set("expr",expr);m.func(f).set("argunit","1");m.func(f).set("fununit","1");
  }
  m.component("comp1").variable().create("src");
  m.component("comp1").variable("src").set("k_bg","2*pi*n_bg*freq/c_const");
  m.component("comp1").variable("src").set("lg_u","(x-xLG-i*y)/w0");
  m.component("comp1").variable("src").set("lg_r2","((x-xLG)^2+y^2)/w0^2");
  StringBuilder bx=new StringBuilder(),bz=new StringBuilder();
  for(int j=0;j<SOURCE_T.length;j++){
   String t=Double.toString(SOURCE_T[j]),w=Double.toString(SOURCE_W[j]),kz="lg_kz"+(j+1);
   m.component("comp1").variable("src").set(kz,"sqrt(k_bg^2-4*("+t+")/w0^2)");
   String phase="exp(-i*"+kz+"*z)",arg="4*("+t+")*lg_r2";
   if(j>0){bx.append("+");bz.append("+");}
   bx.append("("+w+")*("+t+")^2*j2c("+arg+")*"+phase);
   bz.append("("+w+")*((lg_u^3*("+t+")^3*j3c("+arg+")/(3*w0*"+kz+"))-(2*lg_u*("+t+")^2*j1c("+arg+")/(w0*"+kz+")))*"+phase);
  }
  m.component("comp1").variable("src").set("LG_Ex","Eamp*lg_u^2*("+bx+")","Conjugated l=2 vector angular spectrum, propagating +z");
  m.component("comp1").variable("src").set("LG_Ez","i*Eamp*("+bz+")","Exact longitudinal component -kx/kz, regular on axis");
  m.component("comp1").physics().create("ewfd","ElectromagneticWavesBEM","geom1");
  m.component("comp1").physics("ewfd").identifier("ewfd");
  m.component("comp1").physics("ewfd").label("3D full-wave BEM - Janus LG (name ewfd for requested field expression)");
  m.component("comp1").physics("ewfd").feature("wee1").set("DisplacementFieldModel","RelativePermittivity");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("SolveFor","scatteredField");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("WaveType","userdef");
  m.component("comp1").physics("ewfd").prop("BackgroundField").set("Eb",new String[]{"LG_Ex","0","LG_Ez"});
  m.component("comp1").physics("ewfd").prop("Stabilization").set("StabilizationParameter","sqrt(abs(ewfd.k[m]))");
  m.component("comp1").physics("ewfd").prop("FarField").set("bemPrecondMinRange","(2*pi)/ewfd.k0/10");
  m.component("comp1").material().create("matbg","Common");
  m.component("comp1").material("matbg").label("Water, n=1.33, unbounded exterior");
  m.component("comp1").material("matbg").selection().allVoids();
  for(int i=0;i<3;i++){
   String mat=i==0?"matbg":"mat"+i,n=i==0?"n_bg":"n"+i;
   if(i>0){
    m.component("comp1").material().create(mat,"Common");
    m.component("comp1").material(mat).selection().named("geom1_half"+i+"_dom");
    m.component("comp1").material(mat).label(i==1?"x>0 hemisphere, n1=1.4":"x<0 hemisphere, n2=1.55");
    String wee="wee"+(i+1);m.component("comp1").physics("ewfd").create(wee,"WaveEquationElectric",3);
    m.component("comp1").physics("ewfd").feature(wee).selection().named("geom1_half"+i+"_dom");
    m.component("comp1").physics("ewfd").feature(wee).set("DisplacementFieldModel","RelativePermittivity");
   }
   m.component("comp1").material(mat).propertyGroup("def").set("relpermittivity",new String[]{n+"^2"});
   m.component("comp1").material(mat).propertyGroup("def").set("relpermeability",new String[]{"1"});
   m.component("comp1").material(mat).propertyGroup("def").set("electricconductivity",new String[]{"0"});
  }
  MeshSequence mesh=m.component("comp1").mesh().create("mesh1");
  mesh.feature("size").set("custom",true);mesh.feature("size").set("hmax","h_bnd");
  mesh.feature("size").set("hmin","h_bnd/5");mesh.feature("size").set("hgrad",1.2);
  mesh.create("ftri1","FreeTri");mesh.feature("ftri1").selection().geom("geom1",2);mesh.feature("ftri1").selection().all();mesh.run();
  double[][] v=mesh.getVertex();int[][] tri=mesh.getElem("tri");double maxedge=0;
  for(int j=0;j<tri[0].length;j++)for(int a=0;a<3;a++){
   int i1=tri[a][j],i2=tri[(a+1)%3][j];double dd=0;
   for(int dim=0;dim<3;dim++)dd+=Math.pow(v[dim][i1]-v[dim][i2],2);maxedge=Math.max(maxedge,Math.sqrt(dd));
  }
  m.study().create("std1");m.study("std1").label("Shi Fig1 - 3D Janus sphere, LG p0 l2");
  m.study("std1").create("wave","Wavelength");m.study("std1").feature("wave").set("plist","lambda_ref");
  m.study("std1").createAutoSequences("all");
  for(String sol:m.sol().tags())for(String f:m.sol(sol).feature().tags()){
   System.out.println("SOLVER "+sol+"/"+f+" "+m.sol(sol).feature(f).getType());
   for(String sub:m.sol(sol).feature(f).feature().tags())System.out.println("SUBSOLVER "+sub+" "+m.sol(sol).feature(f).feature(sub).getType());
  }
  m.save(ROOT+"\\models\\Shi2025_Janus_LG_PH_Fig1.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\settings_and_mesh.txt")){
   out.println("BUILD ONLY; compute separately using GUI, native batch or a configured MCP.");
   out.println("Source: DOI10.1364/OL.543787, Fig1; physical parameters preserved; numerical method migrated from FDTD to 3D BEM.");
   for(String[] p:params)out.println(p[0]+"="+m.param().get(p[0]));
   out.println("3D solid sphere split at x=0; n1 x>0 domains="+Arrays.toString(d1)+"; n2 x<0 domains="+Arrays.toString(d2));
   out.println("Infinite water void=0; first wave equation only void=0; separate wave equation for each hemisphere.");
   out.println("Physics type="+m.component("comp1").physics("ewfd").getType()+"; tag=ewfd is a variable prefix, not a claim of FEM.");
   out.println("BEM radiation Green function; no artificial PML, no surrounding volume mesh.");
   out.println("Boundary triangle count="+tri[0].length+"; max edge [um]="+maxedge);
   out.println("Default BEM shapeorder="+m.component("comp1").physics("ewfd").prop("ShapeProperty").getString("shapeorder"));
   out.println("Vector LG source order="+SOURCE_T.length+"; E={LG_Ex,0,LG_Ez}; source waist z=0; l=2 in paper convention.");
   out.println("COMSOL exp(+i*omega*t): background is conjugated physical vortex source, exact longitudinal projection -kx/kz.");
   out.println("Paper source uses paraxial -kx/k; exact projection and BEM are declared migration differences, not identical FDTD settings.");
   out.println("One wavelength, no sweep or chirality analysis; target raw field plots ewfd.normE^2, Rainbow.");
  }
  System.out.println("JANUS_BUILD_OK boundary_triangles="+tri[0].length+" max_edge_um="+maxedge);return m;
 }
 public static void main(String[] args) throws IOException{try{run();}catch(RuntimeException e){e.printStackTrace();throw e;}}
}


