package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.analysis.StrategyScoresAnalysis.StrategyScoreLog;

public class AverageNumberOfStrategyGroups {

	static boolean ignoreNegativeScoreStrategies = true;

	public static void main(String[] args) {
		int startIteration = 0;
		int endIteration = 550;
		int iterationStep = 5;
		boolean removePrivateParking = false;
		String runOutputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run150/output/";

		for (int i = startIteration; i < endIteration; i += iterationStep) {
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> parkingScores = StrategyScoresAnalysis
					.getScores(runOutputFolder, i, removePrivateParking);

			int sampleSize = 0;
			int sumNumberOfStrategies = 0;
			for (LinkedList<StrategyScoreLog> strategyScores : parkingScores
					.getValues()) {

				sumNumberOfStrategies += getNumberOfStrategies(strategyScores);
				sampleSize++;
			}
			System.out.println(i + "\t" + sumNumberOfStrategies / 1.0
					/ sampleSize);
		}
		
	}

	protected static int getNumberOfStrategies(
			LinkedList<StrategyScoreLog> strategyScores) {
		

		HashSet<String> strategies = new HashSet<String>();

		for (StrategyScoreLog logElement : strategyScores) {

			if (logElement.score>Double.NEGATIVE_INFINITY) {
					String[] split = logElement.strategyName.split("-");
					String groupName = "";

					for (int i = 0; i < split.length - 1; i++) {
						if (i == split.length - 2) {
							groupName += split[i];
						} else {
							groupName += split[i] + "-";
						}
					}
					strategies.add(groupName);
			}

		}

		return strategies.size();
	}

}
