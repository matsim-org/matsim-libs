package playground.wrashid.lib;

import org.matsim.api.core.v01.Id;

public class DebugLib {

	public static void traceAgent(Id personId){
		if (personId.toString().equalsIgnoreCase("743")){
			System.out.println();
		}
	}

	public static void startDebuggingInIteration(int iterationNumber){
		if (iterationNumber==18){
			System.out.println();
		}
	}
	
	public static void stopSystemAndReportInconsistency(){
		throw new Error("system is in inconsistent state");
	}

}
