package playground.wrashid.diverse.erath;

import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
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

public class LoadedNetworkRouter {

	Config config;
	String configfile = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LoadedNetworkRouter loadedNetworkRouter=new LoadedNetworkRouter();
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
		//String rootPath="/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/nomwin_home/MatSimRouting/";
		String rootPath=System.getProperty("user.dir")+"/";
		//String rootPath="/home/wrashid/erath/";

		//String networkFile=rootPath + "network.car.xml.gz";
		String eventsFile=rootPath + "50.events.txt.gz";
		String inputPlansFile=rootPath + "inputPlanFile.xml";
		String outputPlansFile=rootPath + "outputPlanFile.xml";

		parseArguments(args);
		
		Config config = ConfigUtils.loadConfig(this.configfile);
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		Network network = scenario.getNetwork();
		this.config = scenario.getConfig();

		final PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(scenario);
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(outputPlansFile);

		// add algorithm to map coordinates to links
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network, null));

		// add algorithm to estimate travel cost
		// and which performs routing based on that
		TravelTimeCalculator travelTimeCalculator= Events2TTCalculator.getTravelTimeCalculator(scenario, eventsFile);
		TravelDisutilityFactory travelCostCalculatorFactory = new Builder( TransportMode.car );
		TravelDisutility travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes(), this.config.planCalcScore());
		plans.addAlgorithm(
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).get(
						) ) );

		// add algorithm to write out the plans
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(inputPlansFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");
	}

}
