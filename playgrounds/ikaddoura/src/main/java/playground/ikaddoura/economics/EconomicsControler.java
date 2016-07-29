/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.economics;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 * @author ikaddoura
 *
 */
public class EconomicsControler {
	
	private static final Logger log = Logger.getLogger(EconomicsControler.class);
	private static final String path = "../../../runs-svn/economics/";
	
	private final int minDemand = 1;
	private final int maxDemand = 100;
	private final int incrementDemand = 1;
	private final double timeBin = 3600;
	
	private final double minCost = 1000.;
	private final double maxCost = 2500.;
	private final double incrementCost = 50.;
	
	private Map<Integer, Double> demand2privateCost = new HashMap<Integer, Double>();
	private Map<Integer, Double> demand2externalCost = new HashMap<Integer, Double>();
	
	private Map<Double, Integer> cost2demand = new HashMap<Double, Integer>();
	
	public static void main(String[] args) throws IOException {
				
		EconomicsControler main = new EconomicsControler();
		
		main.generateCostAsFunctionOfDemand(); // cost as function of demand (fixed demand)

//		main.generateDemandAsFunctionOfCost(); // demand as function of cost (fixed cost)
//		main.standardRunNoPricing(); // standard MATSim run: demand as function of cost; cost as function of demand
//		main.standardRunFlatPricing(); // standard MATSim run: demand as function of cost; cost as function of demand
//		main.standardRunUserSpecifictPricing(); // standard MATSim run: demand as function of cost; cost as function of demand
	}
	
