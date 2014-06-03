package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.analysis.StrategyScoresAnalysis.StrategyScoreLog;

/**
 * compares strategy, which achieved best score during 80% MNL phase to
 * strategies in reference iteration (full MNL). => is best score strategy part
 * of memory? => is best score strategy executed at reference iteraiton?
 * 
 * @author wrashid
 * 
 */
public class BestScoreVsAdaptiveStrategies {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int startIteration = 0;
		int endIteration = 198;
		int iterationStep = 1;
		int relaxedIteration=499;
		
		int startFullMNLIteration=450;
		int endFullMNLIteration=499;
		
		boolean removePrivateParking = true;
		String runOutputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run145/output/";

		TwoHashMapsConcatenated<String, Integer, StrategyScoreLog> bestStrategies = new TwoHashMapsConcatenated<String, Integer, StrategyScoreLog>();
		for (int i = startIteration; i < endIteration; i += iterationStep) {
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> parkingScores = StrategyScoresAnalysis
					.getAllScores(runOutputFolder, i);

			for (String personId : parkingScores.getKeySet1()) {
				for (Integer legIndex : parkingScores.getKeySet2(personId)) {
					for (StrategyScoreLog currentStrategyScore : parkingScores
							.get(personId, legIndex)) {
						
						if (!bestStrategies.containsValue(personId, legIndex) || currentStrategyScore.score >bestStrategies.get(personId, legIndex).score){
							bestStrategies.put(personId, legIndex, currentStrategyScore);
						}
					}

				}
			}

		}

		// compare best strategy with strategies in memory at relaxed iteration
		TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> referenceScores = StrategyScoresAnalysis
				.getScores(runOutputFolder, relaxedIteration, removePrivateParking);

		int numberOfBestScoreStrategiesPartOfMemoryAtFinalIteration=0;

		for (String personId : referenceScores.getKeySet1()) {
			for (Integer legIndex : referenceScores.getKeySet2(personId)) {
				
				StrategyScoreLog bestStrategyAtFinalIteration=null;
				
				boolean bestScoreStrategyIsInMemoryAtFinalIteration=false;
				for (StrategyScoreLog currentStrategyScore : referenceScores
						.get(personId, legIndex)) {
					
					if (bestStrategyAtFinalIteration==null){
						bestStrategyAtFinalIteration=currentStrategyScore;
					} else {
						if (currentStrategyScore.score >bestStrategyAtFinalIteration.score){
							bestStrategyAtFinalIteration=currentStrategyScore;
						}
					}
					
					if (currentStrategyScore.strategyName.equalsIgnoreCase(bestStrategies.get(personId, legIndex).strategyName)){
						bestScoreStrategyIsInMemoryAtFinalIteration=true;
					}
				}
				
				if (bestScoreStrategyIsInMemoryAtFinalIteration){
					numberOfBestScoreStrategiesPartOfMemoryAtFinalIteration++;
				}

			}
		}
		
		System.out.println("pctOfBestScoreStrategiesPartOfMemoryAtFinalIteration:" + 100.0 * numberOfBestScoreStrategiesPartOfMemoryAtFinalIteration /bestStrategies.getValues().size());
	
	
		TwoHashMapsConcatenated<String, Integer, Boolean> bestScoreStrategiesExecutedAtFullMNL = new TwoHashMapsConcatenated<String, Integer, Boolean>();
		
		for (int i = startFullMNLIteration; i < endFullMNLIteration; i += iterationStep) {
			TwoHashMapsConcatenated<String, Integer, ArrayList<String>> parkingEvents = StrategyScoresAnalysis
					.getParkingEvents(runOutputFolder, i);

			for (String personId : parkingEvents.getKeySet1()) {
				for (Integer legIndex : parkingEvents.getKeySet2(personId)) {
					
					int strategyColIndex=10;
					int facilityIdColIndex=6;
					
					String executedStrategy = parkingEvents.get(personId, legIndex).get(strategyColIndex);
					String facilityId = parkingEvents.get(personId, legIndex).get(facilityIdColIndex);

					if (removePrivateParking){
						if (!(facilityId.contains("stp") || facilityId.contains("gp")  || facilityId.contains("illegal"))){
							continue;
						}
					}
					
					if (!bestStrategies.containsValue(personId, legIndex)){
						bestScoreStrategiesExecutedAtFullMNL.put(personId, legIndex, new Boolean(false));
					}
					
					if (executedStrategy.equalsIgnoreCase(bestStrategies.get(personId, legIndex).strategyName)){
						bestScoreStrategiesExecutedAtFullMNL.put(personId, legIndex, new Boolean(true));
					}
				}
			}

		}
		
		
		
		int numberOfBestScoreStrategiesExecutedAtFullMNL=0;
		
		for (Boolean bestScoreStrategyExecuted:bestScoreStrategiesExecutedAtFullMNL.getValues()){
			if (bestScoreStrategyExecuted){
				numberOfBestScoreStrategiesExecutedAtFullMNL++;
			}
		}
		
	
		System.out.println("pctOfBestScoreStrategiesExecutedAtFullMNL:" + 100.0 * numberOfBestScoreStrategiesExecutedAtFullMNL /bestScoreStrategiesExecutedAtFullMNL.getValues().size());
	
	
	}
}
