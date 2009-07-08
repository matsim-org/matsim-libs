/* *********************************************************************** *
 * project: org.matsim.*
 * PlanFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel.ectm.planfilter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonIdRecorder;
import org.matsim.population.filters.PersonIntersectAreaFilter;

public class PlanFilter {

	/** Generates a list of different combinations of inner and outer radii to figure out
	 * what are the best numbers for the scenario.
	 *
	 * @param args
	 */
	public static void subPopulation(final String[] args) {
		System.out.println("RUN: subPopulation");

		final CoordImpl center = new CoordImpl(683518.0, 246836.0); // Bellevue, Zrh
		double[] smallRadiuses = {5000, 7000, 9000};
		double[] bigRadiuses = {10000, 11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000, 25000, 30000, 40000, 50000};

		ScenarioLoader sl = new ScenarioLoader(args[0]);
		Scenario sc = sl.loadScenario();
		final NetworkLayer network = sc.getNetwork();
		final PopulationImpl population = sc.getPopulation();

		System.out.println("  finding sub-networks... " + (new Date()));
		System.out.println("smallRadius\tbigRadius\t#linksSmall\t#linksBig\t#peopleSmall\t#peopleLeavingBig");
		for (double smallRadius : smallRadiuses) {
			for (double bigRadius : bigRadiuses) {

				final Map<Id, LinkImpl> smallAOI = new HashMap<Id, LinkImpl>();
				final Map<Id, LinkImpl> bigAOI = new HashMap<Id, LinkImpl>();

				for (LinkImpl link : network.getLinks().values()) {
					final NodeImpl from = link.getFromNode();
					final NodeImpl to = link.getToNode();
					if ((CoordUtils.calcDistance(from.getCoord(), center) <= smallRadius) || (CoordUtils.calcDistance(to.getCoord(), center) <= smallRadius)) {
						smallAOI.put(link.getId(),link);
					}
				}
//				System.out.println("  aoi with radius=" + smallRadius + " contains " + smallAOI.size() + " links.");

				for (LinkImpl link : network.getLinks().values()) {
					final NodeImpl from = link.getFromNode();
					final NodeImpl to = link.getToNode();
					if ((CoordUtils.calcDistance(from.getCoord(), center) <= bigRadius) || (CoordUtils.calcDistance(to.getCoord(), center) <= bigRadius)) {
						bigAOI.put(link.getId(),link);
					}
				}
//				System.out.println("  aoi with radius=" + bigRadius + " contains " + bigAOI.size() + " links.");

				final PersonIdRecorder recorder = new PersonIdRecorder();
				final PersonLeavesAreaFilter outsideFilter = new PersonLeavesAreaFilter(recorder, bigAOI);
				final PersonIntersectAreaFilter insideFilter = new PersonIntersectAreaFilter(outsideFilter, smallAOI);
				insideFilter.run(population);
//				System.out.println("  persons travelling in small area: " + insideFilter.getCount());
//				System.out.println("  persons leaving big area: " + outsideFilter.getCount());
				System.out.println(smallRadius + "\t" + bigRadius + "\t" + smallAOI.size() + "\t" + bigAOI.size() + "\t" + insideFilter.getCount() + "\t" + outsideFilter.getCount());

			}
		}

		System.out.println("RUN: subPopulation finished");
	}

	/** Generates the subset of all nodes and links within the bigger radius and creates a network from it.
	 * Generates the subset of all persons traveling through the inner circle. Trips that leave the outer
	 * circle are cut on the border. All Trips following a cut trip from a person are removed.
	 *
	 * @param args
	 */
	public static void generateSubsets(final String[] args) {
		System.out.println("RUN: generateSubset");

		final CoordImpl center = new CoordImpl(683518.0, 246836.0); // Bellevue, Zrh
		double smallRadius = 7000;
		double bigRadius = 14000;

		ScenarioLoader sl = new ScenarioLoader(args[0]);
		Scenario sc = sl.loadScenario();
		final Config config = sc.getConfig();
		final NetworkLayer network = sc.getNetwork();
		final PopulationImpl population = sc.getPopulation();

		System.out.println("  finding AOI links");

		final Map<Id, LinkImpl> smallAOI = new HashMap<Id, LinkImpl>();
		final Map<Id, LinkImpl> bigAOI = new HashMap<Id, LinkImpl>();

		for (LinkImpl link : network.getLinks().values()) {
			final NodeImpl from = link.getFromNode();
			final NodeImpl to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= smallRadius) || (CoordUtils.calcDistance(to.getCoord(), center) <= smallRadius)) {
				smallAOI.put(link.getId(),link);
			}
		}

		// generate sub-net
		NetworkLayer subnet = new NetworkLayer();
		subnet.setName("based on " + config.network().getInputFile());
		subnet.setCapacityPeriod(network.getCapacityPeriod());
		for (LinkImpl link : network.getLinks().values()) {
			final NodeImpl from = link.getFromNode();
			final NodeImpl to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= bigRadius) || (CoordUtils.calcDistance(to.getCoord(), center) <= bigRadius)) {
				bigAOI.put(link.getId(),link);
				NodeImpl fromNode = link.getFromNode();
				if (!subnet.getNodes().containsKey(fromNode.getId())) {
					subnet.createNode(fromNode.getId(), fromNode.getCoord());
				}
				NodeImpl toNode = link.getToNode();
				if (!subnet.getNodes().containsKey(toNode.getId())) {
					subnet.createNode(toNode.getId(), toNode.getCoord());
				}
				subnet.createLink(link.getId(), subnet.getNode(fromNode.getId()), subnet.getNode(toNode.getId()),
						link.getLength(), link.getFreespeed(Time.UNDEFINED_TIME),
						link.getCapacity(Time.UNDEFINED_TIME), link.getNumberOfLanes(Time.UNDEFINED_TIME));
			}
		}
		new NetworkWriter(subnet, "ivtch-osm_zrh14km.xml").write();

		final PopulationWriter plansWriter = new PopulationWriter(population, "plans_miv_zrh7km_cut14km_transitincl_10pct.xml", "v4");
		plansWriter.writeStartPlans();
		final CutTrips cutAlgo = new CutTrips(plansWriter, bigAOI);
		final PersonIntersectAreaFilter insideFilter = new PersonIntersectAreaFilter(cutAlgo, smallAOI);
		insideFilter.run(population);
		plansWriter.writeEndPlans();

		System.out.println("smallR \t bigR \t #linksSmall \t #linksBig \t #personsSmall");
		System.out.println(smallRadius + "\t" + bigRadius + "\t" + smallAOI.size() + "\t" + bigAOI.size() + "\t" + insideFilter.getCount());

		System.out.println("RUN: generateSubset finished");
	}

	public static void main(final String[] args) {
//		subPopulation(args);
		generateSubsets(args);
	}
}
