/* *********************************************************************** *
 * project: org.matsim.*
 * DgLiveVisEquil
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
package playground.dgrether.teleportationVis;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.OTFVisQueueSim;

/**
 * @author dgrether
 *
 */
public class DgLiveVisEquil {

	public DgLiveVisEquil(){
		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().network().setInputFile("./examples/equil/network.xml");
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		createPopulation(scenario);
		EventsManager events = new EventsManagerImpl();
		OTFVisQueueSim client = new OTFVisQueueSim(scenario, events);
		client.run();
	}
	
	
	private void createPopulation(Scenario sc) {
		for (int i = 0; i < 4; i ++){
			Population pop = sc.getPopulation();
			PopulationFactory fac = pop.getFactory();
			Person pers = fac.createPerson(sc.createId(Integer.toString(i)));
			pop.addPerson(pers);
			Activity act1 = fac.createActivityFromCoord("h", sc.createCoord(0, 0));
			((ActivityImpl)act1).setLink(sc.getNetwork().getLinks().get(sc.createId("1")));
			act1.setEndTime(3600.0);
			Activity act2 = fac.createActivityFromCoord("h", sc.createCoord(5000, 0));
			((ActivityImpl)act2).setLink(sc.getNetwork().getLinks().get(sc.createId("6")));
			Leg leg = fac.createLeg(TransportMode.walk);
			leg.setRoute(null);
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}
	}


	public static void main(String[] args){
		new DgLiveVisEquil();
	}
}
