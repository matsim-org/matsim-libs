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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;


/**
 * @author ikaddoura
 *
 */
public class EconomicsControler {
	
	private static final Logger log = Logger.getLogger(EconomicsControler.class);
	private static final String path = "../../shared-svn/studies/ihab/economics/";
	
	private final int minDemand = 0;
	private final int maxDemand = 1000;
	private final int incrementDemand = 10;
	
	private final double minCost = 700.;
	private final double maxCost = 2000.;
	private final double incrementCost = 10.;
	
	private Map<Integer, Double> demand2privateCost = new HashMap<Integer, Double>();
	private Map<Integer, Double> demand2externalCost = new HashMap<Integer, Double>();
	private Map<Integer, Double> demand2socialCost = new HashMap<Integer, Double>();
	
	private Map<Double, Integer> cost2demand = new HashMap<Double, Integer>();
	
	public static void main(String[] args) throws IOException {
				
		EconomicsControler main = new EconomicsControler();
		main.generateCostAsFunctionOfDemand(); // cost as function of demand
		main.generateDemandAsFunctionOfCost(); // demand as function of cost
	}
	
	private void generateDemandAsFunctionOfCost() {
		
		String csvFile = path + "/economics_DemandAsFunctionOfCost.csv";
		File file = new File(csvFile);
				
		for (double cost = minCost; cost <= maxCost; cost = cost + incrementCost) {
			
			String configFileDemandFunction = path + "input/configDemandAsFunctionOfCost.xml";
			Config config = ConfigUtils.loadConfig(configFileDemandFunction);
			
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, 9.);
			config.plansCalcRoute().setBeelineDistanceFactor(1.0);
			config.planCalcScore().setConstantCar(-1. * cost);
			config.controler().setOutputDirectory(path + "output_DemandAsFunctionOfCost_" + cost + "/");
			
			Population population = PopulationUtils.createPopulation(config);
			population = generatePopulation(population, maxDemand);

			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
			
			new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
			scenario.setPopulation(population);
			
			Controler controler = new Controler(scenario);

			DemandFunctionControlerListener demandFunctionControlerListener = new DemandFunctionControlerListener();

			controler.setOverwriteFiles(true);
			controler.setCreateGraphs(false);
			controler.addControlerListener(demandFunctionControlerListener);
			controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
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
			population = generatePopulation(population, demand);
			
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
			
			new MatsimNetworkReader(scenario).readFile(scenario.getConfig().network().getInputFile());
			scenario.setPopulation(population);
			
			Controler controler = new Controler(scenario);

			CostFunctionsControlerListener economicsControlerListener = new CostFunctionsControlerListener((ScenarioImpl) controler.getScenario());

			controler.setOverwriteFiles(true);
			controler.addControlerListener(economicsControlerListener);
			controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
			controler.run();
			
			// analysis
			UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(scenario.getConfig(), WelfareMeasure.SELECTED, true);		
			
			double totalPrivateCost = -1. * userBenefitsCalculator_selected.calculateUtility_utils(scenario.getPopulation());
			this.demand2privateCost.put(demand, totalPrivateCost);

			double totalExternalCost = -1 * (config.planCalcScore().getTraveling_utils_hr() / 3600) * economicsControlerListener.getCongestionHandler().getTotalDelay();
			this.demand2externalCost.put(demand, totalExternalCost);
			
			double socialCost = totalPrivateCost + totalExternalCost;
			this.demand2socialCost.put(demand, socialCost);
			
			log.info("#####################################################################");
			log.info("Demand: " + demand);
			log.info("Total Private Cost: " + totalPrivateCost);
			log.info("Total External Cost: " + totalExternalCost);
			log.info("Total Social Cost: " + socialCost);
			log.info("#####################################################################");

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("Demand;Private Cost;Social Cost");
				bw.newLine();
				for (Integer demandInMap : this.demand2privateCost.keySet()){
					bw.write(demandInMap + ";" + this.demand2privateCost.get(demandInMap) + ";" + this.demand2socialCost.get(demandInMap));
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
		
		for (int personNr = 0; personNr < demand; personNr++) {
			
			Coord homeLocation = getHomeCoord();	
			Coord workLocation = new CoordImpl(15000., 0.);	
			
			Person person = population.getFactory().createPerson(new IdImpl("person_" + personNr));
			Plan plan = population.getFactory().createPlan();

			Activity activity0 = population.getFactory().createActivityFromCoord("h", homeLocation);
			activity0.setEndTime(0.);
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
		Coord zoneCoord = new CoordImpl(randomXCoord, 0);
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
	
