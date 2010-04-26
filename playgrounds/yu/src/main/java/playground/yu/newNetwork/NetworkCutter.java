/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCutter.java
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

/**
 *
 */
package playground.yu.newNetwork;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * ensures only links in a rectangle that could be passed over by the plans can
 * be retained in the network.
 *
 * @author yu
 *
 */
public class NetworkCutter {
	private double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0,
			maxY = 0;

	private void printUsage() {
		System.out.println();
		System.out.println("NetworkCutter");
		System.out
				.println("Reads a network-file and \"cut\" it. Currently, it performs the following");
		System.out
				.println("steps to ensure a network is suited for simulation:");
		System.out
				.println(" - ensure only links in a rectangle that could be passed over by the plans can be retained in the network.");
		System.out.println();
		System.out
				.println("usage: NetworkCutter [OPTIONS] input-network-file input-plans-file output-network-file");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

	/**
	 * Runs the network cutting algorithms over the network and the plans read
	 * in from <code>inputNetworkFile</code> and <code>plansFile</code>, writes
	 * the resulting ("cleaned") network to the specified file.
	 *
	 * @param inputNetworkFile
	 *            filename of the network to be handled
	 * @param plansFile
	 *            filenmae of the plans to be handled
	 * @param outputNetworkFile
	 *            filename where to write the cleaned network to
	 */
	public void run(final String inputNetworkFile, final String plansFile,
			final String outputNetworkFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);

		final Population pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFile);

		run(network, pop);

		new NetworkWriter(network).writeFile(outputNetworkFile);
	}

	public void run(Network net, Population pop) {
		for (Person person : pop.getPersons().values())
			for (Plan plan : person.getPlans())
				for (PlanElement pe : plan.getPlanElements())
					if (pe instanceof ActivityImpl)
						resetBoundary(((ActivityImpl) pe).getCoord());
					else {
						RouteWRefs route = ((LegImpl) pe).getRoute();
						if (route != null
								&& (route instanceof NetworkRoute))
							resetBoundary((NetworkRoute) route, net);
					}
		Set<Link> links = new HashSet<Link>();
		links.addAll(net.getLinks().values());
		for (Link link : links)
			if (!inside(link))
				net.removeLink(link.getId());
		Set<Node> nodes = new HashSet<Node>();
		nodes.addAll(net.getNodes().values());
		for (Node node : nodes)
			if (!inside(node))
				net.removeNode(node.getId());
	}

	// not perfect, but it's enough to test
	private boolean inside(Link link) {
		return inside(link.getFromNode()) || inside(link.getToNode());
	}

	private boolean inside(Node node) {
		Coord crd = node.getCoord();
		double x = crd.getX();
		double y = crd.getY();
		return x >= minX - 1000 && x <= maxX + 1000 && y >= minY - 1000
				&& y <= maxY + 1000;
	}

	private void resetBoundary(NetworkRoute route, Network net) {
		for (Id linkId : route.getLinkIds()) {
			resetBoundary(net.getLinks().get(linkId));
		}
		resetBoundary(net.getLinks().get(route.getStartLinkId()));
		resetBoundary(net.getLinks().get(route.getEndLinkId()));
	}

	private void resetBoundary(Link link) {
		resetBoundary(link.getFromNode().getCoord());
		resetBoundary(link.getToNode().getCoord());
	}

	private void resetBoundary(Coord crd) {
		double x = crd.getX();
		double y = crd.getY();
		if (x < minX)
			minX = x;
		if (x > maxX)
			maxX = x;
		if (y < minY)
			minY = y;
		if (y > maxY)
			maxY = y;
	}

	/**
	 * Runs the network cutting algorithms over the network read in from the
	 * argument list, and writing the resulting network out to a file again
	 *
	 * @param args
	 *            <code>args[0]</code> filename of the network to be handled,
	 *            <code>args[1]</code> filename of the population to be
	 *            simulated, <code>args[2]</code> filename where to write the
	 *            cleaned network to
	 */
	public void run(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			printUsage();
			throw new RuntimeException("Too few arguments.");
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			printUsage();
			System.exit(0);
		} else {
			String inputFile = arg;
			if (!argIter.hasNext()) {
				System.out.println("Too few arguments.");
				printUsage();
				throw new RuntimeException("Too few arguments.");
			}

			String plansFile = argIter.next();
			if (!argIter.hasNext()) {
				System.out.println("Too few arguments.");
				printUsage();
				throw new RuntimeException("Too few arguments.");
			}

			String outputFile = argIter.next();
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				printUsage();
				throw new RuntimeException("Too many arguments.");
			}

			run(inputFile, plansFile, outputFile);
		}
	}

	public static void main(String[] args) {
		new NetworkCutter()
				.run(new String[] {
						"../berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz",
						"../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/plan.routedOevModell.BVB344.xml",
						"../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml.gz" });
	}

}
