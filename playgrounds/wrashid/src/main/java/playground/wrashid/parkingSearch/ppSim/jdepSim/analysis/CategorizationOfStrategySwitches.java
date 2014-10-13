package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class CategorizationOfStrategySwitches extends
		CompareSelectedParkingPropertyOneRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run179/output/";
		int referenceIteration = 1249;
		int startIteration = 1240;
		int endIteration = referenceIteration - 1;
		int iterationStep = 1;
		boolean ignoreCasesWithBothPPUse = true;

		Matrix eventsReferenceMatrix = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						referenceIteration));
		Matrix eventsMatrixCurrentIter = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));

		System.out.print("iteration");
		System.out.print("\t");
		System.out.print("pctSameStrategy_conseqIter");
		System.out.print("\t");
		System.out.print("pctOfStrategySwitchesWithinSameGroup_conseqIter");
		System.out.print("\t");
		System.out.print("pctOfStrategySwitchesToDiffGroup_conseqIter");
		System.out.print("\t");
		System.out.print("pctSameStrategy_refIter");
		System.out.print("\t");
		System.out.print("pctOfStrategySwitchesWithinSameGroup_refIter");
		System.out.print("\t");
		System.out.print("pctOfStrategySwitchesToDiffGroup_refIter");
		System.out.println();
		
		for (int i = startIteration; i <= endIteration; i += iterationStep) {
			Matrix eventsMatrixNextIter = GeneralLib
					.readStringMatrix(getEventsFileName(outputFolder, i + 1));
			
			double pctSameStrategy_conseqIter=100-percentageOfDifferentParkingUsages(
					eventsMatrixCurrentIter, eventsMatrixNextIter,
					"parkingStrategy",ignoreCasesWithBothPPUse);
			
			double pctOfStrategySwitchesWithinSameGroup_conseqIter = percentageOfStrategySwitchesWithinSameGroup(
					eventsMatrixCurrentIter, eventsMatrixNextIter,ignoreCasesWithBothPPUse)*((100-pctSameStrategy_conseqIter)/100);
			
			double pctOfStrategySwitchesToDiffGroup_conseqIter=100-pctSameStrategy_conseqIter-pctOfStrategySwitchesWithinSameGroup_conseqIter;
			
			
			double pctSameStrategy_refIter=100-percentageOfDifferentParkingUsages(eventsReferenceMatrix,
					eventsMatrixCurrentIter, "parkingStrategy",ignoreCasesWithBothPPUse);
			
			double pctOfStrategySwitchesWithinSameGroup_refIter = percentageOfStrategySwitchesWithinSameGroup(eventsReferenceMatrix,
					eventsMatrixCurrentIter,ignoreCasesWithBothPPUse)*((100-pctSameStrategy_refIter)/100);
			
			double pctOfStrategySwitchesToDiffGroup_refIter=100-pctSameStrategy_refIter-pctOfStrategySwitchesWithinSameGroup_refIter;
			
			System.out.print(i);
			System.out.print("\t");
			System.out.print(pctSameStrategy_conseqIter);
			System.out.print("\t");
			System.out.print(pctOfStrategySwitchesWithinSameGroup_conseqIter);
			System.out.print("\t");
			System.out.print(pctOfStrategySwitchesToDiffGroup_conseqIter);
			System.out.print("\t");
			System.out.print(pctSameStrategy_refIter);
			System.out.print("\t");
			System.out.print(pctOfStrategySwitchesWithinSameGroup_refIter);
			System.out.print("\t");
			System.out.print(pctOfStrategySwitchesToDiffGroup_refIter);
			System.out.println();
			
			eventsMatrixCurrentIter = eventsMatrixNextIter;
		}

	}
	
	
	public static double percentageOfStrategySwitchesWithinSameGroup(
			Matrix matrixA,
			Matrix matrixB, boolean ignoreCasesWithBothPPUse) {
		
		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id<Person>, Integer, Integer> indexMatrixB=getIndex(matrixB);
		
		int indexPersonId=matrixA.getColumnIndex("personId");
		int indexLeg=matrixA.getColumnIndex("legIndex");
		
		int indexParkingStrategy=matrixA.getColumnIndex("parkingStrategy");
		
		int indexGroupName=matrixA.getColumnIndex("groupName");
		
		int totalNumberOfStrategyChanges=0;
		int numberOfStrategySwitchesWithinSameGroup=0;
		for (int i=1;i<matrixA.getNumberOfRows();i++){
			Id<Person> personId=Id.create(matrixA.getString(i, indexPersonId), Person.class);
			int legIndex=matrixA.getInteger(i, indexLeg);
			
			String parkingStrategyA=matrixA.getString(i, indexParkingStrategy);
			String parkingStrategyB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexParkingStrategy);
			
			String groupNameA=matrixA.getString(i, indexGroupName);
			String groupNameB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexGroupName);
			
			if (ignoreCasesWithBothPPUse){
				int indexFacilityId=matrixA.getColumnIndex("FacilityId");
				String facilityIdA=matrixA.getString(i, indexFacilityId);
				String facilityIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexFacilityId);
				
				if (!(facilityIdA.toString().contains("private") && facilityIdB.toString().contains("private") || facilityIdA.toString().contains("publicPOutside") && facilityIdB.toString().contains("publicPOutside"))){
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
