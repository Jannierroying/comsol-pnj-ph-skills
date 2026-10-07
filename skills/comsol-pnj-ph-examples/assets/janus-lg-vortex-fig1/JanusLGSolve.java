import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;
public class JanusLGSolve {
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
 public static Model run()throws IOException {
  Model m=ModelUtil.load("Model",ROOT+"\\models\\Shi2025_Janus_LG_PH_Fig1.mph");
  ModelUtil.showProgress(true);
  System.out.println("BEM_LINSOLVER="+m.sol("sol1").feature("s1").feature("fc1").getString("linsolver"));
  long t=System.nanoTime();m.study("std1").run();double elapsed=(System.nanoTime()-t)*1e-9;
  if(m.sol("sol1").isEmpty())throw new IllegalStateException("Empty solution after compute");
  m.save(ROOT+"\\models\\Shi2025_Janus_LG_PH_Fig1.mph");
  try(PrintWriter out=new PrintWriter(ROOT+"\\validation\\solve_status.txt")){
   out.println("COMPUTED_SUCCESSFULLY");out.println("Elapsed seconds="+elapsed);
   out.println("Size="+Arrays.toString(m.sol("sol1").getSize()));out.println("PNames="+Arrays.toString(m.sol("sol1").getPNames()));out.println("PVals="+Arrays.toString(m.sol("sol1").getPVals()));
  }
  System.out.println("JANUS_SOLVED_AND_SAVED elapsed_seconds="+elapsed);return m;
 }
 public static void main(String[] args)throws IOException{run();}
}
