/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls;

import java.io.BufferedWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;
import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class TestingPricing4SamplePopulation {
	
	private static final Logger log = Logger.getLogger(TestingPricing4SamplePopulation.class);
	private static final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private static final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private static  final double EURO_PER_GRAMM_SO2 = 11000. / (1000. * 1000.);
	private static  final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	private static final double EURO_PER_GRAMM_CO2 = 70. / (1000. * 1000.);
	private static final boolean considerCO2Costs=true;
	private static final double emissionCostFacotr=1.0;


	private static final double marginal_Utl_money=0.062;
	private static final double marginal_Utl_performing_sec=0.96/3600;
	private static final double marginal_Utl_traveling_car_sec=-0.0/3600;
	private static final double marginalUtlOfTravelTime = marginal_Utl_traveling_car_sec+marginal_Utl_performing_sec;
	private static final double vtts_car = marginalUtlOfTravelTime/marginal_Utl_money;
	
	public static void main(String[] args) {
		
		String outputFolder = args[0];
		double [] samplePopulation = {0.01, 0.02, 0.03, 0.04, 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		
		String configFile = args[1];
		String emissionEfficiencyFactor ="1.0";
		Config config = ConfigUtils.loadConfig(configFile);
		
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
		ecg.setAverageColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		ecg.setEmissionRoadTypeMappingFile("../../siouxFalls/input/SiouxFalls_roadTypeMapping.txt");
		ecg.setEmissionVehicleFile("../../siouxFalls/input/SiouxFalls_emissionVehicles.xml");
		ecg.setUsingDetailedEmissionCalculation(false);
		config.addModule(ecg);
		
		SortedMap<Double, Double> flowCap2DelaysCosts = new TreeMap<Double, Double>();
		SortedMap<Double, Double > flowCap2EmissionsCosts = new TreeMap<Double, Double>();
		
		for(double d:samplePopulation){
			log.info("Running sample Population "+d+".");
			String outputDir = outputFolder+"/f="+d+"/";
			String samplePlansFile = outputDir+"/plans"+d+".xml";
//			SamplingPlans samplePlans = new SamplingPlans(d,samplePlansFile);
//			samplePlans.run();
			config.plans().setInputFile(samplePlansFile);
			config.qsim().setFlowCapFactor(d);
			config.qsim().setStorageCapFactor(3*d);
			config.controler().setOutputDirectory(outputDir);
			Controler controler = new Controler(config);
			
			EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
			emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
			emissionModule.createLookupTables();
			emissionModule.createEmissionHandler();
			
			controler.setOverwriteFiles(true);
			controler.setCreateGraphs(true);				
			controler.setDumpDataAtEnd(true);
			controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));

			controler.addControlerListener(new EmissionControlerListener());
			controler.run();
			double delaysCosts = getDelaysFromEvents(outputDir,(ScenarioImpl) controler.getScenario());
			double emissionsCosts = getTotalEmissionsCostsFromEmissionsEvents(outputDir, (ScenarioImpl) controler.getScenario());
			
			flowCap2DelaysCosts.put(d, delaysCosts);
			flowCap2EmissionsCosts.put(d,emissionsCosts);
			log.info("Run for sample population "+d+" is finished. :-) :-) :-)");
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/flowCapFactorVsEmissionsAndDelaysCosts.txt");
		try {
			writer.write("flowCapacityFactor \t emissionsCosts \t delays \n");
			for(double d:samplePopulation){
				writer.write(d+"\t"+flowCap2EmissionsCosts.get(d)+"\t"+flowCap2DelaysCosts.get(d)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason : -"+e);
		}


	}
	private static double getTotalEmissionsCostsFromEmissionsEvents(String outputDir, Scenario scenario){
		EmissionsAnalyzer analyzer	= new EmissionsAnalyzer(outputDir+"ITERS/it.100/100.emission.events.xml.gz");
		analyzer.init((ScenarioImpl) scenario);
		analyzer.preProcessData();
		analyzer.postProcessData();
		SortedMap<String, Double> totalEmissions = analyzer.getTotalEmissions();
		double totalEmissionCost =0;

		for(String str:totalEmissions.keySet()){
			if(str.equals("NOX")) {
				double noxCosts = totalEmissions.get(str) * EURO_PER_GRAMM_NOX;
				totalEmissionCost += noxCosts;
			} else if(str.equals("NMHC")) {
				double nmhcCosts =totalEmissions.get(str) * EURO_PER_GRAMM_NMVOC;
				totalEmissionCost += nmhcCosts;
			} else if(str.equals("SO2")) {
				double so2Costs = totalEmissions.get(str) * EURO_PER_GRAMM_SO2;
				totalEmissionCost += so2Costs;
			} else if(str.equals("PM")) {
				double pmCosts = totalEmissions.get(str) * EURO_PER_GRAMM_PM2_5_EXHAUST;
				totalEmissionCost += pmCosts;
			} else if(str.equals("CO2_TOTAL")){
				if(considerCO2Costs) {
					double co2Costs = totalEmissions.get(str) * EURO_PER_GRAMM_CO2;
					totalEmissionCost += co2Costs;
				} else ; //do nothing
			}
			else ; //do nothing
		}
		return emissionCostFacotr*totalEmissionCost;
	}
	private static double getDelaysFromEvents(String outputDir, ScenarioImpl sc){
		EventsManager eventManager = EventsUtils.createEventsManager();
		MarginalCongestionHandlerImplV3 congestionHandlerImplV3= new MarginalCongestionHandlerImplV3(eventManager, sc);

		eventManager.addHandler(congestionHandlerImplV3);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
		String inputEventsFile = outputDir+"/ITERS/it.100/100.events.xml.gz";
		eventsReader.readFile(inputEventsFile);

		return congestionHandlerImplV3.getTotalDelay()*vtts_car;
	}

}
