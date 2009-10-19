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
import org.matsim.api.core.v01.ScenarioImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgAvgDeltaScoreIncomeGroupModeSwitchChart;
import playground.dgrether.analysis.population.DgDeltaScoreIncomeChart;
import playground.dgrether.analysis.population.DgDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.population.DgHouseholdsAnalysisReader;
import playground.dgrether.analysis.population.DgPopulationAnalysisReader;
import playground.dgrether.utils.charts.DgChartWriter;


public class Trb09Analysis {

	private static final Logger log = Logger.getLogger(Trb09Analysis.class);
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//			String runNumber1 = "743";
//			String runNumber2 = "746";
			String runNumber1 = "860";
			String runNumber2 = "864";
		
			String runid1 = "run" + runNumber1;
			String runid2 = "run" + runNumber2;
			
			String runiddot1 = runid1 + ".";
			String runiddot2 = runid2 + ".";
			
			String netfile = DgPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_network.xml.gz";
			String plans1file = DgPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_plans.xml.gz";
			String plans2file = DgPaths.RUNBASE + runid2 + "/" + runiddot2 + "output_plans.xml.gz";
			String housholdsfile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/households.xml";

			String deltaScoreChartFile = DgPaths.RUNBASE + runid2 + "/deltaScoreSingleColorIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			String deltaScoreColorChartFile = DgPaths.RUNBASE + runid2 + "/deltaScoreColorIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			String deltaScoreModeSwitchOnlyChartFile = DgPaths.RUNBASE + runid2 + "/deltaScoreIncomeChart"+runNumber1+"vs"+runNumber2+"ModeSwitcherOnly.png";
			String modeChoiceIncomeChartFile1 = DgPaths.RUNBASE + runid1 + "/"+runNumber1+"modeChoiceIncomeChart.png";
			String modeChoiceIncomeChartFile2 = DgPaths.RUNBASE + runid2 + "/"+runNumber2+"modeChoiceIncomeChart.png";
			
			String avgDeltaScoreIncomeGroupChartFile = DgPaths.RUNBASE + runid2 + "/avgDeltaScoreIncomeGroupChart"+runNumber1+"vs"+runNumber2+".png";
			String avgDeltaScoreIncomeGroupChartModeSwitchFile = DgPaths.RUNBASE + runid2 + "/avgDeltaScoreIncomeGroupModeSwitchChart"+runNumber1+"vs"+runNumber2+".png";
			
			String mixedDeltaScoreIncomeChartFile = DgPaths.RUNBASE + runid2 + "/mixedDeltaScoreIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			
			ScenarioImpl sc = new ScenarioImpl();

			
			DgPopulationAnalysisReader pc = new DgPopulationAnalysisReader(sc);
			DgAnalysisPopulation ana = pc.doPopulationAnalysis(netfile, plans1file, plans2file);
			
			DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
			hhr.readHousholds(housholdsfile);
			
			DgDeltaScoreIncomeChart incomeChart;
			//			incomeChart = new DgDeltaScoreIncomeChart(ana);
//			incomeChart.writeFile(deltaScoreChartFile);
			
//			incomeChart.setWriteModeSwitcherOnly(true);
//			incomeChart.writeFile(deltaScoreModeSwitchOnlyChartFile);
//		
			DgDeltaScoreIncomeModeChoiceChart modeChoiceIncomeChart;
			modeChoiceIncomeChart = new DgDeltaScoreIncomeModeChoiceChart(ana);
//			DgChartWriter.writerChartToFile(deltaScoreColorChartFile, modeChoiceIncomeChart.createChart());
			
//			new DgModeChoiceIncomeChart(ana).writeFile(modeChoiceIncomeChartFile1, DgAnalysisPopulation.RUNID1);
//			new DgModeChoiceIncomeChart(ana).writeFile(modeChoiceIncomeChartFile2, DgAnalysisPopulation.RUNID2);
//
//			new DgAvgDeltaScoreIncomeGroupChart(ana).writeFile(avgDeltaScoreIncomeGroupChartFile);
			DgAvgDeltaScoreIncomeGroupModeSwitchChart avgDScoreIncomeChartData = new DgAvgDeltaScoreIncomeGroupModeSwitchChart(ana);
//			DgChartWriter.writerChartToFile(avgDeltaScoreIncomeGroupChartModeSwitchFile, avgDScoreIncomeChartData.createChart());
			
			DgMixedDeltaScoreIncomeChart mixedDsIncomeChart = new DgMixedDeltaScoreIncomeChart();
			mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceIncomeChart.createDeltaScoreIncomeModeChoiceDataset());
			mixedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgDScoreIncomeChartData.getDatasets());
//			DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
			DgChartWriter.writerChartToFile(mixedDeltaScoreIncomeChartFile, mixedDsIncomeChart.createChart());
			
			log.debug("ya esta ;-)");
			
	}
	

}
