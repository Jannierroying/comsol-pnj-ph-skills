import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;
public class DualEllipseRayDisplay {
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
 static double[][] evaluate(Model m,String tag,String expr,String inner){
  m.result().numerical().create(tag,"Ray");m.result().numerical(tag).set("data","ray1");m.result().numerical(tag).set("expr",expr);m.result().numerical(tag).set("innerinput",inner);return m.result().numerical(tag).getReal();
 }
 static void plot(Model m,String tag,String label,boolean primary,int last) throws IOException {
  m.result().create(tag,"PlotGroup2D");m.result(tag).label(label);m.result(tag).set("data","ray1");m.result(tag).set("looplevel",new int[]{last});
  m.result(tag).create("rtrj1","RayTrajectories");m.result(tag).feature("rtrj1").set("linetype","line");
  m.result(tag).feature("rtrj1").create("col1","Color");m.result(tag).feature("rtrj1").feature("col1").set("expr","gop.Q/(Qray0*1[m])");m.result(tag).feature("rtrj1").feature("col1").set("unit","1");m.result(tag).feature("rtrj1").feature("col1").set("descr","Ray power fraction relative to initial ray (unit depth)");m.result(tag).feature("rtrj1").feature("col1").set("colortable","Rainbow");
  if(primary){m.result(tag).feature("rtrj1").create("filt1","RayTrajectoriesFilter");m.result(tag).feature("rtrj1").feature("filt1").set("type","primary");}
  m.result(tag).run();m.save(ROOT+"\\models\\DualEllipse_Core60_Shell60_RayTracing.mph");
  String exp="img"+tag;m.result().export().create(exp,tag,"Image");m.result().export(exp).set("imagetype","png");m.result().export(exp).set("pngfilename",ROOT+"\\figures\\"+(primary?"Primary_Rays.png":"All_Rays_Reflections.png"));m.result().export(exp).set("size","manualweb");m.result().export(exp).set("width",1600);m.result().export(exp).set("height",900);m.result().export(exp).set("options2d","on");m.result().export(exp).set("axes2d","on");m.result().export(exp).set("legend2d","on");m.result().export(exp).set("title2d","on");m.result().export(exp).set("logo2d","off");m.result().export(exp).run();
 }
 public static Model run() throws IOException {
  Model m=ModelUtil.load("Model",ROOT+"\\models\\DualEllipse_Core60_Shell60_RayTracing.mph");
  for(String tag:m.result().numerical().tags())if(tag.startsWith("ev"))m.result().numerical().remove(tag);
  for(String tag:m.result().export().tags())if(tag.startsWith("imgpg"))m.result().export().remove(tag);
  for(String tag:m.result().tags())if(tag.equals("pgPrimary")||tag.equals("pgAll")){
   System.out.println("PREVIOUS_COLOR_UNIT="+m.result(tag).feature("rtrj1").feature("col1").getString("unit"));m.result().remove(tag);
  }
  for(String tag:m.result().dataset().tags())if(tag.equals("ray1"))m.result().dataset().remove(tag);
  String sol=m.result().dataset("dset1").getString("solution");double[] times=m.sol(sol).getPVals();
  System.out.println("RAY_PROPERTY_SPECIFICATION "+m.component("comp1").physics("gop").feature("op1").getString("RayPropertySpecification"));
  System.out.println("RAY_PROPERTY_OPTIONS "+Arrays.toString(m.component("comp1").physics("gop").feature("op1").getAllowedPropertyValues("RayPropertySpecification")));
  m.result().dataset().create("ray1","Ray");m.result().dataset("ray1").set("solution",sol);m.result().dataset("ray1").set("physicsinterface","gop");
  double[][] q=evaluate(m,"evQ","gop.Q","all");double[][] xx=evaluate(m,"evX","qx/1[um]","all");double[][] yy=evaluate(m,"evY","qy/1[um]","all");
  double[][] lam=evaluate(m,"evLambda","gop.lambda0/1[nm]","first");
  int primary=0,released=0;double sum=0;boolean rightLambda=true;
  for(int r=0;r<q.length;r++){
   if(q[r][0]>0&&Double.isFinite(q[r][0])){primary++;sum+=q[r][0];if(Math.abs(lam[r][0]-532)>1e-7)rightLambda=false;}
   for(double value:q[r])if(value>0&&Double.isFinite(value)){released++;break;}
  }
  System.out.println("ARRAY_SHAPE rays="+q.length+" times="+q[0].length+" time_values="+times.length);
  System.out.println("INITIAL_RAYS="+primary+" EVER_RELEASED="+released+" INITIAL_POWER_W_PER_M="+sum+" WAVELENGTH_532="+rightLambda);
  if(primary!=201||!rightLambda||q[0].length!=times.length)throw new IllegalStateException("Ray solution verification failed");
  double expected=m.param().evaluate("Psrc_2d");if(Math.abs(sum/expected-1)>1e-6)throw new IllegalStateException("Source power mismatch");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\solved_rays.txt")){
   out.println("Actual COMSOL Ray Tracing solve completed and reloaded.");out.println("Primary rays="+primary);out.println("Secondary rays released="+(released-primary));out.println("Allocated secondary slots="+m.param().get("Nsecondary"));out.println("Stored time steps="+times.length);out.println("Initial total power per thickness="+sum+" W/m");out.println("Derived plane-wave power per thickness="+expected+" W/m");out.println("Actual wavelength of all initially released rays=532 nm");out.println("First ray x="+xx[0][0]+" um, y="+yy[0][0]+" um");out.println("Core angle="+m.param().get("theta_core")+"; shell angle="+m.param().get("theta_shell"));out.println("Polarization in plane="+m.component("comp1").physics("gop").feature("relg1").getString("axy0")+", out of plane="+m.component("comp1").physics("gop").feature("relg1").getString("az0"));
  }
  try(PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(ROOT+"\\data\\ray_trajectories.csv")))){
   double q0=m.param().evaluate("Qray0");
   out.println("ray_index_1based,step_index_1based,time_s,x_um,y_um,power_W_at_unit_depth,power_relative_to_initial_ray,is_initial_ray");
   for(int r=0;r<q.length;r++)for(int k=0;k<q[r].length;k++)if(q[r][k]>0&&Double.isFinite(q[r][k])&&Double.isFinite(xx[r][k])&&Double.isFinite(yy[r][k]))out.println((r+1)+","+(k+1)+","+times[k]+","+xx[r][k]+","+yy[r][k]+","+q[r][k]+","+(q[r][k]/q0)+","+(q[r][0]>0?1:0));
  }
  plot(m,"pgPrimary","Primary rays - core 60 deg - shell 60 deg",true,times.length);
  plot(m,"pgAll","All rays including Fresnel reflections",false,times.length);
  m.save(ROOT+"\\models\\DualEllipse_Core60_Shell60_RayTracing.mph");
  System.out.println("RAY_DISPLAY_AND_DATA_COMPLETE");return m;
 }
 public static void main(String[] args) throws IOException {run();}
}
