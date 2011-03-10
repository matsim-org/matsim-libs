package playground.ciarif.router;

import java.util.Iterator;

import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * Use this tool, if you want to find out the travel times for certain routes for a previous run.
 *
 * input: network, events file, plans (only those, which you want to find out the route/distance/estimated travel time)
 * output: plans file with the route, distance and estimated tavel time filled in.
 *
 * ===============
 * inorder to set it up for a different user:
 * 1.) change root path (and package it as a jar file)
 * 2.) place all files (network, config, events, input (plan) there
 * 3.) change the path of the network in the config file accordingly
 *
 *
 * @author wrashid
 *
 *
 *
 */

public class MyLoadedNetworkRouter {

	Config config;
	String configfile = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MyLoadedNetworkRouter loadedNetworkRouter=new MyLoadedNetworkRouter();
		loadedNetworkRouter.run(args);
	}

	/**
	 * Parses all arguments and sets the corresponding members.
	 *
	 * @param args
	 */
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			System.exit(0);
		} else {
			this.configfile = arg;
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				System.exit(1);
			}
		}
	}

	public void run(final String[] args) {
		
		String rootPathOut="/data/matsim/ciarif/output/routing/";
		String rootPath="/data/matsim/ciarif/input/routing/";
		String networkFile=rootPath + "network.car.xml.gz";
		String eventsFile=rootPath + "50.events.txt.gz";
		String inputPlansFile=rootPath + "inputPlanFile.xml";
		String outputPlansFile=rootPathOut + "outputPlanFileX.xml";

		parseArguments(args);
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(this.configfile);
		sl.loadNetwork();
		NetworkImpl network = sl.getScenario().getNetwork();
		this.config = sl.getScenario().getConfig();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(outputPlansFile);

		// add algorithm to map coordinates to links
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));

		// add algorithm to estimate travel cost
		// and which performs routing based on that
		TravelTimeCalculator travelTimeCalculator= Events2TTCalculator.getTravelTimeCalculator(sl.getScenario(), eventsFile);
		TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
		PersonalizableTravelCost travelCostCalculator = travelCostCalculatorFactory.createTravelCostCalculator(travelTimeCalculator, this.config
				.planCalcScore());
		plans.addAlgorithm(new PlansCalcRoute(this.config.plansCalcRoute(), network, travelCostCalculator, travelTimeCalculator));

		// add algorithm to write out the plans
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(inputPlansFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");
	}

}

