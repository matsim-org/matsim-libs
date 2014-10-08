/* *********************************************************************** *
 * project: org.matsim.*
 * PtPlansFileCreator.java
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
package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author yu
 * 
 */
public class PtPlansFileCreator {
	private int personCount = 0;
	private Population pop = null;

	public void setPop(final Population pop) {
		this.pop = pop;
	}

	public void setNetwork(final Network network) {
		this.network = network;
	}

	public Population getPop() {
		return pop;
	}

	private Network network = null;

	public Network getNetwork() {
		return network;
	}

	/**
	 *
	 */
	public PtPlansFileCreator() {
	}

	public void createPersons() {
		// north
		createNorthBus245Person("04:27");
		createNorthBus245Person("04:47");
		createNorthBus245Person("05:07");
		createNorthBus245Person("05:27");
		createNorthBus245Person("05:45");
		createNorthBus245Person("05:52");
		createNorthBus245Person("05:58");
		for (int i = 6; i < 20; i++) {
			for (int j = 0; j < 6; j++) {
				createNorthBus245Person((i < 10 ? "0" + i : i) + ":" + j + "8");
			}
		}
		for (int i = 20; i < 24; i++) {
			for (int j = 0; j < 5; j += 2) {
				createNorthBus245Person(i + ":" + j + "7");
			}
		}
		createNorthBus245Person("00:07");
		// south
		createSouthBus245Person("04:58");
		createSouthBus245Person("05:18");
		createSouthBus245Person("05:38");
		createSouthBus245Person("05:48");
		createSouthBus245Person("05:58");

		for (int j = 1; j < 6; j++) {
			createSouthBus245Person("06:" + j + "0");
		}

		for (int i = 7; i < 19; i++) {
			for (int j = 0; j < 6; j++) {
				createSouthBus245Person((i < 10 ? "0" + i : i) + ":" + j + "0");
			}
		}

		for (int j = 0; j < 5; j++) {
			createSouthBus245Person("19:" + j + "0");
		}

		createSouthBus245Person("20:00");

		for (int i = 20; i < 24; i++) {
			for (int j = 1; j < 6; j += 2) {
				createSouthBus245Person(i + ":" + j + "8");
			}
		}

		createSouthBus245Person("00:18");
	}

	public List<Link> getSrcRoute(final int[] nodes) {
		List<Node> srcRoute = new ArrayList<Node>();
		for (int i = 0; i < nodes.length; i++) {
			srcRoute.add(network.getNodes().get(Id.create(nodes[i], Node.class)));
		}
		return RouteUtils.getLinksFromNodes(srcRoute);
	}

	protected void createSouthBus245Person(final String endTime) {
		int[] southNodes = { 2857, 2856, 2855, 1000142, 2850, 1000134, 2848,
				6575, 2787, 2805, 1000544, 6362, 1000540, 2768, 1000580, 2765,
				2763, 2727, 2728, 1915, 1918, 1917, 1867, 1872, 1869, 1865,
				1000447, 1866, 1854, 1853, 1852, 1863, 1000448, 1859, 1861,
				1849, 1892, 1893, 1000454, 1906 };
		createPtPerson("7622", endTime, "1982", getSrcRoute(southNodes));
	}

	protected void createNorthBus245Person(final String endTime) {
		int[] northNodes = { 1906, 1000453, 1892, 1849, 1861, 1859, 1000448,
				1863, 1856, 1854, 1866, 1000447, 1865, 1869, 1872, 1867, 1917,
				1918, 1915, 2728, 2727, 2763, 2765, 1000580, 2768, 1000540,
				6362, 1000544, 2805, 2787, 6575, 2848, 1000134, 2850, 1000142,
				2855, 2856, 2857 };
		createPtPerson("1981", endTime, "7621", getSrcRoute(northNodes));
	}

	@SuppressWarnings("deprecation")
	private void createPtPerson(final String startLinkId, final String endTime,
			final String endLinkId, final List<Link> srcRoute) {

		PersonImpl p = new PersonImpl(Id.create("245-" + personCount, Person.class));
		try {
			PlanImpl pl = new org.matsim.core.population.PlanImpl(p);
			p.addPlan(pl);
			Id<Link> startLinkID = Id.create(startLinkId, Link.class);
			ActivityImpl a = pl.createAndAddActivity("h", startLinkID);
			a.setEndTime(Time.parseTime(endTime));
			LegImpl leg = pl.createAndAddLeg(TransportMode.car);
			leg.setDepartureTime(Time.parseTime(endTime));
			NetworkRoute route = new LinkNetworkRouteImpl(null, null);
			leg.setRoute(route);
			Id<Link> endLinkID = Id.create(endLinkId, Link.class);
			pl.createAndAddActivity("w", endLinkID);
			route.setLinkIds(startLinkID, NetworkUtils.getLinkIds(srcRoute),
					endLinkID);
			pop.addPerson(p);
			personCount++;
			System.out.println("i have " + pop.getPersons().size()
					+ " persons\t" + startLinkId + "\t" + endTime + "\t"
					+ endLinkId);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String newPlansFilename = "output/bvg/245.xml.gz";
		String netFilename = "test/scenarios/berlin/network.xml.gz";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());

		PtPlansFileCreator pfc = new PtPlansFileCreator();
		pfc.setNetwork(scenario.getNetwork());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		pfc.setPop(scenario.getPopulation());
		pfc.createPersons();
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
				.write(newPlansFilename);
		System.out.println("done.");
	}
}
