import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;
public class JanusLGDisplay {
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
  Model m=ModelUtil.load("Model",ROOT+"\\models\\Shi2025_Janus_LG_PH_Fig1.mph");
  if(m.sol("sol1").isEmpty())throw new IllegalStateException("No computed solution");
  if(!m.component("comp1").physics("ewfd").identifier().equals("ewfd"))throw new IllegalStateException("Wrong physics variable scope");
  String data=null;for(String d:m.result().dataset().tags())if(m.result().dataset(d).getType().equals("Solution")&&m.result().dataset(d).getString("solution").equals("sol1")){data=d;break;}
  if(data==null)throw new IllegalStateException("Solution dataset absent");
  for(String pg:m.result().tags())m.result().remove(pg);
  String plane="planeFig1",grid="gridField";
  if(Arrays.asList(m.result().dataset().tags()).contains(plane))m.result().dataset().remove(plane);
  if(Arrays.asList(m.result().dataset().tags()).contains(grid))m.result().dataset().remove(grid);
  m.result().dataset().create(grid,"Grid3D");m.result().dataset(grid).set("source","data");m.result().dataset(grid).set("data",data);
  m.result().dataset(grid).set("par1","x");m.result().dataset(grid).set("par2","y");m.result().dataset(grid).set("par3","z");
  m.result().dataset(grid).set("parmin1",-3);m.result().dataset(grid).set("parmax1",3);
  m.result().dataset(grid).set("parmin2",-0.001);m.result().dataset(grid).set("parmax2",0.001);
  m.result().dataset(grid).set("parmin3",3);m.result().dataset(grid).set("parmax3",8);
  m.result().dataset(grid).set("res1",121);m.result().dataset(grid).set("res2",2);m.result().dataset(grid).set("res3",121);
  m.result().dataset().create(plane,"CutPlane");m.result().dataset(plane).set("data",grid);
  m.result().dataset(plane).label("Fig1 xoz plane: horizontal z, vertical x, y=0");
  m.result().dataset(plane).set("planetype","general");m.result().dataset(plane).set("genmethod","threepoint");
  m.result().dataset(plane).set("genpoints",new double[][]{{0,0,0},{0,0,1},{1,0,0}});
  if(!Arrays.asList(m.component("comp1").view().tags()).contains("viewField2D"))m.component("comp1").view().create("viewField2D",2);
  m.component("comp1").view("viewField2D").axis().set("viewscaletype","none");
  m.component("comp1").view("viewField2D").axis().set("xmin",3);m.component("comp1").view("viewField2D").axis().set("xmax",8);
  m.component("comp1").view("viewField2D").axis().set("ymin",-3);m.component("comp1").view("viewField2D").axis().set("ymax",3);
  m.result().create("pgField","PlotGroup2D");m.result("pgField").label("Fig1 baseline: ewfd.normE^2, xoz y=0");
  m.result("pgField").set("data",plane);m.result("pgField").set("view","viewField2D");
  m.result("pgField").set("xlabelactive",true);m.result("pgField").set("xlabel","z (um)");
  m.result("pgField").set("ylabelactive",true);m.result("pgField").set("ylabel","x (um)");
  m.result("pgField").create("surf1","Surface");m.result("pgField").feature("surf1").set("expr","ewfd.normE^2");
  m.result("pgField").feature("surf1").set("colortable","Rainbow");m.result("pgField").feature("surf1").set("unit","V^2/m^2");
  m.result("pgField").feature("surf1").set("resolution","normal");
  m.save(ROOT+"\\models\\Shi2025_Janus_LG_PH_Fig1.mph");
  System.out.println("FIELD_PLOT_RUN_BEGIN");m.result("pgField").run();System.out.println("FIELD_PLOT_RUN_OK");
  if(Arrays.asList(m.result().export().tags()).contains("imgField"))m.result().export().remove("imgField");m.result().export().create("imgField","pgField","Image");m.result().export("imgField").set("imagetype","png");
  m.result().export("imgField").set("pngfilename",ROOT+"\\figures\\Shi2025_Fig1_Field.png");
  m.result().export("imgField").set("size","manualweb");m.result().export("imgField").set("width",1400);m.result().export("imgField").set("height",1100);
  for(String p:new String[]{"options2d","axes2d","legend2d","title2d"})m.result().export("imgField").set(p,"on");
  m.result().export("imgField").set("logo2d","off");m.result().export("imgField").run();
  m.save(ROOT+"\\models\\Shi2025_Janus_LG_PH_Fig1.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\computed_solution.txt")){
   out.println("Actual computed solution reloaded; no study run in this display script.");
   out.println("Solution size="+Arrays.toString(m.sol("sol1").getSize()));out.println("PNames="+Arrays.toString(m.sol("sol1").getPNames()));out.println("PVals="+Arrays.toString(m.sol("sol1").getPVals()));
   out.println("Physics type="+m.component("comp1").physics("ewfd").getType()+"; actual identifier="+m.component("comp1").physics("ewfd").identifier());
   out.println("Wave step="+m.study("std1").feature("wave").getType()+"; plist="+m.study("std1").feature("wave").getString("plist"));
   out.println("Plot expression="+m.result("pgField").feature("surf1").getString("expr")+"; palette="+m.result("pgField").feature("surf1").getString("colortable"));
   out.println("Observation plane y=0; horizontal +z, vertical +x; z=3..8 um, x=-3..3 um; 121 by2 by121 Grid3D samples, cut plane at y=0.");
   out.println("No chirality, angle, FWHM, streamlines or convergence analysis. User confirmation pending.");
  }
  System.out.println("JANUS_COMPUTED_FIELD_EXPORT_OK");return m;
 }
 public static void main(String[] args)throws IOException{run();}
}


