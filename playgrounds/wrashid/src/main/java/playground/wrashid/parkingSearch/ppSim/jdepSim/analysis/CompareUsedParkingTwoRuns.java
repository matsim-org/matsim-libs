package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;

public class CompareUsedParkingTwoRuns {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA="H:/data/experiments/parkingSearchOct2013/runs/run136/output1/";
		String outputFolderRunB="H:/data/experiments/parkingSearchOct2013/runs/run143/output/";
		int startIteration=400;
		int endIteration=499;
		
		System.out.println("iteration\tpctDifferce");
		for (int i=startIteration;i<=endIteration;i++){
			StringMatrix eventsMatrixA = GeneralLib.readStringMatrix(CompareUsedParkingOneRun.getEventsFileName(outputFolderRunA,i));	
			StringMatrix eventsMatrixB = GeneralLib.readStringMatrix(CompareUsedParkingOneRun.getEventsFileName(outputFolderRunB,i));	
			
			double pctDifference = CompareUsedParkingOneRun.percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB);
			
			System.out.println(i + "\t" + pctDifference);
		}
	}

}
