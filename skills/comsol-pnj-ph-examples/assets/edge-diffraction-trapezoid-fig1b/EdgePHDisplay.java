import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Display an already computed model. Does not compute a study or derive PH metrics. */
public class EdgePHDisplay {
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
 static void plot(Model m,String tag,String view,String label,double xmin,double xmax,double ymin,double ymax,String data){
  if(Arrays.asList(m.result().tags()).contains(tag))m.result().remove(tag);
  if(!Arrays.asList(m.component("comp1").view().tags()).contains(view))m.component("comp1").view().create(view,2);
  m.component("comp1").view(view).axis().set("viewscaletype","none");
  m.component("comp1").view(view).axis().set("xmin",xmin);m.component("comp1").view(view).axis().set("xmax",xmax);
  m.component("comp1").view(view).axis().set("ymin",ymin);m.component("comp1").view(view).axis().set("ymax",ymax);
  m.result().create(tag,"PlotGroup2D");m.result(tag).label(label);m.result(tag).set("data",data);m.result(tag).set("view",view);
  m.result(tag).create("surf1","Surface");m.result(tag).feature("surf1").set("expr","ewfd.normE^2");
  m.result(tag).feature("surf1").set("colortable","Rainbow");m.result(tag).feature("surf1").set("unit","V^2/m^2");
  m.result(tag).feature("surf1").create("sel1","Selection");
  m.result(tag).feature("surf1").feature("sel1").selection().set(m.component("comp1").selection("physical").entities(2));
  m.result(tag).run();
 }
 static void export(Model m,String tag,String pg,String filename){
  if(Arrays.asList(m.result().export().tags()).contains(tag))m.result().export().remove(tag);
  m.result().export().create(tag,pg,"Image");m.result().export(tag).set("imagetype","png");
  m.result().export(tag).set("pngfilename",ROOT+"\\figures\\"+filename);
  m.result().export(tag).set("size","manualweb");m.result().export(tag).set("width",1600);m.result().export(tag).set("height",850);
  for(String p:new String[]{"options2d","axes2d","legend2d","title2d"})m.result().export(tag).set(p,"on");
  m.result().export(tag).set("logo2d","off");m.result().export(tag).run();
 }
 public static Model run() throws IOException {
  Model m=ModelUtil.load("Model",ROOT+"\\models\\Xu2024_Fig1b_EdgeDiffraction_PH.mph");
  if(m.sol("sol1").isEmpty())throw new IllegalStateException("No actual solution");
  if(Math.abs(m.param().evaluate("lambda_ref")-1e-6)>1e-15 || Math.abs(m.param().evaluate("L")-3e-6)>1e-15)
   throw new IllegalStateException("Geometry/wavelength mismatch");
  if(Math.abs(m.param().evaluate("tan(alpha)")-1.0/3)>1e-12 || Math.abs(m.param().evaluate("n_particle")-1.46)>1e-12)
   throw new IllegalStateException("Angle/index mismatch");
  String data=null;for(String d:m.result().dataset().tags())
   if(m.result().dataset(d).getType().equals("Solution") && m.result().dataset(d).getString("solution").equals("sol1")){data=d;break;}
  if(data==null)throw new IllegalStateException("No solution dataset");
  for(String pg:m.result().tags())m.result().remove(pg);
  plot(m,"pgField","viewField","Fig1b - full physical field",-6,8,-3.5,3.5,data);
  plot(m,"pgHook","viewHook","Fig1b - photonic hook detail",-4.3,7,-2,2,data);
  // Preserve the computed solution and configured plots before image export.
  m.save(ROOT+"\\models\\Xu2024_Fig1b_EdgeDiffraction_PH.mph");
  export(m,"imgFull","pgField","Xu2024_Fig1b_FullField.png");
  export(m,"imgHook","pgHook","Xu2024_Fig1b_PH.png");
  m.label("Xu2024_Fig1b_EdgeDiffraction_PH_computed");m.save(ROOT+"\\models\\Xu2024_Fig1b_EdgeDiffraction_PH.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\computed_solution.txt")){
   out.println("ALREADY COMPUTED SOLUTION RELOADED AND DISPLAYED; no study run in this script.");
   out.println("Solution=sol1; size="+Arrays.toString(m.sol("sol1").getSize())+"; dataset="+data);
   out.println("Solution parameter names="+Arrays.toString(m.sol("sol1").getPNames()));
   out.println("Solution parameter values="+Arrays.toString(m.sol("sol1").getPVals()));
   out.println("Study type="+m.study("std1").feature("wave").getType()+"; wavelength list="+m.study("std1").feature("wave").getString("plist"));
   out.println("L="+m.param().get("L")+"; alpha="+m.param().get("alpha")+"; n_particle="+m.param().get("n_particle"));
   out.println("Permittivity="+Arrays.toString(m.component("comp1").material("mat2").propertyGroup("def").getStringArray("relpermittivity")));
   out.println("Background="+Arrays.toString(m.component("comp1").physics("ewfd").prop("BackgroundField").getStringArray("Eb")));
   for(String pg:new String[]{"pgField","pgHook"})out.println(pg+": "+m.result(pg).feature("surf1").getString("expr")+", "+m.result(pg).feature("surf1").getString("colortable"));
   out.println("Only basic field images; no fitted hook arms, angle, FWHM, Poynting streamlines or full convergence analysis.");
   out.println("User feasibility verification pending.");
  }
  System.out.println("COMPUTED_FIELD_EXPORT_OK");return m;
 }
 public static void main(String[] args) throws IOException {run();}
}
