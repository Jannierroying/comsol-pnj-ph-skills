import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Read an already computed axisymmetric solution and show scalar intensity cross-section. */
public class PNJChainDisplay {
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
 static void plot(Model m,String pg,String view,String label,double xmin,double xmax,double ymin,double ymax) {
  if(Arrays.asList(m.result().tags()).contains(pg))m.result().remove(pg);
  m.result().create(pg,"PlotGroup2D");m.result(pg).label(label);m.result(pg).set("data","horizontal");
  if(pg.equals("pgChain"))m.result(pg).set("edges","off");m.result(pg).create("surf1","Surface");m.result(pg).feature("surf1").set("expr","ewfd.normE^2");
  m.result(pg).feature("surf1").set("unit","V^2/m^2");m.result(pg).feature("surf1").set("colortable","Rainbow");
  m.result(pg).feature("surf1").create("sel1","Selection");m.result(pg).feature("surf1").feature("sel1").selection().set(m.component("comp1").selection("physical").entities(2));
  if(!Arrays.asList(m.component("comp1").view().tags()).contains(view))m.component("comp1").view().create(view,2);
  m.component("comp1").view(view).axis().set("viewscaletype","none");
  m.component("comp1").view(view).axis().set("xmin",xmin);m.component("comp1").view(view).axis().set("xmax",xmax);
  m.component("comp1").view(view).axis().set("ymin",ymin);m.component("comp1").view(view).axis().set("ymax",ymax);
  m.result(pg).set("view",view);m.result(pg).set("xlabelactive",true);m.result(pg).set("ylabelactive",true);
  m.result(pg).set("xlabel","z (um)");m.result(pg).set("ylabel","Signed transverse coordinate (um)");m.result(pg).run();
 }
 static void imageExport(Model m,String tag,String pg,String file,int width,int height) {
  if(Arrays.asList(m.result().export().tags()).contains(tag))m.result().export().remove(tag);
  m.result().export().create(tag,pg,"Image");m.result().export(tag).set("imagetype","png");m.result().export(tag).set("pngfilename",ROOT+"\\figures\\"+file);
  m.result().export(tag).set("size","manualweb");m.result().export(tag).set("width",width);m.result().export(tag).set("height",height);
  m.result().export(tag).set("options2d","on");m.result().export(tag).set("axes2d","on");m.result().export(tag).set("legend2d","on");
  m.result().export(tag).set("title2d","on");m.result().export(tag).set("logo2d","off");m.result().export(tag).run();
 }
 public static Model run() throws IOException {
  Model m=ModelUtil.load("Model",ROOT+"\\models\\Xu2024_Fig4_PNJChain.mph");
  if(m.sol("sol1").isEmpty())throw new IllegalStateException("No computed solution");
  if(!m.component("comp1").geom("geom1").isAxisymmetric())throw new IllegalStateException("Geometry must be axisymmetric");
  if(Math.abs(m.param().evaluate("n_cyl")-1.33)>1e-10)throw new IllegalStateException("Empty-air check is not the target model");
  if(Math.abs(m.param().evaluate("gap")/m.param().evaluate("lambda_ref")-10)>1e-10)throw new IllegalStateException("Gap mismatch");
  if(Math.abs(m.param().evaluate("delta_phi"))>1e-10)throw new IllegalStateException("Phase difference mismatch");
  for(String p:new String[]{"port1","port2"})if(!m.component("comp1").physics("ewfd").feature(p).getString("PortExcitation").equals("on"))throw new IllegalStateException("Both ports must be excited");
  for(String ds:new String[]{"horizontal","mirror1"})if(Arrays.asList(m.result().dataset().tags()).contains(ds))m.result().dataset().remove(ds);
  m.result().dataset().create("mirror1","Mirror2D");m.result().dataset("mirror1").set("data","dset1");
  m.result().dataset("mirror1").set("method","pointdir");m.result().dataset("mirror1").set("pdpoint",new String[]{"0","0"});
  m.result().dataset("mirror1").set("pddir",new String[]{"0","1"});
  m.result().dataset().create("horizontal","Transformation2D");m.result().dataset("horizontal").set("data","mirror1");
  m.result().dataset("horizontal").set("transtype","general");
  m.result().dataset("horizontal").set("transmatrix",new double[][]{{0,1},{-1,0}});
  m.result().dataset("horizontal").set("translation",new double[]{0,0});
  double lam=m.param().evaluate("lambda_ref")*1e6,rmax=m.param().evaluate("Rair")*1e6;
  plot(m,"pgField","viewField","PNJ-chain - full meridional field",m.param().evaluate("Zmin")*1e6,m.param().evaluate("Zmax")*1e6,-rmax,rmax);
  plot(m,"pgChain","viewChain","Fig4(a) - PNJ chain between facing surfaces",0,10*lam,-2*lam,2*lam);
  imageExport(m,"imgFull","pgField","Xu2024_Fig4_FullField.png",1300,1300);
  imageExport(m,"imgChain","pgChain","Xu2024_Fig4_PNJChain.png",1500,720);
  m.label("Xu2024_Fig4_Coaxial_PNJ_Chain_computed");m.save(ROOT+"\\models\\Xu2024_Fig4_PNJChain.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\computed_solution.txt")){
   out.println("Computed target solution loaded and displayed; no study run by display script.");
   out.println("Axisymmetric="+m.component("comp1").geom("geom1").isAxisymmetric()+"; components="+m.component("comp1").physics("ewfd").prop("components").getString("components")+"; azimuthal m="+m.component("comp1").physics("ewfd").prop("outofplanewavenumber").getString("mFloquet"));
   out.println("Solution sizes="+Arrays.toString(m.sol("sol1").getSize())+"; inner names="+Arrays.toString(m.sol("sol1").getPNames())+"; values="+Arrays.toString(m.sol("sol1").getPVals()));
   for(String p:new String[]{"lambda_ref","rc","lc","gap","omega","n_cyl","n_bg","delta_phi","Epeak","Abeam","Pbeam"})out.println(p+"="+m.param().get(p));
   for(String p:new String[]{"port1","port2"})out.println(p+" excitation="+m.component("comp1").physics("ewfd").feature(p).getString("PortExcitation")+" type="+m.component("comp1").physics("ewfd").feature(p).getString("PortType")+" slit="+m.component("comp1").physics("ewfd").feature(p).getString("SlitType")+" orientation="+m.component("comp1").physics("ewfd").feature(p).getString("PortOrientation")+" E0="+Arrays.toString(m.component("comp1").physics("ewfd").feature(p).getStringArray("E0")));
   out.println("Surface expr="+m.result("pgChain").feature("surf1").getString("expr")+" palette="+m.result("pgChain").feature("surf1").getString("colortable"));
   out.println("Scalar intensity mirrored about r=0 and rigidly rotated for z-horizontal display. This is a meridional cross-section of the axisymmetric 3D field, not a planar infinite-cylinder simulation.");
   out.println("No hotspot fitting/count extraction, FWHM, optical forces or complete convergence study. User feasibility confirmation pending.");
  }
  System.out.println("COMPUTED_PNJ_CHAIN_DISPLAY_OK");return m;
 }
 public static void main(String[] args)throws IOException{run();}
}
