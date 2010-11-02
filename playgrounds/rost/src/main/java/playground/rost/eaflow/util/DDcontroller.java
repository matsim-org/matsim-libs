/* *********************************************************************** *
 * project: org.matsim.*
 * DDcontroller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.rost.eaflow.util;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFEvent2MVI;


public class DDcontroller {

	public static void main(final String[] args) {

    	// choose instance

		//final String netFilename = "./examples/equil/network.xml";
		//final String plansFilename = "./examples/equil/plans100.xml";
		//final String netFilename = "./examples/meine_EA/siouxfalls_network_5s_euclid.xml";
		final String netFilename = "/homes/combi/dressler/V/Project/padang/network/padang_net_evac.xml";
		//final String netFilename = "./examples/meine_EA/swissold_network_5s.xml";

		//final String netFilename = "/homes/combi/dressler/V/Project/padang/network/padang_net_evac.xml";
		//final String plansFilename = "/homes/combi/dressler/V/Project/padang/plans/padang_plans_10p.xml.gz";
		//final String plansFilename = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padangplans.xml";
		//final String plansFilename = "./examples/meine_EA/siouxfalls_plans_5s_euclid_demands_100_empty.xml";
		//final String plansFilename = "/homes/combi/dressler/V/Project/testcases/winnipeg/matsimevac/winnipeg_plans_evac.xml";
		//final String plansFilename = "./examples/meine_EA/swissold_plans_5s_demands_100.xml";
		final String plansFilename = "./examples/meine_EA/padang_plans_100p_flow_2s.xml";

		boolean testplans = false; // FIXME !
		boolean dosim = true;
		boolean otfvis = true;
		boolean netvis = false & (!otfvis);

		ScenarioImpl scenario = new ScenarioImpl();

		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		if (testplans) {
			for (Person person : population.getPersons().values()) {
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					System.out.println("Person " + person.getId() + " has no plan.");
					continue;
				}
				Activity act = ((PlanImpl) plan).getFirstActivity();
				if (act == null) {
					System.out.println("Person " + person.getId() + " has no act.");
					continue;
				}

				Leg leg = ((PlanImpl) plan).getNextLeg(act);
				if (leg == null) {
					System.out.println("Person " + person.getId() + " has no leg.");
					continue;
				}
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				if (route == null) {
					System.out.println("Person " + person.getId() + " has no route.");
					continue;
				}

				Node node2 = network.getLinks().get(route.getStartLinkId()).getToNode();
				Node node1 = null;
				for (int n = 1; n < route.getLinkIds().size(); n++) {
					node1 = network.getLinks().get(route.getLinkIds().get(n)).getFromNode();
					if (!node1.getId().equals(node2.getId())) {
					System.out.println("Person " + person.getId() + " starts on link " + act.getLinkId());
					System.out.println(route.getLinkIds().get(n) + " does not match next link.");
					System.out.println(node1.getId() + " != " + node2.getId());
					}
					node2 = network.getLinks().get(route.getLinkIds().get(n)).getToNode();
				}


			}

			System.out.println("Paths tested.");
		}

		if (dosim) {
			EventsManagerImpl events = new EventsManagerImpl();

			EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
			events.addHandler(eventWriter);

			QSim sim = new QSim(scenario, events);
//			sim.openNetStateWriter("./output/simout", netFilename, 10); // NetVis is no longer supported
			sim.run();

			eventWriter.closeFile();
			System.out.println("Simulation done.");
		}

		if (otfvis) {
      QSim sim = new QSim(scenario, new EventsManagerImpl());
      NetsimNetwork qnet = sim.getNetsimNetwork();

			String eventFile = "./output/events.txt";
			OTFEvent2MVI mviconverter = new OTFEvent2MVI(qnet, eventFile, "./output/otfvis.mvi", 60);
			mviconverter.convert(scenario.getConfig());

			String[] visargs = {"./output/otfvis.mvi"};
			OTFVis.main(visargs);
		}

		if (netvis) {
			String[] visargs = {"./output/simout"};
			// NetVis.main(visargs);
		}

		System.out.println("Done.");

	}

}
