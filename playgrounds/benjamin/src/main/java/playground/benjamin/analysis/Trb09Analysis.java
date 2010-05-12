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
package playground.benjamin.analysis;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.benjamin.BkPaths;
import playground.dgrether.analysis.charts.DgAvgDeltaMoneyGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaMoneyQuantilesChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsModeQuantilesChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsQuantilesChart;
import playground.dgrether.analysis.charts.DgDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgMixedDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgModalSplitDiffQuantilesChart;
import playground.dgrether.analysis.charts.DgModalSplitGroupChart;
import playground.dgrether.analysis.charts.DgModalSplitQuantilesChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.analysis.io.DgAnalysisPopulationReader;
import playground.dgrether.analysis.io.DgHouseholdsAnalysisReader;
import playground.dgrether.analysis.population.DgAnalysisPopulation;


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
		String runNumber1 = "891";
		String runNumber2 = "899";
		
		String runid1String = "run" + runNumber1;
		String runid2String = "run" + runNumber2;
		
		Id runid1 = new IdImpl(runid1String);
		Id runid2 = new IdImpl(runid2String);
			
		String runiddot1 = runid1String + ".";
		String runiddot2 = runid2String + ".";
		
		if (isTestscenario){
			netfile = BkPaths.RUNSSVN + runid1String + "/" + runiddot1 + "output_network.xml.gz";
			plans1file = BkPaths.RUNSSVN + runid1String + "/" + runiddot1 + "output_plans.xml.gz";
			plans2file = BkPaths.RUNSSVN + runid2String + "/" + runiddot2 + "output_plans.xml.gz";			
			housholdsfile = BkPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/households.xml";
			threshold = 4;
		}
		else {
			netfile = BkPaths.RUNSSVN + runid1String + "/" + runNumber1 + ".output_network.xml.gz";
			plans1file = BkPaths.RUNSSVN + runid1String + "/" + runNumber1 + ".output_plans.xml.gz";
			plans2file = BkPaths.RUNSSVN + runid2String + "/" + runNumber2 + ".output_plans.xml.gz";
			housholdsfile = BkPaths.SHAREDSVN + "/studies/dgrether/einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";
			threshold = 100;
		}

		
//		runid1 += "best";
//		runid2 += "best";
//		
//		File file = new File(BkPaths.RUNSSVN + runid1);
//		file.mkdir();
//		File file2 = new File(BkPaths.RUNSSVN + runid2);
//		file2.mkdir();
//		
		String modalSplitGroupChartFileRun1 = BkPaths.RUNSSVN + runid1String + "/"+runNumber1+"modalSplitGroupChart";
		String modalSplitGroupChartFileRun2 = BkPaths.RUNSSVN + runid2String + "/"+runNumber2+"modalSplitGroupChart";
		
		String deltaUtilsModeGroupChartFile = BkPaths.RUNSSVN + runid2String + "/deltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		String avgDeltaUtilsGroupChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaUtilsGroupChart"+runNumber1+"vs"+runNumber2;
		String avgDeltaUtilsModeGroupChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		String avgDeltaMoneyGroupChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaMoneyGroupChart"+runNumber1+"vs"+runNumber2;

		String mixedDeltaUtilsModeGroupChartFile = BkPaths.RUNSSVN + runid2String + "/mixedDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;
		String mixedMsoDeltaUtilsModeGroupChartFile = BkPaths.RUNSSVN + runid2String + "/mixedMsoDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		//quantile charts
		String modalSplitQuantilesChartFileRun1 = BkPaths.RUNSSVN + runid1String + "/"+runNumber1+"modalSplitQuantilesChart";
		String modalSplitQuantilesChartFileRun2 = BkPaths.RUNSSVN + runid2String + "/"+runNumber2+"modalSplitQuantilesChart";
		String modalSplitDiffQuantilesChartFileRun2 = BkPaths.RUNSSVN + runid2String + "/modalSplitQuantilesChart" + runNumber1 + "vs" +runNumber2;

		String avgDeltaUtilsQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaUtilsQuantilesChart" + runNumber1 + "vs" + runNumber2;
		String avgDeltaUtilsModeQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaUtilsModeQuantilesChart"+runNumber1+"vs"+runNumber2;

		String avgDeltaMoneyQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaMoneyQuantilesChart"+runNumber1+"vs"+runNumber2;

		

		DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader();
		DgAnalysisPopulation ana = new DgAnalysisPopulation();
//		DgAnalysisPopulation ana = pc.doPopulationAnalysis(netfile, plans1file, plans2file);
		pc.readAnalysisPopulation(ana, runid1, netfile, plans1file);
		pc.readAnalysisPopulation(ana, runid2, netfile, plans2file);
		

		DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
		hhr.readHousholds(housholdsfile);
		ana.calculateIncomeData();
			
		DgModalSplitGroupChart modalSplitGroupChartRun1 = new DgModalSplitGroupChart(ana, runid1, threshold);
		DgChartWriter.writeChart(modalSplitGroupChartFileRun1, modalSplitGroupChartRun1.createChart());
		DgModalSplitGroupChart modalSplitGroupChartRun2 = new DgModalSplitGroupChart(ana, runid2, threshold);
		DgChartWriter.writeChart(modalSplitGroupChartFileRun2, modalSplitGroupChartRun2.createChart());

		
		DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart = new DgDeltaUtilsModeGroupChart(ana, runid1, runid2);
