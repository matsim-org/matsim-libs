package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

public class CompareSelectedParkingPropertyTwoRuns extends CompareSelectedParkingPropertyOneRun {

	/**
	 * This script does the following:
	 * For two given runs, it compares for the same iteration, what percentage of parking choices are different.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA="f:/data/experiments/parkingSearchOct2013/runs/run179/output/";
		String outputFolderRunB="f:/data/experiments/parkingSearchOct2013/runs/run180/output/";
		int startIteration=1200;
		int endIteration=1205;
		int iterationStep = 10;
		boolean ignoreCasesWithBothPPUse=true;
		
		System.out.println("iteration\tpctDifferce-FacilityId\tpctDifferce-parkingStrategy\tpctDifferce-groupName");
		for (int i=startIteration;i<=endIteration; i += iterationStep){
			Matrix eventsMatrixA = GeneralLib.readStringMatrix(getEventsFileName(outputFolderRunA,i));	
			Matrix eventsMatrixB = GeneralLib.readStringMatrix(getEventsFileName(outputFolderRunB,i));	
			
			System.out.print(i + "\t" + percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,"FacilityId",ignoreCasesWithBothPPUse));
			System.out.print("\t" + percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,"parkingStrategy",ignoreCasesWithBothPPUse));
			System.out.print("\t" + percentageOfDifferentParkingUsages(eventsMatrixA,eventsMatrixB,"groupName",ignoreCasesWithBothPPUse));
			System.out.println();
		}
	}

}
