package playground.tschlenther.parkingSearch.utils;

public class TestVBSRun {

	public static void main(String[] args) {
	String runPath = "C:/Users/Work/Bachelor Arbeit/RUNS/MemoryBased/Distance/testEvaluationASInstance";	
	String scriptPath = 	"C:/Users/Work/Bachelor Arbeit/Analysis.vbs";
	String highestIter = "1";
	String[] arguments = new String[] {
			"wscript.exe", scriptPath , runPath, highestIter, "Homezone" , "Zielzone"
	};
	System.out.println("START");
	runVBScript(arguments);
		System.out.println("DONE");
	}
	private static void runVBScript(String[] cmds){
		try {
	        Runtime.getRuntime().exec(cmds);        
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        throw new RuntimeException("konnte Skript nicht durchlaufen lassen");
	    }
	}
}
