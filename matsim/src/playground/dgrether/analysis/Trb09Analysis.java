/* *********************************************************************** *
 * project: org.matsim.*
 * Trb09Analysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.analysis;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.ScenarioImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.charts.DgAvgDeltaMoneyGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaMoneyQuantilesChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsQuantilesChart;
import playground.dgrether.analysis.charts.DgDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgMixedDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgModalSplitGroupChart;
import playground.dgrether.analysis.charts.DgModalSplitQuantilesChart;
import playground.dgrether.analysis.io.DgAnalysisPopulationReader;
import playground.dgrether.analysis.io.DgHouseholdsAnalysisReader;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.utils.charts.DgChartWriter;


public class Trb09Analysis {

	private static final Logger log = Logger.getLogger(Trb09Analysis.class);
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netfile, plans1file, plans2file, housholdsfile;
		int threshold;
		
//		boolean isTestscenario = true;
//		String runNumber1 = "860";
//		String runNumber2 = "864";
		
		boolean isTestscenario = false;		
		String runNumber1 = "749";
		String runNumber2 = "867";
		
		String runid1 = "run" + runNumber1;
		String runid2 = "run" + runNumber2;
			
		String runiddot1 = runid1 + ".";
		String runiddot2 = runid2 + ".";
		
		if (isTestscenario){
			netfile = DgPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_network.xml.gz";
			plans1file = DgPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_plans.xml.gz";
			plans2file = DgPaths.RUNBASE + runid2 + "/" + runiddot2 + "output_plans.xml.gz";			
			housholdsfile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/households.xml";
			threshold = 4;
		}
		else {
			netfile = DgPaths.RUNBASE + runid1 + "/" + runNumber1 + ".output_network.xml.gz";
			plans1file = DgPaths.RUNBASE + runid1 + "/" + runNumber1 + ".output_plans.xml.gz";
			plans2file = DgPaths.RUNBASE + runid2 + "/" + runNumber2 + ".output_plans.xml.gz";
			housholdsfile = DgPaths.STUDIESDG + "einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";
			threshold = 100;
		}

		
		String modalSplitGroupChartFileRun1 = DgPaths.RUNBASE + runid1 + "/"+runNumber1+"modalSplitGroupChart";
		String modalSplitGroupChartFileRun2 = DgPaths.RUNBASE + runid2 + "/"+runNumber2+"modalSplitGroupChart";
		
		String deltaUtilsModeGroupChartFile = DgPaths.RUNBASE + runid2 + "/deltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		String avgDeltaUtilsGroupChartFile = DgPaths.RUNBASE + runid2 + "/avgDeltaUtilsGroupChart"+runNumber1+"vs"+runNumber2;
		String avgDeltaUtilsModeGroupChartFile = DgPaths.RUNBASE + runid2 + "/avgDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		String avgDeltaMoneyGroupChartFile = DgPaths.RUNBASE + runid2 + "/avgDeltaMoneyGroupChart"+runNumber1+"vs"+runNumber2;

		String mixedDeltaUtilsModeGroupChartFile = DgPaths.RUNBASE + runid2 + "/mixedDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;
		String mixedMsoDeltaUtilsModeGroupChartFile = DgPaths.RUNBASE + runid2 + "/mixedMsoDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		//quantile charts
		String modalSplitQuantilesChartFileRun1 = DgPaths.RUNBASE + runid1 + "/"+runNumber1+"modalSplitQuantilesChart";
		String modalSplitQuantilesChartFileRun2 = DgPaths.RUNBASE + runid2 + "/"+runNumber2+"modalSplitQuantilesChart";

		String avgDeltaUtilsQuantilesChartFile = DgPaths.RUNBASE + runid2 + "/avgDeltaUtilsQuantilesChart" + runNumber1 + "vs" + runNumber2;

		String avgDeltaMoneyQuantilesChartFile = DgPaths.RUNBASE + runid2 + "/avgDeltaMoneyQuantilesChart"+runNumber1+"vs"+runNumber2;

		

		ScenarioImpl sc = new ScenarioImpl();


		DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader(sc);
		DgAnalysisPopulation ana = pc.doPopulationAnalysis(netfile, plans1file, plans2file);

		DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
		hhr.readHousholds(housholdsfile);
			
		DgModalSplitGroupChart modalSplitGroupChartRun1 = new DgModalSplitGroupChart(ana, DgAnalysisPopulation.RUNID1, threshold);
		DgChartWriter.writerChartToFile(modalSplitGroupChartFileRun1, modalSplitGroupChartRun1.createChart());
		DgModalSplitGroupChart modalSplitGroupChartRun2 = new DgModalSplitGroupChart(ana, DgAnalysisPopulation.RUNID2, threshold);
		DgChartWriter.writerChartToFile(modalSplitGroupChartFileRun2, modalSplitGroupChartRun2.createChart());

		DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart;
		deltaUtilsModeGroupChart = new DgDeltaUtilsModeGroupChart(ana);
		DgChartWriter.writerChartToFile(deltaUtilsModeGroupChartFile, deltaUtilsModeGroupChart.createChart());

		DgAvgDeltaUtilsGroupChart avgDeltaUtilsGroupChart = new DgAvgDeltaUtilsGroupChart(ana, threshold);
		//			DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsGroupChart.createChart());
		DgChartWriter.writerChartToFile(avgDeltaUtilsGroupChartFile, avgDeltaUtilsGroupChart.createChart());

		DgAvgDeltaUtilsModeGroupChart avgDeltaUtilsModeGroupChart = new DgAvgDeltaUtilsModeGroupChart(ana, threshold);
		//			DgChartFrame frame = new DgChartFrame("test", avgDScoreIncomeChartData.createChart());
		DgChartWriter.writerChartToFile(avgDeltaUtilsModeGroupChartFile, avgDeltaUtilsModeGroupChart.createChart());

		DgAvgDeltaMoneyGroupChart avgDeltaMoneyGroupChart = new DgAvgDeltaMoneyGroupChart(ana, threshold);
		//		DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsGroupChart.createChart());
		DgChartWriter.writerChartToFile(avgDeltaMoneyGroupChartFile, avgDeltaMoneyGroupChart.createChart());

		writeMixedDeltaUtilsModeGroupChart(deltaUtilsModeGroupChart, avgDeltaUtilsModeGroupChart, 
				mixedDeltaUtilsModeGroupChartFile, mixedMsoDeltaUtilsModeGroupChartFile);

		//quantile charts
		DgModalSplitQuantilesChart modalSplitQuantilesChartRun1 = new DgModalSplitQuantilesChart(ana, DgAnalysisPopulation.RUNID1);
		//			DgChartFrame frame = new DgChartFrame("test", modalSplitQuantilesChartRun1.createChart());
		DgChartWriter.writerChartToFile(modalSplitQuantilesChartFileRun1, modalSplitQuantilesChartRun1.createChart());
		DgModalSplitQuantilesChart modalSplitQuantilesChartRun2 = new DgModalSplitQuantilesChart(ana, DgAnalysisPopulation.RUNID2);
		DgChartWriter.writerChartToFile(modalSplitQuantilesChartFileRun2, modalSplitQuantilesChartRun2.createChart());


		DgAvgDeltaUtilsQuantilesChart avgDeltaUtilsQuantilesChart = new DgAvgDeltaUtilsQuantilesChart(ana);
		//			DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsQuantilesChart.createChart());
		DgChartWriter.writerChartToFile(avgDeltaUtilsQuantilesChartFile, avgDeltaUtilsQuantilesChart.createChart());

		DgAvgDeltaMoneyQuantilesChart avgDeltaMoneyQuantilesChart = new DgAvgDeltaMoneyQuantilesChart(ana);
		//		DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsQuantilesChart.createChart());
		JFreeChart jfChart = avgDeltaMoneyQuantilesChart.createChart();
		DgChartWriter.writeChartDataToFile(avgDeltaMoneyQuantilesChartFile, jfChart);
		DgChartWriter.writerChartToFile(avgDeltaMoneyQuantilesChartFile, jfChart);

		log.debug("ya esta ;-)");
			
	}
	
  public static void writeMixedDeltaUtilsModeGroupChart(DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart, 
  		DgAvgDeltaUtilsModeGroupChart avgDScoreModeIncomeChartData, 
  		String mixedDeltaScoreIncomeChartFile, String mixedMsoDeltaScoreIncomeChartFile){
		DgMixedDeltaScoreIncomeModeChoiceChart mixedDsIncomeChart = new DgMixedDeltaScoreIncomeModeChoiceChart();
		XYSeriesCollection modeChoiceDataset = deltaUtilsModeGroupChart.createDeltaScoreIncomeModeChoiceDataset();
		mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceDataset);
		XYSeriesCollection avgScoreDataset = avgDScoreModeIncomeChartData.getDataset();
		mixedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgScoreDataset);
//		DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
		DgChartWriter.writerChartToFile(mixedDeltaScoreIncomeChartFile, mixedDsIncomeChart.createChart());
		
		XYSeriesCollection ds2 = new XYSeriesCollection();
		ds2.addSeries(modeChoiceDataset.getSeries(2));
		ds2.addSeries(modeChoiceDataset.getSeries(3));
		XYSeriesCollection ds3 = new XYSeriesCollection();
		ds3.addSeries(avgScoreDataset.getSeries(2));
		ds3.addSeries(avgScoreDataset.getSeries(3));
		DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart mixedSwichterOnlyDsIncomeChart = new DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart();
		mixedSwichterOnlyDsIncomeChart.addIncomeModeChoiceDataSet(ds2);
		mixedSwichterOnlyDsIncomeChart.addAvgDeltaScoreIncomeDs(ds3);
//		DgChartFrame frame = new DgChartFrame("test", mixedSwichterOnlyDsIncomeChart.createChart());
		DgChartWriter.writerChartToFile(mixedMsoDeltaScoreIncomeChartFile, mixedSwichterOnlyDsIncomeChart.createChart());
  }

}
