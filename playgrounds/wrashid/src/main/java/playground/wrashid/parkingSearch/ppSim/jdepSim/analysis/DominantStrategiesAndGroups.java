package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

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

		String basePath="f:/data/experiments/parkingSearchOct2013/runs/";
		
//		analyzeRuns(basePath,133,133,450,499);
//		analyzeRuns(basePath,135,140,450,499);
//		
//		
//		analyzeRuns(basePath,144,149,450,499);
//		analyzeRuns(basePath,150,150,950,999);
//		analyzeRuns(basePath,151,156,450,499);
//		analyzeRuns(basePath,157,157,950,999);
//		analyzeRuns(basePath,159,164,450,499);
//		analyzeRuns(basePath,165,171,650,699);
//
//		analyzeRuns(basePath,179,181,1200,1249);
//		
//		analyzeRuns(basePath,182,186,450,499);
//		analyzeRuns(basePath,188,189,450,499);
//		analyzeRuns(basePath,190,191,1950,1999);
//		analyzeRuns(basePath,192,193,2450,2499);
//		analyzeRuns(basePath,194,195,2950,2999);
//		analyzeRuns(basePath,196,197,1950,1999);
//		analyzeRuns(basePath,198,199,2450,2499);
//		analyzeRuns(basePath,200,209,450,499);
//		analyzeRuns(basePath,222,225,450,499);
//		analyzeRuns(basePath,261,265,450,499);
		analyzeRuns(basePath,199,199,2399,2499);
	}
	
	public static void analyzeRuns(String basePath, int runStartId, int runEndId, int startIteration, int endIteration){
		for (int i=runStartId;i<=runEndId;i++){
			startAnalysis_tryCatch(basePath + "run"+ i +"/output/", startIteration, endIteration,true);
		}
	}
	
	public static void startAnalysis_tryCatch(String outputFolder, int startIteration,
			int endIteration, boolean ignoreCasesWithPPUse){
		try{
			startAnalysis(outputFolder, startIteration, endIteration,true);
		}catch (Throwable e){
			e.printStackTrace();
		}
	}
	
	
	private static void startAnalysis(String outputFolder, int startIteration,
			int endIteration, boolean ignoreCasesWithPPUse) {
		
		System.out.println(outputFolder);
		System.out.println("startIteration:" + startIteration);
		System.out.println("endIteration:" + endIteration);
		
		Matrix eventsMatrixCurrentIter = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));
		
		
		int indexPersonId=eventsMatrixCurrentIter.getColumnIndex("personId");
		int indexLeg=eventsMatrixCurrentIter.getColumnIndex("legIndex");
		int indexStrategy=eventsMatrixCurrentIter.getColumnIndex("parkingStrategy");
		int indexGroupName=eventsMatrixCurrentIter.getColumnIndex("groupName");
		int indexFacilityId=eventsMatrixCurrentIter.getColumnIndex("FacilityId");
		
		TwoHashMapsConcatenated<Id<Person>, Integer, String> strategies=getColumnValues(eventsMatrixCurrentIter,indexStrategy);
		TwoHashMapsConcatenated<Id<Person>, Integer, String> strategyGroups=getColumnValues(eventsMatrixCurrentIter,indexGroupName);
		TwoHashMapsConcatenated<Id<Person>, Integer, String> facilityIds=getColumnValues(eventsMatrixCurrentIter,indexFacilityId);
		
		if (ignoreCasesWithPPUse){
			for (Id<Person> personId:facilityIds.getKeySet1()){
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
			if (i%5==0){
				System.out.println("iteration: "+ i);
			}
			
			
			for (int j=1;j<eventsMatrixCurrentIter.getNumberOfRows();j++){
				Id<Person> personId=Id.create(eventsMatrixCurrentIter.getString(j, indexPersonId), Person.class);
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
