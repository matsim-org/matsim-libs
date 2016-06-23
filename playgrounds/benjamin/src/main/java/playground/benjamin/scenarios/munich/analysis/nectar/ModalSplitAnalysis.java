/* *********************************************************************** *
 * project: org.matsim.*
 * ModalSplitAnalysis.java
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
package playground.benjamin.scenarios.munich.analysis.nectar;

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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.utils.BkNumberUtils;

/**
 * @author benjamin
 *
 */
public class ModalSplitAnalysis {
	private static final Logger logger = Logger.getLogger(ModalSplitAnalysis.class);

	private final static String runNumber = "981";
	private final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
	private final String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	private final String plansFile = runDirectory + runNumber + ".output_plans.xml.gz";
	
	private void run(String[] args) {
		Scenario scenario = loadScenario(netFile, plansFile);
		Population pop = scenario.getPopulation();
		PersonFilter personFilter = new PersonFilter();
		
		for(UserGroup userGroup : UserGroup.values()){
			Population relevantPop = personFilter.getPopulation(pop, userGroup);
			SortedSet<String> usedModes = getUsedModes(relevantPop);
			logger.info("\nThe following transport modes are are used by " + userGroup + ": " + usedModes);
			
			Map<String, Double> mode2AverageBeelineDistance = getMode2AverageBeelineDistance(relevantPop, usedModes);
			Map<String, Integer> mode2NoOfLegs = getMode2NoOfLegs(relevantPop, usedModes);
			int totalNoOfLegs = getTotalNoOfLegs(mode2NoOfLegs);
			
			System.out.println("\n*******************************************************************");
			System.out.println("VALUES FOR " + userGroup);
			System.out.println("*******************************************************************");
			for(String mode : mode2NoOfLegs.keySet()){
				int noOfLegs = mode2NoOfLegs.get(mode);
				double noOfLegPct = BkNumberUtils.roundDouble(100. * ((double) noOfLegs / (double) totalNoOfLegs), 3);
				double averageBeelineDistance_km = mode2AverageBeelineDistance.get(mode) / 1000.;
				System.out.println(mode + ":\t" + noOfLegs + " legs of total " + totalNoOfLegs + " (" + noOfLegPct + "%);\t" +
				averageBeelineDistance_km + " average beeline distance [km]");
			}
			System.out.println("*******************************************************************\n");
		}
		
		SortedSet<String> usedModes = getUsedModes(pop);
		logger.info("\nThe following transport modes are are used: " + usedModes);
		
		Map<String, Double> mode2AverageBeelineDistance = getMode2AverageBeelineDistance(pop, usedModes);
		Map<String, Integer> mode2NoOfLegs = getMode2NoOfLegs(pop, usedModes);
		int totalNoOfLegs = getTotalNoOfLegs(mode2NoOfLegs);
		
		System.out.println("\n*******************************************************************");
		System.out.println("VALUES FOR WHOLE POPULATION");
		System.out.println("*******************************************************************");
		for(String mode : mode2NoOfLegs.keySet()){
			int noOfLegs = mode2NoOfLegs.get(mode);
			double noOfLegPct = BkNumberUtils.roundDouble(100. * ((double) noOfLegs / (double) totalNoOfLegs), 3);
			double averageBeelineDistance_km = mode2AverageBeelineDistance.get(mode) / 1000.;
			System.out.println(mode + ":\t" + noOfLegs + " legs of total " + totalNoOfLegs + " (" + noOfLegPct + "%);\t" +
			averageBeelineDistance_km + " average beeline distance [km]");
		}
		System.out.println("*******************************************************************\n");
	}

	private int getTotalNoOfLegs(Map<String, Integer> mode2NoOfLegs) {
		int totalNoOfLegs = 0;
		for(String mode : mode2NoOfLegs.keySet()){
			int noOfLegs = mode2NoOfLegs.get(mode);
			totalNoOfLegs += noOfLegs;
		}
		return totalNoOfLegs;
	}

	private Map<String, Integer> getMode2NoOfLegs(Population pop, SortedSet<String> usedModes) {
		SortedMap<String, Integer> mode2noOfLegs = new TreeMap<String, Integer>();
		
		for(String mode : usedModes){
			int noOfLegs = 0;
			for(Person person : pop.getPersons().values()){
				Plan plan = (Plan) person.getSelectedPlan();
				List<PlanElement> planElements = plan.getPlanElements();
				for(PlanElement pe : planElements){
					if(pe instanceof Leg){
						Leg leg = (Leg) pe;
						String legMode = leg.getMode();
						if(legMode.equals(mode)){
							noOfLegs ++;
						}

					}
				}
			}
			mode2noOfLegs.put(mode, noOfLegs);
		}
		return mode2noOfLegs;
	}

	private SortedMap<String, Double> getMode2AverageBeelineDistance(Population pop, SortedSet<String> usedModes) {
		SortedMap<String, Double> mode2AverageBeelineDistance = new TreeMap<String, Double>();

		for(String mode : usedModes){
			int noOfLegs = 0;
			double sumOfBeelineDistances = 0.0;
			for(Person person : pop.getPersons().values()){
				Plan plan = (Plan) person.getSelectedPlan();
				List<PlanElement> planElements = plan.getPlanElements();
				for(PlanElement pe : planElements){
					if(pe instanceof Leg){
						Leg leg = (Leg) pe;
						String legMode = leg.getMode();
						if(legMode.equals(mode)){
							final Leg leg2 = leg;
							Coord from = PopulationUtils.getPreviousActivity(plan, leg2).getCoord();
							final Leg leg1 = leg;
							Coord to = PopulationUtils.getNextActivity(plan, leg1).getCoord();
							Double legDist = CoordUtils.calcEuclideanDistance(from, to);
							noOfLegs ++;
							sumOfBeelineDistances += legDist;
						}

					}
				}
			}
			double averageBeelineDistance = sumOfBeelineDistances / noOfLegs;
			mode2AverageBeelineDistance.put(mode, averageBeelineDistance);
		}
		return mode2AverageBeelineDistance;
	}

	private SortedSet<String> getUsedModes(Population pop) {
		SortedSet<String> usedModes = new TreeSet<String>();
		for(Person person : pop.getPersons().values()){
			Plan plan = (Plan) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					String legMode = leg.getMode();
					if(!usedModes.contains(legMode)){
						usedModes.add(legMode);
					}
				}
			}
		}
		return usedModes;
	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		ModalSplitAnalysis msa = new ModalSplitAnalysis();
		msa.run(args);
	}

}
