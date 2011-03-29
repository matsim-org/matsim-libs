/* *********************************************************************** *
 * project: org.matsim.*
 * LegModeDistanceDistribution.java
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
package playground.benjamin.szenarios.munich;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author benjamin
 *
 */
public class LegModeDistanceDistribution {

	private static final Logger logger = Logger.getLogger(LegModeDistanceDistribution.class);

	// INPUT
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run14/";
	private static String netFile = runDirectory + "output_network.xml.gz";
//	private static String plansFile = runDirectory + "output_plans.xml.gz";
	private static String plansFile = runDirectory + "ITERS/it.0/0.plans.xml.gz";

	private final Scenario scenario;
	private List<Integer> distanceClasses;
	private SortedSet<String> usedModes;

	public LegModeDistanceDistribution(){
		Config config = ConfigUtils.createConfig();
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();
	}

	public static void main(String[] args) {
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.run(args);
	}

	private void run(String[] args) {
		loadSceanrio();
		setDistanceClasses();
		getUsedModes();
		Population population = scenario.getPopulation();
		Population miDPop = getMiDPopulation(population);
		Map<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(miDPop);

		System.out.println(mode2DistanceClassNoOfLegs);
		writeInformation(mode2DistanceClassNoOfLegs);
	}

	private void writeInformation(Map<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs) {

		for(String mode : this.usedModes){
			System.out.print("\t" + mode);
		}
		System.out.print("\n");
		String outLine = null;
		for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
			String bikeLegs = mode2DistanceClassNoOfLegs.get("bike").get(this.distanceClasses.get(i + 1)).toString();
			String carLegs = mode2DistanceClassNoOfLegs.get("car").get(this.distanceClasses.get(i + 1)).toString();
			String ptLegs = mode2DistanceClassNoOfLegs.get("pt").get(this.distanceClasses.get(i + 1)).toString();
			String rideLegs = mode2DistanceClassNoOfLegs.get("ride").get(this.distanceClasses.get(i + 1)).toString();
			String undefinedLegs = mode2DistanceClassNoOfLegs.get("undefined").get(this.distanceClasses.get(i + 1)).toString();
			String walkLegs = mode2DistanceClassNoOfLegs.get("walk").get(this.distanceClasses.get(i + 1)).toString();

			outLine = this.distanceClasses.get(i + 1).toString() + "\t" + 
			bikeLegs + "\t" + 
			carLegs + "\t" + 
			ptLegs + "\t" + 
			rideLegs + "\t" + 
			undefinedLegs + "\t" +
			walkLegs;
			System.out.println(outLine);
		}
	}

	private Map<String, Map<Integer, Integer>> calculateMode2DistanceClassNoOfLegs(Population population) {
		Map<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = new HashMap<String, Map<Integer, Integer>>();

		for(String mode : this.usedModes){
			SortedMap<Integer, Integer> distanceClass2NoOfLegs = new TreeMap<Integer, Integer>();
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				Integer noOfLegs = 0;
				for(Person person : population.getPersons().values()){
					PlanImpl plan = (PlanImpl) person.getSelectedPlan();
					List<PlanElement> planElements = plan.getPlanElements();
					for(PlanElement pe : planElements){
						if(pe instanceof Leg){
							Leg leg = (Leg) pe;
							String legMode = leg.getMode();
							Coord from = plan.getPreviousActivity(leg).getCoord();
							Coord to = plan.getNextActivity(leg).getCoord();
							Double legDist = CoordUtils.calcDistance(from, to);

							if(legMode.equals(mode)){
								if(legDist > this.distanceClasses.get(i) && legDist <= this.distanceClasses.get(i + 1)){
									noOfLegs++;
								}
							}
						}
					}
				}
				distanceClass2NoOfLegs.put(this.distanceClasses.get(i + 1), noOfLegs);
			}
			mode2DistanceClassNoOfLegs.put(mode, distanceClass2NoOfLegs);
		}
		return mode2DistanceClassNoOfLegs;
	}

	private boolean isPersonFromMID(Person person) {
		boolean isFromMID = false;
		if(!person.getId().toString().contains("gv_") && !person.getId().toString().contains("pv_")){
			isFromMID = true;
		}
		return isFromMID;
	}

	private Population getMiDPopulation(Population population) {
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonFromMID(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	private void getUsedModes() {
		Population population = scenario.getPopulation();
		for(Person person : population.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					String legMode = leg.getMode();
					if(!this.usedModes.contains(legMode)){
						this.usedModes.add(legMode);
					}
				}
			}
		}
		logger.info("The following transport modes are found in the population: " + this.usedModes);
	}

	private void setDistanceClasses() {
		this.distanceClasses.add(0);
		for(int noOfClasses = 0; noOfClasses < 14; noOfClasses++){
			int distanceClass = 100 * (int) Math.pow(2, noOfClasses);
			this.distanceClasses.add(distanceClass);
		}
		logger.info("The following distance classes are defined: " + this.distanceClasses);
	}

	private void loadSceanrio() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;

	}
}
