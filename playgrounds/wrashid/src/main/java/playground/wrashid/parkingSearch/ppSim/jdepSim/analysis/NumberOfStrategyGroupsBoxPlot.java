package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.LinkedList;

import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.analysis.StrategyScoresAnalysis.StrategyScoreLog;

public class NumberOfStrategyGroupsBoxPlot extends AverageNumberOfStrategyGroups {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int startIteration = 0;
		int endIteration = 10;
		int iterationStep = 1;
		String runOutputFolder = "C:/data/parkingSearch/psim/zurich/output/run20/output/";
		String boxPlotOutputFile="C:/tmp/abc.png";
		boolean removePrivateParking = false;
		
		
		Matrix table=new Matrix();
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
		String tempOutputPath=new RIntegration().getPathToTempFile("txt");
		System.out.println(tempOutputPath);
		table.writeMatrix(tempOutputPath);
		
		new RIntegration().generateBoxPlot(tempOutputPath, boxPlotOutputFile, "Number of Strategy Groups", "iteration", "number of strategy Groups", null);
	}

}
