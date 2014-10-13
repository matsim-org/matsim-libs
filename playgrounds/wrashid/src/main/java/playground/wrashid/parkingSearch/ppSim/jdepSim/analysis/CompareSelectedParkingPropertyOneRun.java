package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class CompareSelectedParkingPropertyOneRun {

	/**
	 * 
	 * compare the iterations of a run to a reference iteration and to consecutive iterations. 
	 * 
	 *  As when private parking is used, the strategy (group) does not matter, it is also possible to ignore such parking.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run135/output/";
		int referenceIteration = 410;
		int startIteration = 400;
		int endIteration = referenceIteration - 1;
		int iterationStep = 1;
		boolean ignoreCasesWithBothPPUse=true;

		Matrix eventsReferenceMatrix = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						referenceIteration));
		Matrix eventsMatrixCurrentIter = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));

		System.out
				.println("iteration\tpctDiffConsequete-FacilityId\tpctDiffReference-FacilityId\tpctDiffConsequete-parkingStrategy\tpctDiffReference-parkingStrategy\tpctDiffConsequete-groupName\tpctDiffReference-groupName");
		for (int i = startIteration; i <= endIteration; i += iterationStep) {
			Matrix eventsMatrixNextIter = GeneralLib
					.readStringMatrix(getEventsFileName(outputFolder, i + 1));

			System.out.print(i
					+ "\t"
					+ percentageOfDifferentParkingUsages(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"FacilityId",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfDifferentParkingUsages(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "FacilityId",ignoreCasesWithBothPPUse));
			System.out.print("\t"
					+ percentageOfDifferentParkingUsages(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"parkingStrategy",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfDifferentParkingUsages(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "parkingStrategy",ignoreCasesWithBothPPUse));
			System.out.print("\t"
					+ percentageOfDifferentParkingUsages(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"groupName",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfDifferentParkingUsages(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "groupName",ignoreCasesWithBothPPUse));
			System.out.println();

			eventsMatrixCurrentIter = eventsMatrixNextIter;
		}
	}

	public static String getEventsFileName(String outputFolder,
			int iterationNumber) {
		String fileName = outputFolder + "/ITERS/" + iterationNumber
				+ ".parkingEvents.txt.gz";
		return fileName;
	}

	public static double percentageOfDifferentParkingUsages(
			Matrix matrixA,
			Matrix matrixB, String fieldName, boolean ignoreCasesWithBothPPUse) {
		
		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id<Person>, Integer, Integer> indexMatrixB=getIndex(matrixB);
		
		int indexPersonId=matrixA.getColumnIndex("personId");
		int indexLeg=matrixA.getColumnIndex("legIndex");
		
		int indexSelectedField=matrixA.getColumnIndex(fieldName);
		
		int totalNumberOfParkingOperations=0;
		int numberOfTimesSameParkingFacilityChosen=0;
		for (int i=1;i<matrixA.getNumberOfRows();i++){
			Id<Person> personId=Id.create(matrixA.getString(i, indexPersonId), Person.class);
			int legIndex=matrixA.getInteger(i, indexLeg);
			
			String selectedFieldIdA=matrixA.getString(i, indexSelectedField);
			String selectedFieldIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexSelectedField);
			
			if (ignoreCasesWithBothPPUse){
				int indexFacilityId=matrixA.getColumnIndex("FacilityId");
				String facilityIdA=matrixA.getString(i, indexFacilityId);
				String facilityIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexFacilityId);
				
				if (!((facilityIdA.toString().contains("private") && facilityIdB.toString().contains("private")) || (facilityIdA.toString().contains("publicPOutside") && facilityIdB.toString().contains("publicPOutside")))){
					if (selectedFieldIdA.equalsIgnoreCase(selectedFieldIdB)){
						numberOfTimesSameParkingFacilityChosen++;
					}
					totalNumberOfParkingOperations++;
				}
			} else {
				if (selectedFieldIdA.equalsIgnoreCase(selectedFieldIdB)){
					numberOfTimesSameParkingFacilityChosen++;
				}
				totalNumberOfParkingOperations++;
			}
		}
		
		return (1-(1.0*numberOfTimesSameParkingFacilityChosen/totalNumberOfParkingOperations))*100;
	}

	public static TwoHashMapsConcatenated<Id<Person>, Integer, Integer> getIndex(
			Matrix matrix) {
		TwoHashMapsConcatenated<Id<Person>, Integer, Integer> index = new TwoHashMapsConcatenated<>();

		int indexPersonId = matrix.getColumnIndex("personId");
		int indexLeg = matrix.getColumnIndex("legIndex");

		for (int i = 1; i < matrix.getNumberOfRows(); i++) {
			index.put(Id.create(matrix.getString(i, indexPersonId), Person.class),
					matrix.getInteger(i, indexLeg), i);
		}

		return index;
	}
	
	public static TwoHashMapsConcatenated<Id<Person>, Integer, String> getColumnValues(
			Matrix matrix, int columnIndex) {
		
		TwoHashMapsConcatenated<Id<Person>, Integer, String> values = new TwoHashMapsConcatenated<>();
		
		int indexPersonId=matrix.getColumnIndex("personId");
		int indexLeg=matrix.getColumnIndex("legIndex");
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			Id<Person> personId=Id.create(matrix.getString(i, indexPersonId), Person.class);
			int legIndex=matrix.getInteger(i, indexLeg);
			values.put(personId, legIndex, matrix.getString(i, columnIndex));
		}	
		
		return values;
	}

}
