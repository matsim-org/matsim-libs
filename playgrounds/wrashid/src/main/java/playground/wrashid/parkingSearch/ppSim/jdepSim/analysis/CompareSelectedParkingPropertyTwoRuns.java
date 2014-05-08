package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;

public class CompareSelectedParkingPropertyTwoRuns extends CompareSelectedParkingPropertyOneRun {

	/**
	 * This script does the following:
	 * For two given runs, it compares for the same iteration, what percentage of parking choices are different.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA="H:/data/experiments/parkingSearchOct2013/runs/run163/output/";
		String outputFolderRunB="H:/data/experiments/parkingSearchOct2013/runs/run164/output/";
		int startIteration=400;
		int endIteration=499;
		int iterationStep = 10;
		boolean ignoreCasesWithBothPPUse=true;
		
		System.out.println("iteration\tpctDifferce-FacilityId\tpctDifferce-parkingStrategy\tpctDifferce-groupName");
		for (int i=startIteration;i<=endIteration; i += iterationStep){
			StringMatrix eventsMatrixA = GeneralLib.readStringMatrix(getEventsFileName(outputFolderRunA,i));	
			StringMatrix eventsMatrixB = GeneralLib.readStringMatrix(getEventsFileName(outputFolderRunB,i));	
			
			System.out.print(i + "\t" + percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,"FacilityId",ignoreCasesWithBothPPUse));
			System.out.print("\t" + percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,"parkingStrategy",ignoreCasesWithBothPPUse));
			System.out.print("\t" + percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,"groupName",ignoreCasesWithBothPPUse));
			System.out.println();
		}
	}

}
