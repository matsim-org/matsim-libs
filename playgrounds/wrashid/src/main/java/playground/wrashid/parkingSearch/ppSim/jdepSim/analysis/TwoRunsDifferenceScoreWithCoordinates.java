package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class TwoRunsDifferenceScoreWithCoordinates extends CompareSelectedParkingPropertyOneRun {

	/**
	 * This script does the following: For two given runs, it compares for the
	 * same iteration, what percentage of parking choices are different.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolderRunA = "h:/data/experiments/parkingSearchOct2013/runs/run403/output/";
		String outputFolderRunB = "h:/data/experiments/parkingSearchOct2013/runs/run404/output/";
		int iteration = 499;
		boolean ignoreCasesWithBothPPUse = true;

		System.out.println("x\ty\tscoreDiff");

		Matrix eventsMatrixA = GeneralLib.readStringMatrix(getEventsFileName(outputFolderRunA, iteration));
		Matrix eventsMatrixB = GeneralLib.readStringMatrix(getEventsFileName(outputFolderRunB, iteration));

		printScoreDifferenceAndCoordinates(eventsMatrixA, eventsMatrixB, ignoreCasesWithBothPPUse);
	}

	public static void printScoreDifferenceAndCoordinates(Matrix matrixA, Matrix matrixB, boolean ignoreCasesWithBothPPUse) {

		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id<Person>, Integer, Integer> indexMatrixB = getIndex(matrixB);

		int indexPersonId = matrixA.getColumnIndex("personId");
		int indexLeg = matrixA.getColumnIndex("legIndex");

		int indexScore = matrixA.getColumnIndex("score");

		for (int i = 1; i < matrixA.getNumberOfRows(); i++) {
			Id<Person> personId = Id.create(matrixA.getString(i, indexPersonId), Person.class);
			int legIndex = matrixA.getInteger(i, indexLeg);

			

			if (ignoreCasesWithBothPPUse) {
				Integer rowIndexMatrixB = indexMatrixB.get(personId, legIndex);
				
				double scoreDifference=Math.abs(matrixA.getDouble(i, indexScore)-matrixB.getDouble(rowIndexMatrixB, indexScore));
				
				
				int indexFacilityId = matrixA.getColumnIndex("FacilityId");
				String facilityIdA = matrixA.getString(i, indexFacilityId);
				String facilityIdB = matrixB.getString(rowIndexMatrixB, indexFacilityId);

				if (!((facilityIdA.toString().contains("private") && facilityIdB.toString().contains("private")) || (facilityIdA
						.toString().contains("publicPOutside") && facilityIdB.toString().contains("publicPOutside")))) {
					System.out.println(matrixA.getString(i, matrixA.getColumnIndex("destination-X")) + "\t" + matrixA.getString(i,matrixA.getColumnIndex("destination-Y")) + "\t" + scoreDifference + "\t" + facilityIdA.toString() + "\t" + facilityIdB.toString());
				}
			} else {
				DebugLib.stopSystemAndReportInconsistency();
			}
		}
	}

}
