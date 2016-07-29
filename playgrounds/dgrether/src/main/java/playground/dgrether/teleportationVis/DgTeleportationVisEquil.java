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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.utils.LogOutputEventHandler;

/**
 * @author dgrether
 *
 */
public class DgTeleportationVisEquil {

	private static final Logger log = Logger.getLogger(DgTeleportationVisEquil.class);

	public DgTeleportationVisEquil(){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile("../matsim/examples/equil/network.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());

		createPopulation(scenario);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new LogOutputEventHandler());
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true);
		QSim otfVisQSim = QSimUtils.createDefaultQSim(scenario, events);
		// client.setVisualizeTeleportedAgents(true);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
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
			Person pers = fac.createPerson(Id.create(i, Person.class));
			pop.addPerson(pers);
			Activity act1 = fac.createActivityFromCoord("h", new Coord((double) 0, (double) 0));
			Link link1 = sc.getNetwork().getLinks().get(Id.create(id1, Link.class));
			((Activity)act1).setLinkId(link1.getId());
			act1.setEndTime(3600.0);
			Activity act2 = fac.createActivityFromCoord("h", new Coord((double) 5000, (double) 0));
			Link link6 = sc.getNetwork().getLinks().get(Id.create(id2, Link.class));
			((Activity)act2).setLinkId(link6.getId());
			Leg leg = fac.createLeg(TransportMode.walk);
			leg.setTravelTime(600.0 - id1);

			leg.setRoute(new LinkNetworkRouteImpl(link1.getId(), link6.getId()));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}
	}


	public static void main(String[] args){
		new DgTeleportationVisEquil();
	}
}
