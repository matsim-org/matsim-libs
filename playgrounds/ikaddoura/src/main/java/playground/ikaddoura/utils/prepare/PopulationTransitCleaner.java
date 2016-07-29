/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.utils.prepare;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

/**
 * Replaces all simulated pt legs by teleported pt leg.
 * Replaces all transit_walk legs by the normal walk mode.
 * 
 * @author ikaddoura
 *
 */
public class PopulationTransitCleaner {
	
	static String inputPlansFile;
	static String outputPlansFile;
	static String networkFile;

	private Scenario scenario;
	private static final Logger log = Logger.getLogger(PopulationTransitCleaner.class);
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			inputPlansFile = args[0];		
			log.info("first argument (input plans file): " + inputPlansFile);
			
			outputPlansFile = args[1];
			log.info("second argument (output plans file): " + outputPlansFile);
			
			networkFile = args[2];
			log.info("third argument (network file): " + networkFile);
			
		} else {
			
			inputPlansFile = "/Users/ihab/Documents/workspace/runs-svn/berlin-bvg09/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.selected.xml.gz";
			outputPlansFile = "/Users/ihab/Desktop/bvg.run189.10pct.100.plans.selected.genericPt.xml.gz";
			networkFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/input/network.xml";
		}
		
		PopulationTransitCleaner filter = new PopulationTransitCleaner();
		filter.run();		
	}

	private void run() {
		
		Config config1 = ConfigUtils.createConfig();
		config1.plans().setInputFile(inputPlansFile);
		config1.network().setInputFile(networkFile);
		scenario = ScenarioUtils.loadScenario(config1);
				
		removePtInteractionsAndBelongingTransitWalkLegs();
		renameTransitWalk2Walk();
		
		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation(), null);
		popWriter.write(outputPlansFile);
		
	}

	private void renameTransitWalk2Walk() {
		
		for (Person person : this.scenario.getPopulation().getPersons().values()){
			for (Plan plan : person.getPlans()){
				List<PlanElement> planElements = plan.getPlanElements();
				for (PlanElement pE : planElements) {
					if (pE instanceof Leg){
						Leg leg = (Leg) pE;
						if (leg.getMode().toString().equals(TransportMode.transit_walk)){
							leg.setMode(TransportMode.walk);
						}
					}
				}
			}
		}
		
	}

	private void removePtInteractionsAndBelongingTransitWalkLegs() {
			
		for (Person person : this.scenario.getPopulation().getPersons().values()){
			for (Plan plan : person.getPlans()){
				List<PlanElement> planElements = plan.getPlanElements();
				for (int i = 0, n = planElements.size(); i < n; i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {
							PlanElement previousPe = planElements.get(i-1);
							if (previousPe instanceof Leg) {
								Leg previousLeg = (Leg) previousPe;
								previousLeg.setMode(TransportMode.pt);
								previousLeg.setRoute(null);
							} else {
								throw new RuntimeException("A transit activity should follow a leg! Aborting...");
							}
							final int index = i;
							PopulationUtils.removeActivity(((Plan) plan), index); // also removes the following leg
							n -= 2;
							i--;
						}
					}
				}
			}
		}
	}

}
