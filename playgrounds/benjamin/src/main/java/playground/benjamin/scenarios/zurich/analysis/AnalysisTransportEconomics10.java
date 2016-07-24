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
package playground.benjamin.scenarios.zurich.analysis;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.benjamin.BkPaths;

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
		
		String outputPath1 = BkPaths.RUNSSVN + runid1String;
		String outputPath2 = BkPaths.RUNSSVN + runid2String;
		OutputDirectoryHierarchy io1 = new OutputDirectoryHierarchy(
				outputPath1,
				runid1String,
						false ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		OutputDirectoryHierarchy io2 = new OutputDirectoryHierarchy(
				outputPath2,
				runid2String,
						false ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		

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
		
//		DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader();
//		DgAnalysisPopulation ana = new DgAnalysisPopulation();	
//		pc.readAnalysisPopulation(ana, runid1, netfile, plans1file);
//		pc.readAnalysisPopulation(ana, runid2, netfile, plans2file);
//		
//		DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
//		hhr.readHousholds(housholdsfile);
//		ana.calculateIncomeData();
//		
		Config config = new Config();
//		config.addCoreModules();
//		MatsimConfigReader configReader = new MatsimConfigReader(config);
//		configReader.readFile(outputConfig2Path);
//		
////***************************************************************************************************************************************************
////any filter can be applied here or for toll area see below:
//		pc.addFilter(new ExcludeZurichTransitFilter());
//		
////***************************************************************************************************************************************************
////creating the charts:		
//	//group chart
//		DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart = new DgDeltaUtilsModeGroupChart(ana, runid1, runid2);
//
//	//quantile charts
//		//modal split
//		DgModalSplitQuantilesChart modalSplitQuantilesChartRun1 = new DgModalSplitQuantilesChart(ana, runid1);
//		DgChartWriter.writeChart(modalSplitQuantilesChartFileRun1, modalSplitQuantilesChartRun1.createChart());
//		
//		DgModalSplitQuantilesChart modalSplitQuantilesChartRun2 = new DgModalSplitQuantilesChart(ana, runid2);
//		DgChartWriter.writeChart(modalSplitQuantilesChartFileRun2, modalSplitQuantilesChartRun2.createChart());
//
//		DgModalSplitDiffQuantilesChart modalSplitDiffQuantilesChartRun2 = new DgModalSplitDiffQuantilesChart(ana, runid1,  runid2);
//		DgChartWriter.writeChart(modalSplitDiffQuantilesChartFileRun2, modalSplitDiffQuantilesChartRun2.createChart());
//
//		//utility differences
//		DgAvgDeltaUtilsQuantilesChart avgDeltaUtilsQuantilesChart = new DgAvgDeltaUtilsQuantilesChart(ana, runid1, runid2);
//		DgChartWriter.writeChart(avgDeltaUtilsQuantilesChartFile, avgDeltaUtilsQuantilesChart.createChart());
//
//		DgAvgDeltaUtilsModeQuantilesChart avgDeltaUtilesModeQuantilesChart = new DgAvgDeltaUtilsModeQuantilesChart(ana, threshold,  runid1, runid2);
//		DgChartWriter.writeChart(avgDeltaUtilsModeQuantilesChartFile, avgDeltaUtilesModeQuantilesChart.createChart());
//		
//		//monetized utility differences
//		DgAvgDeltaMoneyQuantilesChart avgDeltaMoneyQuantilesChart = new DgAvgDeltaMoneyQuantilesChart(ana, runid1, runid2);
//		DgChartWriter.writeChart(avgDeltaMoneyQuantilesChartFile, avgDeltaMoneyQuantilesChart.createChart());

//		writeMixedDeltaUtilsModeGroupChart(deltaUtilsModeGroupChart, avgDeltaUtilesModeQuantilesChart, mixedDeltaUtilsModeGroupChartFile, mixedMsoDeltaUtilsModeGroupChartFile,  runid1, runid2);
		
		log.debug("Plots and tables finished!");
		
//***************************************************************************************************************************************************
//only if there is a toll scheme, money events file needed
//        if(ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).isUsingRoadpricing()){
		Logger.getLogger(AnalysisTransportEconomics10.class).fatal("the above functionality does no longer exist; pls tlk to me if you need it. kai, sep'14") ;
		System.exit(-1) ;
if ( true ) {
	eventsfile = io2.getIterationFilename(config.controler().getLastIteration(), "events") + ".txt.gz";
	
	EventsManager events = EventsUtils.createEventsManager();
	MoneyEventHandler tollCollectHandler = new MoneyEventHandler();
	events.addHandler(tollCollectHandler);
	
	MatsimEventsReader eventsReader = new MatsimEventsReader(events);
	eventsReader.readFile(eventsfile);
		System.out.println("Events file read!");
			
	Map<Id<Person>, Double> id2Toll = tollCollectHandler.getPersonId2TollMap();
//	for (DgPersonData data : ana.getPersonData().values()){
//		if (id2Toll.containsKey(data.getDriverId())){
//			data.setToll(id2Toll.get(data.getDriverId()));
//		}
//		else {
//			data.setToll(0.0);
//		}
//	}
	
	//money difference chart (due to toll)
//	BkAvgTollPaidQuantilesChart avgTollPaidQuantilesChart = new BkAvgTollPaidQuantilesChart (ana, runid2);
//	DgChartWriter.writeChart(avgTollPaidInQuantilesChartFile, avgTollPaidQuantilesChart.createChart());
	
	
	
//***************************************************************************************************************************************************
//another filter for a certain area
//	String tollLinksFilePath = BkPaths.STUDIESBK + "/transportEconomics/zh_forRun891_distanceMorningToll_0630-0900_cityWOhighways_1120rp_per_km.xml";
//	RoadPricingScheme tollLinks = loadTollLinksFile(tollLinksFilePath);
//pc.addFilter(new OnlyInnerZurichFilter(tollLinks));
//***************************************************************************************************************************************************	
	}		
}
	
  private static RoadPricingScheme loadTollLinksFile(String tollLinksFilePath) {
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
	  RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(scheme);
		reader.readFile(tollLinksFilePath);
		return scheme;
	}

//public static void writeMixedDeltaUtilsModeGroupChart(DgDeltaUtilsModeGroupChart deltaUtilsModeGroupChart, DgAvgDeltaUtilsModeQuantilesChart avgDScoreModeIncomeChartData, String mixedDeltaScoreIncomeChartFile, String mixedMsoDeltaScoreIncomeChartFile,  Id runid1, Id runid2){
//		DgMixedDeltaUtilsModeGroupChart mixedDsIncomeChart = new DgMixedDeltaUtilsModeGroupChart();
//		XYSeriesCollection modeChoiceDataset = deltaUtilsModeGroupChart.createDeltaScoreIncomeModeChoiceDataset(runid1, runid2);
//		mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceDataset);
//		XYSeriesCollection avgScoreDataset = avgDScoreModeIncomeChartData.getDataset();
//		mixedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgScoreDataset);
//////		DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
//		DgChartWriter.writeChart(mixedDeltaScoreIncomeChartFile, mixedDsIncomeChart.createChart());
//		
//		XYSeriesCollection ds2 = new XYSeriesCollection();
//		ds2.addSeries(modeChoiceDataset.getSeries(2));
//		ds2.addSeries(modeChoiceDataset.getSeries(3));
//		XYSeriesCollection ds3 = new XYSeriesCollection();
//		ds3.addSeries(avgScoreDataset.getSeries(2));
//		ds3.addSeries(avgScoreDataset.getSeries(3));
//		DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart mixedSwichterOnlyDsIncomeChart = new DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart();
//		mixedSwichterOnlyDsIncomeChart.addIncomeModeChoiceDataSet(ds2);
//		mixedSwichterOnlyDsIncomeChart.addAvgDeltaScoreIncomeDs(ds3);
//////		DgChartFrame frame = new DgChartFrame("test", mixedSwichterOnlyDsIncomeChart.createChart());
//		DgChartWriter.writeChart(mixedMsoDeltaScoreIncomeChartFile, mixedSwichterOnlyDsIncomeChart.createChart());
//  }

}
