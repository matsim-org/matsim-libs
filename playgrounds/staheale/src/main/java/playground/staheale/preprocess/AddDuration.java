/* *********************************************************************** *
 * project: org.matsim.*
 * AddDuration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.staheale.preprocess;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class AddDuration {
	private static Logger log = Logger.getLogger(AdaptPlans.class);
	private ScenarioImpl scenario;

	public AddDuration() {
		super();		
	}
		
	public static void main(String[] args) throws IOException {
		AddDuration addDuration = new AddDuration();
		addDuration.run();
		}

	public void run() {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
	    log.info("Reading network xml file...");
	    MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
		NetworkReader.readFile("./input/miniScenarioNetwork.xml");
		Network network = scenario.getNetwork();
	    log.info("Reading network xml file...done.");
	    log.info("Number of nodes: " +network.getNodes().size());
	    log.info("Number of links: " +network.getLinks().size());
		
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(scenario); 
		PlansReader.readFile("./input/miniScenarioPlans.xml");
		
		MatsimFacilitiesReader FacReader = new MatsimFacilitiesReader(scenario);  
		System.out.println("Reading facilities xml file... ");
		FacReader.readFile("./input/miniScenarioFacilities.xml");
		System.out.println("Reading facilities xml file...done.");
		ActivityFacilities facilities = scenario.getActivityFacilities();
	    log.info("Number of facilities: " +facilities.getFacilities().size());

			
		for (Person p : scenario.getPopulation().getPersons().values()) {
			for (int i=0; i<p.getSelectedPlan().getPlanElements().size();i++){
				PlanElement pe = p.getSelectedPlan().getPlanElements().get(i);
				if (pe instanceof Activity && i!=p.getSelectedPlan().getPlanElements().size()) {
					if (i==0){
						ActivityImpl peAct = (ActivityImpl)pe;
						peAct.setMaximumDuration(peAct.getEndTime());
					}
					else{
						ActivityImpl peAct = (ActivityImpl)pe;
						ActivityImpl act = (ActivityImpl)p.getSelectedPlan().getPlanElements().get(i-2);
						double dur;
						dur = peAct.getEndTime()-act.getEndTime();
						peAct.setMaximumDuration(dur);
					}
				}
			}
		}
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("./output/adaptedPlans.xml");	
	}
}
