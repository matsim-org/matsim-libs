/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.analysis.microcensus;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.analysis.Bins;


public class AnalyzeMicrocensus {

	private final static Logger log = Logger.getLogger(AnalyzeMicrocensus.class);
	private String outputFolder="src/main/java/playground/anhorni/output/microcensus/";
	private String plansFilePath="src/main/java/playground/anhorni/input/microcensus/plansMOSO.xml";
	
	private Bins shoppingDistanceDistribution = new Bins(200.0, 5000.0, "shopping microcensus");
	private Bins shoppingDistanceDistributionHomeBased = new Bins(200.0, 5000.0, "shopping microcensus home-based");
	
	private ScenarioImpl scenario = new ScenarioImpl();
		
	public static void main(final String[] args) {
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		analyzer.init();
		analyzer.run();	
		analyzer.write();
		log.info("Analysis finished -----------------------------------------");
	}
	
	private void init() {
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(this.plansFilePath);	
	}
	
	private void run() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().equals("s")) {
						ActivityImpl previousAct = (ActivityImpl) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
						double distance = ((CoordImpl)previousAct.getCoord()).calcDistance(act.getCoord());
						shoppingDistanceDistribution.addVal(distance);
						
						if (previousAct.getType().equals("h")) {
							shoppingDistanceDistributionHomeBased.addVal(distance);	
						}
					}
				}
			}
		}	
	}
			
	private void write() {
		shoppingDistanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m");
		shoppingDistanceDistributionHomeBased.plotBinnedDistribution(this.outputFolder, "m", "m");
	}
}
