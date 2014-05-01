package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.analysis.StrategyScoresAnalysis.StrategyScoreLog;

public class AverageNumberOfStrategyGroups {

	public static void main(String[] args) {
		int startIteration=0;
		int endIteration=20;
		int iterationStep=1;
		String runOutputFolder="C:/data/parkingSearch/psim/zurich/output/run20/output/";
		
		
		for (int i=startIteration;i<endIteration;i+=iterationStep){
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> parkingScores = StrategyScoresAnalysis.getScores(
					runOutputFolder,i,true);
			
			int sampleSize=0;
			int sumNumberOfStrategies=0;
			for (LinkedList<StrategyScoreLog> strategyScores:parkingScores.getValues()){
				sumNumberOfStrategies+=getNumberOfStrategies(strategyScores);
				sampleSize++;
			}
			System.out.println(i + ":" + sumNumberOfStrategies/1.0/sampleSize);
		}
		
		
		
	}

	private static int getNumberOfStrategies(
			LinkedList<StrategyScoreLog> strategyScores) {
		HashSet<String> strategies=new HashSet<String>();
		
		for (StrategyScoreLog logElement: strategyScores){
			strategies.add(logElement.strategyName);
		}
		
		if (strategies.size()>5){
			for (StrategyScoreLog logElement: strategyScores){
				//logElement.print();
			}
		}
		
		if (strategies.size()<strategyScores.size()){
			for (StrategyScoreLog logElement: strategyScores){
				logElement.print();
			}
		}
		
		
		return strategies.size();
	}
	
}
