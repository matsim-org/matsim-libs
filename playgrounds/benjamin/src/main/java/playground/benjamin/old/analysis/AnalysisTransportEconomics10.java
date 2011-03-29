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
package playground.benjamin.old.analysis;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.benjamin.BkPaths;
import playground.benjamin.old.charts.BkAvgTollPaidQuantilesChart;
import playground.dgrether.analysis.charts.DgAvgDeltaMoneyQuantilesChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsModeQuantilesChart;
import playground.dgrether.analysis.charts.DgAvgDeltaUtilsQuantilesChart;
import playground.dgrether.analysis.charts.DgDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgMixedDeltaUtilsModeGroupChart;
import playground.dgrether.analysis.charts.DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgModalSplitDiffQuantilesChart;
import playground.dgrether.analysis.charts.DgModalSplitQuantilesChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.analysis.io.DgAnalysisPopulationReader;
import playground.dgrether.analysis.io.DgHouseholdsAnalysisReader;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonData;
import playground.dgrether.analysis.population.ExcludeZurichTransitFilter;

public class AnalysisTransportEconomics10 {

	private static final Logger log = Logger.getLogger(AnalysisTransportEconomics10.class);
	
	public static void main(String[] args) {
		String netfile, plans1file, plans2file, housholdsfile, outputConfig2Path, eventsfile;
		int threshold;
		
//***************************************************************************************************************************************************	
//changes should be done here and under filters (see below):
		String runNumber1 = "891";
		String runNumber2 = "896";
//***************************************************************************************************************************************************		
		
		String runid1String = "run" + runNumber1;
		String runid2String = "run" + runNumber2;
		
		Id runid1 = new IdImpl(runNumber1);
		Id runid2 = new IdImpl(runNumber2);
		
		String outputPath1 = BkPaths.RUNSSVN + runid1String;
		String outputPath2 = BkPaths.RUNSSVN + runid2String;
		ControlerIO io1 = new ControlerIO(outputPath1, runid1);
		ControlerIO io2 = new ControlerIO(outputPath2, runid2);
		

		netfile = io1.getOutputFilename("output_network") + ".xml.gz";
		plans1file = io1.getOutputFilename("output_plans") + ".xml.gz";
		plans2file = io2.getOutputFilename("output_plans") + ".xml.gz";		
		housholdsfile = io2.getOutputFilename("output_households") + ".xml.gz";
		outputConfig2Path = io2.getOutputFilename("output_config") + ".xml.gz";	
		threshold = 100;

		String mixedDeltaUtilsModeGroupChartFile = BkPaths.RUNSSVN + runid2String + "/mixedDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;
		String mixedMsoDeltaUtilsModeGroupChartFile = BkPaths.RUNSSVN + runid2String + "/mixedMsoDeltaUtilsModeGroupChart"+runNumber1+"vs"+runNumber2;

		//quantile charts
		String modalSplitQuantilesChartFileRun1 = BkPaths.RUNSSVN + runid1String + "/"+runNumber1+"modalSplitQuantilesChart";
		String modalSplitQuantilesChartFileRun2 = BkPaths.RUNSSVN + runid2String + "/"+runNumber2+"modalSplitQuantilesChart";
		String modalSplitDiffQuantilesChartFileRun2 = BkPaths.RUNSSVN + runid2String + "/modalSplitQuantilesChart" + runNumber1 + "vs" +runNumber2;
		String avgDeltaUtilsQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaUtilsQuantilesChart" + runNumber1 + "vs" + runNumber2;
		String avgDeltaUtilsModeQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaUtilsModeQuantilesChart"+runNumber1+"vs"+runNumber2;
		String avgDeltaMoneyQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgDeltaMoneyQuantilesChart"+runNumber1+"vs"+runNumber2;
		String avgTollPaidInQuantilesChartFile = BkPaths.RUNSSVN + runid2String + "/avgTollPaidInQuantilesChart"+runNumber2;
//***************************************************************************************************************************************************
		
		DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader();
		DgAnalysisPopulation ana = new DgAnalysisPopulation();	
		pc.readAnalysisPopulation(ana, runid1, netfile, plans1file);
		pc.readAnalysisPopulation(ana, runid2, netfile, plans2file);
		
		DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
		hhr.readHousholds(housholdsfile);
		ana.calculateIncomeData();
		
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(outputConfig2Path);
		
//***************************************************************************************************************************************************
//any filter can be applied here or for toll area see below:
		pc.addFilter(new ExcludeZurichTransitFilter());
		
//***************************************************************************************************************************************************
//creating the charts:		
	//group chart
		DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart = new DgDeltaUtilsModeGroupChart(ana, runid1, runid2);

	//quantile charts
		//modal split
		DgModalSplitQuantilesChart modalSplitQuantilesChartRun1 = new DgModalSplitQuantilesChart(ana, runid1);
		DgChartWriter.writeChart(modalSplitQuantilesChartFileRun1, modalSplitQuantilesChartRun1.createChart());
		
		DgModalSplitQuantilesChart modalSplitQuantilesChartRun2 = new DgModalSplitQuantilesChart(ana, runid2);
		DgChartWriter.writeChart(modalSplitQuantilesChartFileRun2, modalSplitQuantilesChartRun2.createChart());

		DgModalSplitDiffQuantilesChart modalSplitDiffQuantilesChartRun2 = new DgModalSplitDiffQuantilesChart(ana, runid1,  runid2);
		DgChartWriter.writeChart(modalSplitDiffQuantilesChartFileRun2, modalSplitDiffQuantilesChartRun2.createChart());

		//utility differences
		DgAvgDeltaUtilsQuantilesChart avgDeltaUtilsQuantilesChart = new DgAvgDeltaUtilsQuantilesChart(ana, runid1, runid2);
		DgChartWriter.writeChart(avgDeltaUtilsQuantilesChartFile, avgDeltaUtilsQuantilesChart.createChart());

		DgAvgDeltaUtilsModeQuantilesChart avgDeltaUtilesModeQuantilesChart = new DgAvgDeltaUtilsModeQuantilesChart(ana, threshold,  runid1, runid2);
		DgChartWriter.writeChart(avgDeltaUtilsModeQuantilesChartFile, avgDeltaUtilesModeQuantilesChart.createChart());
		
		//monetized utility differences
		DgAvgDeltaMoneyQuantilesChart avgDeltaMoneyQuantilesChart = new DgAvgDeltaMoneyQuantilesChart(ana, runid1, runid2);
		DgChartWriter.writeChart(avgDeltaMoneyQuantilesChartFile, avgDeltaMoneyQuantilesChart.createChart());

		writeMixedDeltaUtilsModeGroupChart(deltaUtilsModeGroupChart, avgDeltaUtilesModeQuantilesChart, mixedDeltaUtilsModeGroupChartFile, mixedMsoDeltaUtilsModeGroupChartFile,  runid1, runid2);
		
		log.debug("Plots and tables finished!");
		
//***************************************************************************************************************************************************
//only if there is a toll scheme, money events file needed
if(config.scenario().isUseRoadpricing()){
	eventsfile = io2.getIterationFilename(config.controler().getLastIteration(), "events") + ".txt.gz";
	
	EventsManager events = new EventsManagerImpl();
	MoneyEventHandler tollCollectHandler = new MoneyEventHandler();
	events.addHandler(tollCollectHandler);
	
	MatsimEventsReader eventsReader = new MatsimEventsReader(events);
	eventsReader.readFile(eventsfile);
		System.out.println("Events file read!");
			
	Map<Id, Double> id2Toll = tollCollectHandler.getPersonId2TollMap();
	for (DgPersonData data : ana.getPersonData().values()){
		if (id2Toll.containsKey(data.getPersonId())){
			data.setToll(id2Toll.get(data.getPersonId()));
		}
		else {
			data.setToll(0.0);
		}
	}
	
	//money difference chart (due to toll)
	BkAvgTollPaidQuantilesChart avgTollPaidQuantilesChart = new BkAvgTollPaidQuantilesChart (ana, runid2);
	DgChartWriter.writeChart(avgTollPaidInQuantilesChartFile, avgTollPaidQuantilesChart.createChart());
	
//***************************************************************************************************************************************************
//another filter for a certain area
//	String tollLinksFilePath = BkPaths.STUDIESBK + "/transportEconomics/zh_forRun891_distanceMorningToll_0630-0900_cityWOhighways_1120rp_per_km.xml";
//	RoadPricingScheme tollLinks = loadTollLinksFile(tollLinksFilePath);
//pc.addFilter(new OnlyInnerZurichFilter(tollLinks));
//***************************************************************************************************************************************************	
	}		
}
	
