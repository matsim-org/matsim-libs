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
package playground.agarwalamit.siouxFalls.sampleSizePricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.internalization.EmissionCostFactors;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author amit
 */
public class TestingPricing4SamplePopulation {

	private static final Logger LOG = Logger.getLogger(TestingPricing4SamplePopulation.class);
	private static final boolean CONSIDER_CO2_COSTS=true;
	private static final double EMISS_COST_FACTOR=1.0;

	private static final double MARGINAL_UTIL_MONEY=0.062;
	private static final double MARGINAL_UTIL_PERF_SEC=0.96/3600;
	private static final double MARGINAL_UTIL_TRAV_CAR_SEC=-0.0/3600;
	private static final double MARGINAL_UTIL_TRAVEL_TIME = MARGINAL_UTIL_TRAV_CAR_SEC+MARGINAL_UTIL_PERF_SEC;
	private static final double VTTS_CAR = MARGINAL_UTIL_TRAVEL_TIME/MARGINAL_UTIL_MONEY;

	public static void main(String[] args) {

		String outputFolder = "/Users/aagarwal/Desktop/ils4/agarwal/flowCapTest/";//args[0]; 

		double [] samplePopulation = {0.01, 0.02, 0.03, 0.04, 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};//, 

		String configFile = outputFolder+"/input/SiouxFalls_config.xml";//args[1];
		String emissionEfficiencyFactor ="1.0";
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(outputFolder+"/input/SiouxFalls_networkWithRoadType0.5Capacity.xml.gz");

		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setAverageColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		ecg.setEmissionRoadTypeMappingFile("../../siouxFalls/input/SiouxFalls_roadTypeMapping.txt");
		ecg.setEmissionVehicleFile("../../siouxFalls/input/SiouxFalls_emissionVehicles.xml");
		ecg.setUsingDetailedEmissionCalculation(false);
		config.addModule(ecg);

		SortedMap<Double, Double> flowCap2DelaysCosts = new TreeMap<Double, Double>();
		SortedMap<Double, Double > flowCap2EmissionsCosts = new TreeMap<Double, Double>();
		SortedMap<Double, Integer > flowCap2NoOfPersons = new TreeMap<Double, Integer>();
		SortedMap<Double, Integer > flowCap2NoOfCarPersons = new TreeMap<Double, Integer>();

		for(double d:samplePopulation){
			LOG.info("Running sample Population "+d+".");
			String outputDir = outputFolder+"/f/f"+d+"/";
			new File(outputDir).mkdir();
			String samplePlansFile = outputDir+"/plans"+d+".xml";
			//			SamplingPlans samplePlans = new SamplingPlans(d,samplePlansFile);
			//			samplePlans.run();
			config.plans().setInputFile(samplePlansFile);
			config.qsim().setFlowCapFactor(d);
			double strCapCoeff = Double.valueOf(3);
			config.qsim().setStorageCapFactor(strCapCoeff*d);
			config.controler().setOutputDirectory(outputDir);
			config.controler().setLastIteration(500);
			Controler controler = new Controler(config);

			//			EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
			//			emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
			//			emissionModule.createLookupTables();
			//			emissionModule.createEmissionHandler();

			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.getConfig().controler().setCreateGraphs(true);
			controler.getConfig().controler().setDumpDataAtEnd(false);

//			controler.addControlerListener(new EmissionControlerListner());
			Scenario scenario = ScenarioUtils.loadScenario(config);
			//			controler.run();
			double delaysCosts = getDelaysFromEventsDefaultHandler(outputDir,(MutableScenario) scenario );
			//			double[] delaysCosts = getDelaysFromEvents(outputDir,(ScenarioImpl) controler.getScenario());
			double emissionsCosts = getTotalEmissionsCostsFromEmissionsEvents(outputDir, (MutableScenario) scenario);

			flowCap2NoOfPersons.put(d, scenario.getPopulation().getPersons().size());
			int carCounter =0;
			for(Person p:scenario.getPopulation().getPersons().values()){
				List<PlanElement> pes = new ArrayList<PlanElement>(p.getSelectedPlan().getPlanElements());
				for(PlanElement  pe :pes){
					if(pe instanceof Leg){
						String mode = ((Leg) pe).getMode();
						if(mode.equals(TransportMode.car)) {
							carCounter++;
							break;
						}
					}
				}
			}
			flowCap2NoOfCarPersons.put(d, carCounter);
			flowCap2DelaysCosts.put(d, delaysCosts);
			flowCap2EmissionsCosts.put(d,emissionsCosts);
			LOG.info(d+"\t"+flowCap2NoOfPersons.get(d)+"\t"+flowCap2NoOfCarPersons.get(d)+"\t"+flowCap2EmissionsCosts.get(d)+"\t"+flowCap2DelaysCosts.get(d)+"\n");
			LOG.info("Run for sample population "+d+" is finished. :-) :-) :-)");
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/f/flowCapFactorVsEmissionsAndTotalDelaysCosts.txt");
		try {
			writer.write("flowCapacityFactor \t noOfPersons \t noOfCarPersons \t emissionsCosts \t flowCapDelays  \n");
			for(double d:samplePopulation){
				writer.write(d+"\t"+flowCap2NoOfPersons.get(d)+"\t"+flowCap2NoOfCarPersons.get(d)+"\t"+flowCap2EmissionsCosts.get(d)+"\t"+flowCap2DelaysCosts.get(d)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason : -"+e);
		}


	}
	private static double getTotalEmissionsCostsFromEmissionsEvents(String outputDir, Scenario scenario){
		EmissionsAnalyzer analyzer	= new EmissionsAnalyzer(outputDir+"ITERS/it.500/500.emission.events.xml.gz");
		analyzer.init((MutableScenario) scenario);
		analyzer.preProcessData();
		analyzer.postProcessData();
		SortedMap<String, Double> totalEmissions = analyzer.getTotalEmissions();
		double totalEmissionCost =0;

		for(EmissionCostFactors ecf:EmissionCostFactors.values()) {
			String str = ecf.toString();			
			if(str.equals("CO2_TOTAL") && !CONSIDER_CO2_COSTS){
				// do not include CO2_TOTAL costs.
			} else {
				double emissionsCosts = EmissionCostFactors.valueOf(str).getCostFactor() * totalEmissions.get(str);
				totalEmissionCost += emissionsCosts;
			}
		}
		return EMISS_COST_FACTOR*totalEmissionCost;
	}
	//	private static double[] getDelaysFromEvents(String outputDir, ScenarioImpl sc){
	//		EventsManager eventManager = EventsUtils.createEventsManager();
	//		MarginalCongestionHandlerImplV3AA congestionHandlerImplV3= new MarginalCongestionHandlerImplV3AA(eventManager, sc);
	//
	//		eventManager.addHandler(congestionHandlerImplV3);
	//
	//		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
	//		String inputEventsFile = outputDir+"/ITERS/it.500/500.events.xml.gz";
	//		eventsReader.readFile(inputEventsFile);
	//		double flowAndStorageDelays [] = {congestionHandlerImplV3.getTotalInternalizedDelay()*vtts_car,congestionHandlerImplV3.getDelaysFromStorageCapacity()*vtts_car};
	//		return flowAndStorageDelays;
	//	}

	private static double getDelaysFromEventsDefaultHandler(String outputDir, MutableScenario sc){
		EventsManager eventManager = EventsUtils.createEventsManager();
		CongestionHandlerImplV3 congestionHandlerImplV3= new CongestionHandlerImplV3(eventManager, sc);

		eventManager.addHandler(congestionHandlerImplV3);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
		String inputEventsFile = outputDir+"/ITERS/it.500/500.events.xml.gz";
		eventsReader.readFile(inputEventsFile);
		double flowAndStorageDelays = congestionHandlerImplV3.getTotalInternalizedDelay()*VTTS_CAR; 
		return flowAndStorageDelays;
	}
}
