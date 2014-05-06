package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class CompareSelectedParkingPropertyOneRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder="H:/data/experiments/parkingSearchOct2013/runs/run143/output/";
		int referenceIteration=499;
		int startIteration=400;
		int endIteration=referenceIteration-1;
		String fieldName="FacilityId";
		//String fieldName="parkingStrategy";
		//String fieldName="groupName";
		
		
		StringMatrix eventsReferenceMatrix = GeneralLib.readStringMatrix(getEventsFileName(outputFolder,referenceIteration));
		StringMatrix eventsMatrixCurrentIter = GeneralLib.readStringMatrix(getEventsFileName(outputFolder,startIteration));
		
		System.out.println("iteration\tpctDiffConsequete\tpctDiffReference");
		for (int i=startIteration;i<=endIteration;i++){
			StringMatrix eventsMatrixNextIter = GeneralLib.readStringMatrix(getEventsFileName(outputFolder,i+1));	
			
			double pctDiffConsequete = percentageOfDifferentParkingUsages(eventsMatrixCurrentIter,eventsMatrixNextIter,fieldName);
			double pctDiffReference = percentageOfDifferentParkingUsages(eventsReferenceMatrix,eventsMatrixCurrentIter,fieldName);
			
			System.out.println(i + "\t" + pctDiffConsequete + "\t" + pctDiffReference);
			
			eventsMatrixCurrentIter=eventsMatrixNextIter;
		}
	}
	
	public static String getEventsFileName(String outputFolder, int iterationNumber) {
		String fileName = outputFolder + "/ITERS/" + iterationNumber +".parkingEvents.txt.gz";
		return fileName;
	}
	
	public static double percentageOfDifferentParkingUsages(
			StringMatrix matrixA,
			StringMatrix matrixB, String fieldName) {
		
		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id, Integer, Integer> indexMatrixB=getIndex(matrixB);
		
		int indexPersonId=matrixA.getColumnIndex("personId");
		int indexLeg=matrixA.getColumnIndex("legIndex");
		int indexSelectedField=matrixA.getColumnIndex(fieldName);
		
		int totalNumberOfParkingOperations=0;
		int numberOfTimesSameParkingFacilityChosen=0;
		for (int i=1;i<matrixA.getNumberOfRows();i++){
			totalNumberOfParkingOperations++;
			Id personId=new IdImpl(matrixA.getString(i, indexPersonId));
			int legIndex=matrixA.getInteger(i, indexLeg);
			
			String facilityIdA=matrixA.getString(i, indexSelectedField);
			String facilityIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexSelectedField);
			
			if (facilityIdA.equalsIgnoreCase(facilityIdB)){
				numberOfTimesSameParkingFacilityChosen++;
			}
		}
		
		return (1-(1.0*numberOfTimesSameParkingFacilityChosen/totalNumberOfParkingOperations))*100;
	}
	
	public static TwoHashMapsConcatenated<Id, Integer, Integer> getIndex(
			StringMatrix matrix) {
		TwoHashMapsConcatenated<Id, Integer, Integer> index=new TwoHashMapsConcatenated<Id, Integer, Integer>();
		
		int indexPersonId=matrix.getColumnIndex("personId");
		int indexLeg=matrix.getColumnIndex("legIndex");
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			index.put(new IdImpl(matrix.getString(i, indexPersonId)), matrix.getInteger(i, indexLeg), i);
		}
		
		return index;
	}

}