//		DgChartFrame frame = new DgChartFrame("test", deltaUtilsModeGroupChart.createChart());
		DgChartWriter.writeChart(deltaUtilsModeGroupChartFile, deltaUtilsModeGroupChart.createChart());

		DgAvgDeltaUtilsGroupChart avgDeltaUtilsGroupChart = new DgAvgDeltaUtilsGroupChart(ana, threshold,  runid1, runid2);
		//			DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsGroupChart.createChart());
		DgChartWriter.writeChart(avgDeltaUtilsGroupChartFile, avgDeltaUtilsGroupChart.createChart());

		DgAvgDeltaUtilsModeGroupChart avgDeltaUtilsModeGroupChart = new DgAvgDeltaUtilsModeGroupChart(ana, threshold,  runid1, runid2);
		//			DgChartFrame frame = new DgChartFrame("test", avgDScoreIncomeChartData.createChart());
		DgChartWriter.writeChart(avgDeltaUtilsModeGroupChartFile, avgDeltaUtilsModeGroupChart.createChart());

		DgAvgDeltaMoneyGroupChart avgDeltaMoneyGroupChart = new DgAvgDeltaMoneyGroupChart(ana, threshold,  runid1, runid2);
		//		DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsGroupChart.createChart());
		DgChartWriter.writeChart(avgDeltaMoneyGroupChartFile, avgDeltaMoneyGroupChart.createChart());



		//quantile charts
		DgModalSplitQuantilesChart modalSplitQuantilesChartRun1 = new DgModalSplitQuantilesChart(ana, runid1);
		//			DgChartFrame frame = new DgChartFrame("test", modalSplitQuantilesChartRun1.createChart());
		DgChartWriter.writeChart(modalSplitQuantilesChartFileRun1, modalSplitQuantilesChartRun1.createChart());
		DgModalSplitQuantilesChart modalSplitQuantilesChartRun2 = new DgModalSplitQuantilesChart(ana, runid2);
		DgChartWriter.writeChart(modalSplitQuantilesChartFileRun2, modalSplitQuantilesChartRun2.createChart());

		DgModalSplitDiffQuantilesChart modalSplitDiffQuantilesChartRun2 = new DgModalSplitDiffQuantilesChart(ana, runid1,  runid2);
//		DgChartFrame frame = new DgChartFrame("test", modalSplitDiffQuantilesChartRun2.createChart());
		DgChartWriter.writeChart(modalSplitDiffQuantilesChartFileRun2, modalSplitDiffQuantilesChartRun2.createChart());

		
		DgAvgDeltaUtilsQuantilesChart avgDeltaUtilsQuantilesChart = new DgAvgDeltaUtilsQuantilesChart(ana, runid1, runid2);
//					DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsQuantilesChart.createChart());
		DgChartWriter.writeChart(avgDeltaUtilsQuantilesChartFile, avgDeltaUtilsQuantilesChart.createChart());

		DgAvgDeltaUtilsModeQuantilesChart avgDeltaUtilesModeQuantilesChart = new DgAvgDeltaUtilsModeQuantilesChart(ana, threshold,  runid1, runid2);
//		DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilesModeQuantilesChart.createChart());
		DgChartWriter.writeChart(avgDeltaUtilsModeQuantilesChartFile, avgDeltaUtilesModeQuantilesChart.createChart());
		
		DgAvgDeltaMoneyQuantilesChart avgDeltaMoneyQuantilesChart = new DgAvgDeltaMoneyQuantilesChart(ana, runid1, runid2);
		//		DgChartFrame frame = new DgChartFrame("test", avgDeltaUtilsQuantilesChart.createChart());
		JFreeChart jfChart = avgDeltaMoneyQuantilesChart.createChart();
		DgChartWriter.writeChart(avgDeltaMoneyQuantilesChartFile, jfChart);

		writeMixedDeltaUtilsModeGroupChart(deltaUtilsModeGroupChart, avgDeltaUtilesModeQuantilesChart, 
				mixedDeltaUtilsModeGroupChartFile, mixedMsoDeltaUtilsModeGroupChartFile,  runid1, runid2);
		
		log.debug("ya esta ;-)");
			
	}
	
  public static void writeMixedDeltaUtilsModeGroupChart(DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart, 
  		DgAvgDeltaUtilsModeQuantilesChart avgDScoreModeIncomeChartData, 
  		String mixedDeltaScoreIncomeChartFile, String mixedMsoDeltaScoreIncomeChartFile,  Id runid1, Id runid2){
		DgMixedDeltaUtilsModeGroupChart mixedDsIncomeChart = new DgMixedDeltaUtilsModeGroupChart();
		XYSeriesCollection modeChoiceDataset = deltaUtilsModeGroupChart.createDeltaScoreIncomeModeChoiceDataset(runid1, runid2);
		mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceDataset);
		XYSeriesCollection avgScoreDataset = avgDScoreModeIncomeChartData.getDataset();
		mixedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgScoreDataset);
//		DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
		DgChartWriter.writeChart(mixedDeltaScoreIncomeChartFile, mixedDsIncomeChart.createChart());
		
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
		DgChartWriter.writeChart(mixedMsoDeltaScoreIncomeChartFile, mixedSwichterOnlyDsIncomeChart.createChart());
  }

}
