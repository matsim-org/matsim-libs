package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;

public class CompareSelectedParkingPropertyTwoRuns {

	/**
	 * This script does the following:
	 * For two given runs, it compares for the same iteration, what percentage of parking choices are different.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA="H:/data/experiments/parkingSearchOct2013/runs/run136/output1/";
		String outputFolderRunB="H:/data/experiments/parkingSearchOct2013/runs/run143/output/";
		int startIteration=400;
		int endIteration=499;
		String fieldName="FacilityId";
		//String fieldName="parkingStrategy";
		//String fieldName="groupName";
		
		System.out.println("iteration\tpctDifferce");
		for (int i=startIteration;i<=endIteration;i++){
			StringMatrix eventsMatrixA = GeneralLib.readStringMatrix(CompareSelectedParkingPropertyOneRun.getEventsFileName(outputFolderRunA,i));	
			StringMatrix eventsMatrixB = GeneralLib.readStringMatrix(CompareSelectedParkingPropertyOneRun.getEventsFileName(outputFolderRunB,i));	
			
			double pctDifference = CompareSelectedParkingPropertyOneRun.percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,fieldName);
			
			System.out.println(i + "\t" + pctDifference);
		}
	}

}