	private void standardRunUserSpecifictPricing() {
						
		String configFileStandardRunFlatPricing = path + "input/configStandardRun.xml";
		Config config = ConfigUtils.loadConfig(configFileStandardRunFlatPricing);
		
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, 9.);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);

		config.controler().setOutputDirectory(path + "output_StandardRunUserSpecificPricing/");
		config.plans().setInputFile(path + "input/population_" + maxDemand + ".xml");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());
		new PopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());

		Controler controler = new Controler(scenario);

		// analysis
		WelfareAnalysisControlerListener analysis = new WelfareAnalysisControlerListener(scenario);
		controler.addControlerListener(analysis);

		// congestion pricing
		TollHandler tollHandler = new TollHandler(controler.getScenario());
		final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});
		controler.addControlerListener(new MarginalCongestionPricingContolerListener( controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())  ));

		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		controler.getConfig().controler().setCreateGraphs(true);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
		
	}

	private void standardRunFlatPricing() {
		
		double flatToll = 448.5;
		
		String csvFile = path + "/economics_StandardRunFlatPricing.csv";
		File file = new File(csvFile);
				
		String configFileStandardRunFlatPricing = path + "input/configStandardRun.xml";
		Config config = ConfigUtils.loadConfig(configFileStandardRunFlatPricing);
		
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, 9.);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);

		config.controler().setOutputDirectory(path + "output_StandardRunFlatPricing_" + flatToll + "/");
		config.plans().setInputFile(path + "input/population_" + maxDemand + ".xml");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());
		new PopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());

		Controler controler = new Controler(scenario);
		
		WelfareAnalysisControlerListener analysis = new WelfareAnalysisControlerListener(scenario);
		controler.addControlerListener(analysis);

		FlatPricingControlerListener flatPricing = new FlatPricingControlerListener(scenario, flatToll);
		controler.addControlerListener(flatPricing);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(true);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
		
	}

	private void standardRunNoPricing() {
		String csvFile = path + "/economics_StandardRunNoPricing.csv";
		File file = new File(csvFile);
				
		String configFileStandardRunNoPricing = path + "input/configStandardRun.xml";
		Config config = ConfigUtils.loadConfig(configFileStandardRunNoPricing);
		
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, 9.);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);

		config.controler().setOutputDirectory(path + "output_StandardRunNoPricing/");
		config.plans().setInputFile(path + "input/population_" + maxDemand + ".xml");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());
		new PopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());

		Controler controler = new Controler(scenario);

		WelfareAnalysisControlerListener analysis = new WelfareAnalysisControlerListener(scenario);
		controler.addControlerListener(analysis);

		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setCreateGraphs(true);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
	}

	private void generateDemandAsFunctionOfCost() {
		
		String csvFile = path + "/economics_DemandAsFunctionOfCost.csv";
		File file = new File(csvFile);
				
		for (double cost = minCost; cost <= maxCost; cost = cost + incrementCost) {
			
			String configFileDemandFunction = path + "input/configDemandAsFunctionOfCost.xml";
			Config config = ConfigUtils.loadConfig(configFileDemandFunction);
			
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, 9.);
			config.plansCalcRoute().setBeelineDistanceFactor(1.0);
			double constantCar = -1. * cost;
			config.planCalcScore().getModes().get(TransportMode.car).setConstant(constantCar);
			config.controler().setOutputDirectory(path + "output_DemandAsFunctionOfCost_" + cost + "/");
			config.plans().setInputFile(path + "input/population_" + maxDemand + ".xml");
			
			MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
			
			new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());
			new PopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
			
			Controler controler = new Controler(scenario);

			DemandFunctionControlerListener demandFunctionControlerListener = new DemandFunctionControlerListener();

			controler.getConfig().controler().setOverwriteFileSetting(
					OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			controler.getConfig().controler().setCreateGraphs(false);
            controler.addControlerListener(demandFunctionControlerListener);
			controler.addOverridingModule(new OTFVisFileWriterModule());
			controler.run();
			
			// analysis
			int carUsers = demandFunctionControlerListener.getDemand();
			this.cost2demand.put(cost, carUsers);
			
			log.info("#####################################################################");
			log.info("Cost: " + cost);
			log.info("Demand: " + carUsers);
			log.info("#####################################################################");
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("Demand;Cost");
				bw.newLine();
				for (Double cc : this.cost2demand.keySet()){
					bw.write(this.cost2demand.get(cc) + ";" + cc);
					bw.newLine();
				}
				
				bw.close();
				log.info("Output written to " + csvFile);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void generateCostAsFunctionOfDemand() {
		
		String csvFile = path + "/economics_CostAsFunctionOfDemand.csv";
		File file = new File(csvFile);
		
		for (int demand = minDemand; demand <= maxDemand; demand = demand + incrementDemand) {

			String configFileCostFunctions = path + "input/configCostAsFunctionOfDemand.xml";
			Config config = ConfigUtils.loadConfig(configFileCostFunctions);
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, 9.);
			config.plansCalcRoute().setBeelineDistanceFactor(1.0);
			config.controler().setOutputDirectory(path + "output_CostAsFunctionOfDemand_" + demand + "/");
			
			Population population = PopulationUtils.createPopulation(config);
//			population = generatePopulation(population, demand);
			population = generatePopulationSameTime(population, demand);
			
			MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
			new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());
			scenario.setPopulation(population);
			
			if (demand == maxDemand) {
				PopulationWriter populationWriter = new PopulationWriter(population, scenario.getNetwork());
				populationWriter.write(path + "input/population_" + demand + ".xml");
			}
			
			Controler controler = new Controler(scenario);

			CostFunctionsControlerListener economicsControlerListener = new CostFunctionsControlerListener((MutableScenario) controler.getScenario());

			controler.getConfig().controler().setOverwriteFileSetting(
					OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			controler.getConfig().controler().setCreateGraphs(false);
            controler.addControlerListener(economicsControlerListener);
			controler.addOverridingModule(new OTFVisFileWriterModule());
			controler.run();
			
			// analysis
			UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(scenario.getConfig(), WelfareMeasure.SELECTED, true);		
			
			double totalPrivateCost = -1. * userBenefitsCalculator_selected.calculateUtility_utils(scenario.getPopulation());
			this.demand2privateCost.put(demand, totalPrivateCost);

			double totalExternalCost = -1 * (config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600) * economicsControlerListener.getCongestionHandler().getTotalDelay();
			this.demand2externalCost.put(demand, totalExternalCost);
			
			log.info("#####################################################################");
			log.info("Demand: " + demand);
			log.info("Total Delay: " + economicsControlerListener.getCongestionHandler().getTotalDelay());
			log.info("Total Travel Cost: " + totalPrivateCost);
			log.info("Total External Cost: " + totalExternalCost);
			log.info("#####################################################################");

			economicsControlerListener.getCongestionHandler().writeCongestionStats(path + "output_CostAsFunctionOfDemand_" + demand + "/congestionStats.csv" );
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("Demand;Total Travel Cost;Total Delay Cost");
				bw.newLine();
				for (Integer demandInMap : this.demand2privateCost.keySet()){
					bw.write(demandInMap + ";" + this.demand2privateCost.get(demandInMap) + ";" + this.demand2externalCost.get(demandInMap));
					bw.newLine();
				}
				
				bw.close();
				log.info("Output written to " + csvFile);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Population generatePopulation(Population population, int demand) {
			
		// distribute the demand equally in the time bin.
		double linkEnterGap = 0.;
		
		if (demand <= 0) {
			throw new RuntimeException("Demand must be larger than 0. Aborting...");
		}
		
		if (demand == 1) {
			// no gap to calculate
		} else {
			linkEnterGap = this.timeBin / (demand - 1);
		}
		
		double linkEnterTimeThisAgent = 0.;
		
		for (int personNr = 0; personNr < demand; personNr++) {
			
			Coord homeLocation = getHomeCoord();
			Coord workLocation = new Coord(15000., 0.);		
			
			Person person = population.getFactory().createPerson(Id.create("person_" + personNr, Person.class));
			Plan plan = population.getFactory().createPlan();

			Activity activity0 = population.getFactory().createActivityFromCoord("h", homeLocation);
			
			activity0.setEndTime(linkEnterTimeThisAgent);
			plan.addActivity(activity0);
						
			plan.addLeg(population.getFactory().createLeg(TransportMode.car));
			
			Activity activity1 = population.getFactory().createActivityFromCoord("w", workLocation);
			plan.addActivity(activity1);
			
			person.addPlan(plan);
			population.addPerson(person);
			
			linkEnterTimeThisAgent += linkEnterGap;
		}
		
		return population;
	}
	
	private Population generatePopulationSameTime(Population population, int demand) {
				
		final double linkEnterTimeThisAgent = 0.;
		
		for (int personNr = 0; personNr < demand; personNr++) {
			
			Coord homeLocation = getHomeCoord();
			Coord workLocation = new Coord(15000., 0.);		
			
			Person person = population.getFactory().createPerson(Id.create("person_" + personNr, Person.class));
			Plan plan = population.getFactory().createPlan();

			Activity activity0 = population.getFactory().createActivityFromCoord("h", homeLocation);
			
			activity0.setEndTime(linkEnterTimeThisAgent);
			plan.addActivity(activity0);
						
			plan.addLeg(population.getFactory().createLeg(TransportMode.car));
			
			Activity activity1 = population.getFactory().createActivityFromCoord("w", workLocation);
			plan.addActivity(activity1);
			
			person.addPlan(plan);
			population.addPerson(person);			
		}
		
		return population;
	}
	
	private Coord getHomeCoord() {
		double minXCoord = -4000.;
		double maxXCoord = 4000.;
		
		double space = maxXCoord - minXCoord;
		double randomXCoord = calculateRandomlyDistributedValue(maxXCoord - (space/2.0), (space/2.0));
		Coord zoneCoord = new Coord(randomXCoord, (double) 0);
		return zoneCoord;
	}
	
	private double calculateRandomlyDistributedValue(double i, double abweichung){
		
		Random random = MatsimRandom.getLocalInstance();
		double rnd1 = random.nextDouble();
		double rnd2 = random.nextDouble();
		double vorzeichen = 0;
		if (rnd1 <= 0.5){
			vorzeichen = -1.0;
		}
		else {
			vorzeichen = 1.0;
		}
		double endTimeInSec = (i + (rnd2 * abweichung * vorzeichen));
		return endTimeInSec;
	}
}
	
