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
package playground.benjamin.old;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;

import playground.benjamin.BkPaths;
import playground.dgrether.analysis.charts.DgMixedDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.analysis.io.DgAnalysisPopulationReader;
import playground.dgrether.analysis.io.DgHouseholdsAnalysisReader;
import playground.dgrether.analysis.population.DgAnalysisPopulation;


public class Iatbr09Analysis {

	private static final Logger log = Logger.getLogger(Iatbr09Analysis.class);
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
			String runNumber1 = "749";
			String runNumber2 = "867";
//			String runNumber1 = "860";
//			String runNumber2 = "864";
		
			String runid1 = "run" + runNumber1;
			String runid2 = "run" + runNumber2;
			
			String runiddot1 = runid1 + ".";
			String runiddot2 = runid2 + ".";
			
//			String netfile = BkPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_network.xml.gz";
			String netfile = BkPaths.RUNSSVN + runid1 + "/" + runNumber1 + ".output_network.xml.gz";
//			String plans1file = BkPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_plans.xml.gz";
			String plans1file = BkPaths.RUNSSVN + runid1 + "/" + runNumber1 + ".output_plans.xml.gz";
//			String plans2file = BkPaths.RUNBASE + runid2 + "/" + runiddot2 + "output_plans.xml.gz";
			String plans2file = BkPaths.RUNSSVN + runid2 + "/" + runNumber2 + ".output_plans.xml.gz";
			String housholdsfile = BkPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";
//			String housholdsfile = BkPaths.RUNBASE+ "bkick/oneRouteTwoModeIncomeTest/households.xml";

			String deltaScoreChartFile = BkPaths.RUNSSVN + runid2 + "/deltaScoreSingleColorIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			String deltaScoreColorChartFile = BkPaths.RUNSSVN + runid2 + "/deltaScoreColorIncomeChart"+runNumber1+"vs"+runNumber2+".png";
//			String deltaScoreModeSwitchOnlyChartFile = BkPaths.RUNBASE + runid2 + "/deltaScoreIncomeChart"+runNumber1+"vs"+runNumber2+"ModeSwitcherOnly.png";
			String modeChoiceIncomeChartFile1 = BkPaths.RUNSSVN + runid1 + "/"+runNumber1+"modeChoiceIncomeChart.png";
			String modeChoiceIncomeChartFile2 = BkPaths.RUNSSVN + runid2 + "/"+runNumber2+"modeChoiceIncomeChart.png";
			
			String avgDeltaScoreIncomeGroupChartFile = BkPaths.RUNSSVN + runid2 + "/avgDeltaScoreIncomeGroupChart"+runNumber1+"vs"+runNumber2+".png";
			String avgDeltaScoreIncomeGroupChartModeSwitchFile = BkPaths.RUNSSVN + runid2 + "/avgDeltaScoreIncomeGroupModeSwitchChart"+runNumber1+"vs"+runNumber2+".png";
			
			String mixedDeltaScoreIncomeChartFile = BkPaths.RUNSSVN + runid2 + "/mixedDeltaScoreIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			
			ScenarioImpl sc = new ScenarioImpl();

			
			DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader(sc);
			DgAnalysisPopulation ana = pc.doPopulationAnalysis(netfile, plans1file, plans2file);
			
			DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
			hhr.readHousholds(housholdsfile);
			
//			DgDeltaScoreIncomeChart incomeChart;
//			incomeChart = new DgDeltaScoreIncomeChart(ana);
//			incomeChart.writeFile(deltaScoreChartFile);
			
//			incomeChart.setWriteModeSwitcherOnly(true);
//			incomeChart.writeFile(deltaScoreModeSwitchOnlyChartFile);
//		
//			DgDeltaScoreIncomeModeChoiceChart modeChoiceIncomeChart;
//			modeChoiceIncomeChart = new DgDeltaScoreIncomeModeChoiceChart(ana);
//			DgChartWriter.writerChartToFile(deltaScoreColorChartFile, modeChoiceIncomeChart.createChart());
			
//			DgModeChoiceIncomeChart modechoiceIncomeChartRun1 = new DgModeChoiceIncomeChart(ana, DgAnalysisPopulation.RUNID1);
//			DgChartWriter.writerChartToFile(modeChoiceIncomeChartFile1, modechoiceIncomeChartRun1.createChart());
//			DgModeChoiceIncomeChart modechoiceIncomeChartRun2 = new DgModeChoiceIncomeChart(ana, DgAnalysisPopulation.RUNID2);
//			DgChartWriter.writerChartToFile(modeChoiceIncomeChartFile2, modechoiceIncomeChartRun2.createChart());
			
//			DgChartFrame frame = new DgChartFrame("test", modechoiceIncomeChartRun2.createChart());
//
//			new DgAvgDeltaScoreIncomeGroupChart(ana).writeFile(avgDeltaScoreIncomeGroupChartFile);
//			DgAvgDeltaScoreIncomeModeChoiceChart avgDScoreIncomeChartData = new DgAvgDeltaScoreIncomeModeChoiceChart(ana);
//			DgChartFrame frame = new DgChartFrame("test", avgDScoreIncomeChartData.createChart());
//			DgChartWriter.writerChartToFile(avgDeltaScoreIncomeGroupChartModeSwitchFile, avgDScoreIncomeChartData.createChart());
			
			DgMixedDeltaUtilsModeGroupChart mixedDsIncomeChart = new DgMixedDeltaUtilsModeGroupChart();
//			mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceIncomeChart.createDeltaScoreIncomeModeChoiceDataset());
//			mixsedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgDScoreIncomeChartData.getDataset());
//			DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
			DgChartWriter.writeChart(mixedDeltaScoreIncomeChartFile, mixedDsIncomeChart.createChart());
			
			log.debug("ya esta ;-)");
			
	}
	

}
