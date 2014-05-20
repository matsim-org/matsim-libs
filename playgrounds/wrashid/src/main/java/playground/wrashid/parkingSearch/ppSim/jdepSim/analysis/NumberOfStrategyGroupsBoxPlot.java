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
		int endIteration = 550;
		int iterationStep = 100;
		String runOutputFolder = "f:/data/experiments/parkingSearchOct2013/runs/run150/output/";
		String outpputFolder="C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/diverse/";
		String outputFileName="NumberOfStrategyGroups_run150";
		boolean removePrivateParking = false;
		
		
		Matrix table=new Matrix();
		for (int i = startIteration; i < endIteration; i += iterationStep) {
			table.putString(0, i, i);
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
		String txtOutputPath=outpputFolder + outputFileName + ".txt";
		table.writeMatrix(txtOutputPath);
		
		new RIntegration().generateBoxPlot(txtOutputPath, outpputFolder + outputFileName + ".png", "Number of Strategy Groups", "iteration", "number of strategy Groups", null);
	}

}
