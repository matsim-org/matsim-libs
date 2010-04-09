/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Links.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.mzilske.deteval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.population.algorithms.PersonPrepareForSim;

/**
 * Assigns each activity in each plan of each person in the population a link
 * where the activity takes place based on the coordinates given for the activity.
 * This tool is used for mapping a new demand/population to a network for the first time.
 *
 * @author mrieser
 */
public class RoutePopulation {

	private Config config;

	private String configfile = null;

	private ScenarioImpl scenario;

	private Collection<Node> usedNodes;

	/**
	 * Parses all arguments and sets the corresponding members.
	 *
	 * @param args
	 */
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			printUsage();
			System.exit(0);
		} else {
			this.configfile = arg;
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				printUsage();
				System.exit(1);
			}
		}
	}

	private void printUsage() {

	}

	public void run(final String[] args) {
		parseArguments(args);
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(this.configfile);
		sl.loadNetwork();
		scenario = sl.getScenario();
		final NetworkImpl network = scenario.getNetwork();
		this.config = scenario.getConfig();
		final PopulationImpl plans = scenario.getPopulation();
		plans.setIsStreaming(true);
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		plansWriter.startStreaming(this.config.plans().getOutputFile());
		TravelTime travelTimes = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(network, this.config.travelTimeCalculator());
		PersonalizableTravelCost travelCosts = new TravelCostCalculatorFactoryImpl().createTravelCostCalculator(travelTimes, this.config.charyparNagelScoring());
		plans.addAlgorithm(new PersonPrepareForSim(new PlansCalcRoute(this.config.plansCalcRoute(), network, travelCosts, travelTimes, new DijkstraFactory()), network));
		plans.addAlgorithm(plansWriter);
		DetermineUsedNetworkParts determineUsedNetworkParts = new DetermineUsedNetworkParts(network);
		plans.addAlgorithm(determineUsedNetworkParts);
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
		usedNodes = determineUsedNetworkParts.getUsedNodes();
		removeUnusedNetworkParts();


		System.out.println("done.");
	}

	private void removeUnusedNetworkParts() {
		System.out.println("Used nodes: " + usedNodes.size());
		Collection<Node> allNodes = new ArrayList<Node>(scenario.getNetwork().getNodes().values());
		for (Node node : allNodes) {
			if (!usedNodes.contains(node)) {
				scenario.getNetwork().removeNode(node.getId());
			}
		}
		new NetworkWriter(scenario.getNetwork()).writeFile(scenario.getConfig().network().getOutputFile());
	}

	/**
	 * Main method to start the assignment of links to activities.
	 *
	 * @param args Array of arguments, usually passed on the command line.
	 */
	public static void main(final String[] args) {
		new RoutePopulation().run(args);
	}

}