  private static RoadPricingScheme loadTollLinksFile(String tollLinksFilePath) {
		RoadPricingScheme scheme = new RoadPricingScheme();
	  	RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(scheme);
		try {
			reader.parse(tollLinksFilePath);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return scheme;
	}

public static void writeMixedDeltaUtilsModeGroupChart(DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart, DgAvgDeltaUtilsModeQuantilesChart avgDScoreModeIncomeChartData, String mixedDeltaScoreIncomeChartFile, String mixedMsoDeltaScoreIncomeChartFile,  Id runid1, Id runid2){
		DgMixedDeltaUtilsModeGroupChart mixedDsIncomeChart = new DgMixedDeltaUtilsModeGroupChart();
		XYSeriesCollection modeChoiceDataset = deltaUtilsModeGroupChart.createDeltaScoreIncomeModeChoiceDataset(runid1, runid2);
		mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceDataset);
		XYSeriesCollection avgScoreDataset = avgDScoreModeIncomeChartData.getDataset();
		mixedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgScoreDataset);
////		DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
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
////		DgChartFrame frame = new DgChartFrame("test", mixedSwichterOnlyDsIncomeChart.createChart());
		DgChartWriter.writeChart(mixedMsoDeltaScoreIncomeChartFile, mixedSwichterOnlyDsIncomeChart.createChart());
  }

}
