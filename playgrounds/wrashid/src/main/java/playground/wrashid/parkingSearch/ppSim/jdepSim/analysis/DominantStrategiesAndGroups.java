package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class DominantStrategiesAndGroups extends
CompareSelectedParkingPropertyOneRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run135/output/";
		int startIteration = 290;
		int endIteration = 340;
		boolean ignoreCasesWithPPUse=true;

		Matrix eventsMatrixCurrentIter = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));
		
		
		int indexPersonId=eventsMatrixCurrentIter.getColumnIndex("personId");
		int indexLeg=eventsMatrixCurrentIter.getColumnIndex("legIndex");
		int indexStrategy=eventsMatrixCurrentIter.getColumnIndex("parkingStrategy");
		int indexGroupName=eventsMatrixCurrentIter.getColumnIndex("groupName");
		int indexFacilityId=eventsMatrixCurrentIter.getColumnIndex("FacilityId");
		
		TwoHashMapsConcatenated<Id, Integer, String> strategies=getColumnValues(eventsMatrixCurrentIter,indexStrategy);
		TwoHashMapsConcatenated<Id, Integer, String> strategyGroups=getColumnValues(eventsMatrixCurrentIter,indexGroupName);
		TwoHashMapsConcatenated<Id, Integer, String> facilityIds=getColumnValues(eventsMatrixCurrentIter,indexFacilityId);
		
		if (ignoreCasesWithPPUse){
			for (Id personId:facilityIds.getKeySet1()){
				for (Integer legIndex:facilityIds.getKeySet2(personId)){
					if (facilityIds.get(personId, legIndex).contains("private") || facilityIds.get(personId, legIndex).contains("publicPOutside")){
						strategies.removeValue(personId, legIndex);
						strategyGroups.removeValue(personId, legIndex);
					}
				}
			}
		}
	
		for (int i = startIteration+1; i < endIteration; i++) {
			eventsMatrixCurrentIter=GeneralLib
					.readStringMatrix(getEventsFileName(outputFolder,
							i+1));
			
			System.out.println("iteration: "+ i);
			
			for (int j=1;j<eventsMatrixCurrentIter.getNumberOfRows();j++){
				Id personId=new IdImpl(eventsMatrixCurrentIter.getString(j, indexPersonId));
				int legIndex=eventsMatrixCurrentIter.getInteger(j, indexLeg);
				
				if (strategies.get(personId, legIndex)!=null){
					if (!strategies.get(personId, legIndex).equalsIgnoreCase(eventsMatrixCurrentIter.getString(j, indexStrategy))){
						strategies.put(personId, legIndex, null);
					}
				}
				
				if (strategyGroups.get(personId, legIndex)!=null){
					if (!strategyGroups.get(personId, legIndex).equalsIgnoreCase(eventsMatrixCurrentIter.getString(j, indexGroupName))){
						strategyGroups.put(personId, legIndex, null);
					}
				}
			}
		}
		
		
		int numberOfDominantStrategies=0;
		for (String strategyName:strategies.getValues()){
			if (strategyName!=null){
				numberOfDominantStrategies++;
			}
		}
		
		System.out.println("percentage of operations with single dominant strategy:" + 100.0*numberOfDominantStrategies/strategies.getValues().size());
		
		int numberOfDominantStrategyGroups=0;
		for (String strategyGroup:strategyGroups.getValues()){
			if (strategyGroup!=null){
				numberOfDominantStrategyGroups++;
			}
		}
		
		System.out.println("percentage of operations with single dominant strategy group:" + 100.0*numberOfDominantStrategyGroups/strategyGroups.getValues().size());
	
	}

	

}
