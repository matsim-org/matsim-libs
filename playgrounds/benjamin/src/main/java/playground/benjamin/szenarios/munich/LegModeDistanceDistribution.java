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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
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
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run20/";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String initialPlansFile = runDirectory + "ITERS/it.0/0.plans.xml.gz";
	private static String finalPlansFile = runDirectory + "output_plans.xml.gz";

	private final Scenario initialScenario;
	private final Scenario finalScenario;
	private List<Integer> distanceClasses;
	private SortedSet<String> usedModes;

	public LegModeDistanceDistribution(){
		Config config = ConfigUtils.createConfig();
		this.initialScenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.finalScenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();
	}

	public static void main(String[] args) {
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.run(args);
	}

	private void run(String[] args) {
		loadSceanrio(this.initialScenario, netFile, initialPlansFile);
		loadSceanrio(this.finalScenario, netFile, finalPlansFile);
		setDistanceClasses();
		getUsedModes();
		Population initialPop = this.initialScenario.getPopulation();
		Population finalPop = this.finalScenario.getPopulation();
		Population initialMiDPop = getMiDPopulation(initialPop);
		Population finalMiDPop = getMiDPopulation(finalPop);
		SortedMap<String, Map<Integer, Integer>> initialMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(initialMiDPop);
		SortedMap<String, Map<Integer, Integer>> finalMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(finalMiDPop);

		logger.info("The initial LegModeDistanceDistribution is :" + initialMode2DistanceClassNoOfLegs);
		logger.info("The final LegModeDistanceDistribution is :" + finalMode2DistanceClassNoOfLegs);
		writeInformation(initialMode2DistanceClassNoOfLegs, "legModeDistanceDistributionInitial");
		writeInformation(finalMode2DistanceClassNoOfLegs, "legModeDistanceDistributionFinal");
	}

	private void writeInformation(Map<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs, String fileName) {
		String outFile = runDirectory + fileName + ".txt";
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			for(String mode : this.usedModes){
				out.write("\t" + mode);
			}
			out.write("\n");
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				Integer middleOfDistanceClass = ((this.distanceClasses.get(i) + this.distanceClasses.get(i + 1)) / 2);
				out.write(middleOfDistanceClass + "\t");
				for(String mode : this.usedModes){
					String modeLegs = null;
					modeLegs = mode2DistanceClassNoOfLegs.get(mode).get(this.distanceClasses.get(i + 1)).toString() + "\t";
					out.write(modeLegs);
				}
				out.write("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		}catch (Exception e){
			logger.error("Error: " + e.getMessage());
		}
	}

	private SortedMap<String, Map<Integer, Integer>> calculateMode2DistanceClassNoOfLegs(Population population) {
		SortedMap<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = new TreeMap<String, Map<Integer, Integer>>();

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
		Population population = this.initialScenario.getPopulation();
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
		logger.info("The following transport modes are found in the initial population: " + this.usedModes);
	}

	private void setDistanceClasses() {
		this.distanceClasses.add(0);
		for(int noOfClasses = 0; noOfClasses < 9; noOfClasses++){
			int distanceClass = 100 * (int) Math.pow(2, noOfClasses);
			this.distanceClasses.add(distanceClass);
		}
		logger.info("The following distance classes are defined: " + this.distanceClasses);
	}

	private void loadSceanrio(Scenario scenario, String netFile, String plansFile) {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;

	}
}