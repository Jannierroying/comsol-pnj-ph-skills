import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/** Display and validate an already computed solution; does not run a study. */
public class PangGRINDisplay {
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
 public static void setView(Model m,String view,double xmin,double xmax,double ymin,double ymax) {
  if(!Arrays.asList(m.component("comp1").view().tags()).contains(view))m.component("comp1").view().create(view,2);
  m.component("comp1").view(view).axis().set("viewscaletype","none");
  m.component("comp1").view(view).axis().set("xmin",xmin);m.component("comp1").view(view).axis().set("xmax",xmax);
  m.component("comp1").view(view).axis().set("ymin",ymin);m.component("comp1").view(view).axis().set("ymax",ymax);
  if(Arrays.asList(m.component("comp1").view(view).axis().properties()).contains("equal"))m.component("comp1").view(view).axis().set("equal","on");
 }
 public static void export(Model m,String tag,String pg,String filename,int width,int height) {
  if(Arrays.asList(m.result().export().tags()).contains(tag))m.result().export().remove(tag);m.result().export().create(tag,pg,"Image");m.result().export(tag).set("imagetype","png");
  m.result().export(tag).set("pngfilename",ROOT+"\\figures\\"+filename);
  m.result().export(tag).set("size","manualweb");m.result().export(tag).set("width",width);m.result().export(tag).set("height",height);
  m.result().export(tag).set("options2d","on");m.result().export(tag).set("axes2d","on");
  m.result().export(tag).set("legend2d","on");m.result().export(tag).set("title2d","on");m.result().export(tag).set("logo2d","off");
  m.result().export(tag).run();
 }
 public static Model run() throws IOException {
  Model m=ModelUtil.load("Model",ROOT+"\\models\\Pang2025_Fig2d_GRIN.mph");
  if(m.sol("sol1").isEmpty())throw new IllegalStateException("No computed solution");
  if(Math.abs(m.param().evaluate("lambda_ref")-633e-9)>1e-15)throw new IllegalStateException("Wavelength mismatch");
  if(Math.abs(m.param().evaluate("R")-7.5e-6)>1e-12)throw new IllegalStateException("Radius mismatch");
  if(!m.component("comp1").material("mat2").propertyGroup("def").getStringArray("relpermittivity")[0].equals("n_grin^2"))throw new IllegalStateException("Continuous GRIN not assigned");
  String dataset=null;
  for(String d:m.result().dataset().tags())if(m.result().dataset(d).getType().equals("Solution") && m.result().dataset(d).getString("solution").equals("sol1")){dataset=d;break;}
  if(dataset==null)throw new IllegalStateException("No solution dataset");
  for(String pg:m.result().tags())if(!pg.equals("pgField"))m.result().remove(pg);
  m.result("pgField").set("data",dataset);m.result("pgField").feature("surf1").set("expr","ewfd.normE^2");
  m.result("pgField").feature("surf1").set("colortable","Rainbow");m.result("pgField").feature("surf1").set("unit","V^2/m^2");if(!Arrays.asList(m.result("pgField").feature("surf1").feature().tags()).contains("sel1"))m.result("pgField").feature("surf1").create("sel1","Selection");m.result("pgField").feature("surf1").feature("sel1").selection().set(m.component("comp1").selection("physical").entities(2));
  setView(m,"viewField",-8.766,11.298,-8.766,8.766);m.result("pgField").set("view","viewField");
  m.result("pgField").run();
  m.result().create("pgJet","PlotGroup2D");m.result("pgJet").label("Fig2d - off-axis PNJ detail");m.result("pgJet").set("data",dataset);
  m.result("pgJet").create("surf1","Surface");m.result("pgJet").feature("surf1").set("expr","ewfd.normE^2");
  m.result("pgJet").feature("surf1").set("colortable","Rainbow");m.result("pgJet").feature("surf1").set("unit","V^2/m^2");m.result("pgJet").feature("surf1").create("sel1","Selection");m.result("pgJet").feature("surf1").feature("sel1").selection().set(m.component("comp1").selection("physical").entities(2));
  setView(m,"viewJet",5.0,11.298,-4.5,3.0);m.result("pgJet").set("view","viewJet");m.result("pgJet").run();
  export(m,"imgFull","pgField","Pang2025_Fig2d_Field.png",1400,1000);
  export(m,"imgJet","pgJet","Pang2025_Fig2d_PNJ.png",1100,1100);
  m.label("Pang2025_Fig2d_GRIN_Air_computed");m.save(ROOT+"\\models\\Pang2025_Fig2d_GRIN.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\computed_solution.txt")){
   out.println("ALREADY COMPUTED SOLUTION RELOADED AND DISPLAYED; no study run in display script.");
   out.println("Solver=sol1; dataset="+dataset+"; solution sizes="+Arrays.toString(m.sol("sol1").getSize()));
   out.println("Inner solution parameter names="+Arrays.toString(m.sol("sol1").getPNames()));out.println("Inner solution parameter values="+Arrays.toString(m.sol("sol1").getPVals()));out.println("Stored parameters="+Arrays.toString(m.sol("sol1").getParamNames()));
   out.println("Stored values="+Arrays.toString(m.sol("sol1").getParamVals()));
   out.println("Study step="+m.study("std1").feature("wave").getType()+"; plist="+m.study("std1").feature("wave").getString("plist"));
   out.println("R="+m.param().get("R")+"; lambda_ref="+m.param().get("lambda_ref")+"; theta="+m.param().get("theta"));
   out.println("Refractive index="+m.component("comp1").variable("var1").get("n_grin"));
   out.println("Permittivity="+Arrays.toString(m.component("comp1").material("mat2").propertyGroup("def").getStringArray("relpermittivity")));
   out.println("Background field="+Arrays.toString(m.component("comp1").physics("ewfd").prop("BackgroundField").getStringArray("Eb")));
   out.println("Surface=ewfd.normE^2; palette=Rainbow; physical domains only.");
   out.println("Figures: full physical domain and PNJ detail. No fitted off-axis angle, FWHM, length or automated parameter sweep.");
   out.println("No comprehensive mesh/domain/PML convergence study; user verification pending.");
  }
  System.out.println("COMPUTED_FIELD_EXPORT_OK");return m;
 }
 public static void main(String[] args) throws IOException {run();}
}
