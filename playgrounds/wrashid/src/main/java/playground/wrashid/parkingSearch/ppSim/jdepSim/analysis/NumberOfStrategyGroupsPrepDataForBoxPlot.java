package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.LinkedList;

import org.matsim.contrib.parking.lib.obj.StringMatrix;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.analysis.StrategyScoresAnalysis.StrategyScoreLog;

public class NumberOfStrategyGroupsPrepDataForBoxPlot extends AverageNumberOfStrategyGroups {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int startIteration = 0;
		int endIteration = 10;
		int iterationStep = 1;
		String runOutputFolder = "C:/data/parkingSearch/psim/zurich/output/run20/output/";
		String outputPath="C:/tmp2/AverageNumberOfStrategyGroups-boxPlotInputData.txt";
		boolean removePrivateParking = false;
		
		
		StringMatrix table=new StringMatrix();
		for (int i = startIteration; i < endIteration; i += iterationStep) {
			table.putString(0, i, "it." + i);
			System.out.println("processing iteration: " + i);
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> parkingScores = StrategyScoresAnalysis
					.getScores(runOutputFolder, i, removePrivateParking);
			int row = 1;
			for (LinkedList<StrategyScoreLog> strategyScores : parkingScores
					.getValues()) {
				table.putString(row, i, Integer.toString(getNumberOfStrategies(strategyScores)));
				row++;
			}
		}
		table.writeMatrix(outputPath);
	}

}
