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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.OTFVisQSim;

import playground.dgrether.utils.LogOutputEventHandler;

/**
 * @author dgrether
 *
 */
public class DgLiveVisEquil {
  
	private static final Logger log = Logger.getLogger(DgLiveVisEquil.class);
	
	public DgLiveVisEquil(){
		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().network().setInputFile("../matsim/examples/equil/network.xml");
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		createPopulation(scenario);
		EventsManager events = new EventsManagerImpl();
		events.addHandler(new LogOutputEventHandler());
		scenario.getConfig().otfVis().setShowTeleportedAgents(true);
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		OTFVisQSim client = new OTFVisQSim(scenario, events);
		client.setVisualizeTeleportedAgents(true);
		client.run();
	}
	
	
	private void createPopulation(Scenario sc) {
		Random r = new Random(234231);
		for (int i = 0; i < 20; i ++){
			int id1 = r.nextInt(23);
			int id2 = r.nextInt(23);
			id1++;
			id2++;
			log.error("Id1: " + id1 + " Id2: " + id2);
			Population pop = sc.getPopulation();
			PopulationFactory fac = pop.getFactory();
			Person pers = fac.createPerson(sc.createId(Integer.toString(i)));
			pop.addPerson(pers);
			Activity act1 = fac.createActivityFromCoord("h", sc.createCoord(0, 0));
			Link link1 = sc.getNetwork().getLinks().get(sc.createId(Integer.toString(id1)));
			((ActivityImpl)act1).setLinkId(link1.getId());
			act1.setEndTime(3600.0);
			Activity act2 = fac.createActivityFromCoord("h", sc.createCoord(5000, 0));
			Link link6 = sc.getNetwork().getLinks().get(sc.createId(Integer.toString(id2)));
			((ActivityImpl)act2).setLinkId(link6.getId());
			Leg leg = fac.createLeg(TransportMode.walk); 
			leg.setTravelTime(600.0 - id1);
     
			leg.setRoute(new LinkNetworkRouteImpl(link1, link6));
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
