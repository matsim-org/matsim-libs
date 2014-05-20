package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class PctStrategySwitchWithinSameGroup extends
		CompareSelectedParkingPropertyOneRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run135/output/";
		int referenceIteration = 410;
		int startIteration = 400;
		int endIteration = referenceIteration - 1;
		int iterationStep = 1;
		boolean ignoreCasesWithBothPPUse = true;

		Matrix eventsReferenceMatrix = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						referenceIteration));
		Matrix eventsMatrixCurrentIter = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));

		System.out
				.println("iteration\tpcConsequeteIteration-SwitchWithinSameStrategyGroup\tpctReferenceIteration-SwitchWithinSameStrategyGroup");
		for (int i = startIteration; i <= endIteration; i += iterationStep) {
			Matrix eventsMatrixNextIter = GeneralLib
					.readStringMatrix(getEventsFileName(outputFolder, i + 1));
			
			System.out.print(i
					+ "\t"
					+ percentageOfStrategySwitchesWithinSameGroup(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"FacilityId",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfStrategySwitchesWithinSameGroup(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "FacilityId",ignoreCasesWithBothPPUse));
			System.out.println();
			
			eventsMatrixCurrentIter = eventsMatrixNextIter;
		}

	}
	
	
	public static double percentageOfStrategySwitchesWithinSameGroup(
			Matrix matrixA,
			Matrix matrixB, String fieldName, boolean ignoreCasesWithBothPPUse) {
		
		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id, Integer, Integer> indexMatrixB=getIndex(matrixB);
		
		int indexPersonId=matrixA.getColumnIndex("personId");
		int indexLeg=matrixA.getColumnIndex("legIndex");
		
		int indexParkingStrategy=matrixA.getColumnIndex("parkingStrategy");
		
		int indexGroupName=matrixA.getColumnIndex("groupName");
		
		int totalNumberOfStrategyChanges=0;
		int numberOfStrategySwitchesWithinSameGroup=0;
		for (int i=1;i<matrixA.getNumberOfRows();i++){
			Id personId=new IdImpl(matrixA.getString(i, indexPersonId));
			int legIndex=matrixA.getInteger(i, indexLeg);
			
			String parkingStrategyA=matrixA.getString(i, indexParkingStrategy);
			String parkingStrategyB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexParkingStrategy);
			
			String groupNameA=matrixA.getString(i, indexGroupName);
			String groupNameB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexGroupName);
			
			if (ignoreCasesWithBothPPUse){
				int indexFacilityId=matrixA.getColumnIndex("FacilityId");
				String facilityIdA=matrixA.getString(i, indexFacilityId);
				String facilityIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexFacilityId);
				
				if (!(facilityIdA.toString().contains("private") && facilityIdB.toString().contains("private"))){
					if (!parkingStrategyA.equalsIgnoreCase(parkingStrategyB)){
						totalNumberOfStrategyChanges++;
						
						if (groupNameA.equalsIgnoreCase(groupNameB)){
							numberOfStrategySwitchesWithinSameGroup++;
						}
					}
				}
			} else {
				totalNumberOfStrategyChanges++;
				
				if (!parkingStrategyA.equalsIgnoreCase(parkingStrategyB)){
					totalNumberOfStrategyChanges++;
					
					if (groupNameA.equalsIgnoreCase(groupNameB)){
						numberOfStrategySwitchesWithinSameGroup++;
					}
				}
			}
		}
		
		return (1-(1.0*numberOfStrategySwitchesWithinSameGroup/totalNumberOfStrategyChanges))*100;
	}

}
