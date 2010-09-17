package playground.andreas.utils.pop;

import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * Assigns each activity in each plan of each person in the population a link
 * where the activity takes place based on the coordinates given for the activity.
 * This tool is used for mapping a new demand/population to a network for the first time.
 *
 * @author mrieser
 */
public class SimpleXY2Links {

	private String networkfile = null;
	private String plansfileIN = null;
	private String plansfileOUT = null;

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
		}
		if (args.length == 3) {			
			this.networkfile = arg;
			this.plansfileIN = argIter.next();
			this.plansfileOUT = argIter.next();
		} else {
			System.out.println("Wrong number of arguments.");
			printUsage();
			System.exit(1);
		}
	}

	private void printUsage() {
		System.out.println();
		System.out.println("Simple XY2Links");
		System.out.println("Reads a plans-file and assignes each activity in each plan of each person");
		System.out.println("a link based on the coordinates given in the activity. The modified plans/");
		System.out.println("persons are then written out to file again.");
		System.out.println();
		System.out.println("usage: XY2Links [OPTIONS] networkfile plansfile plansfileXY2Links");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

	/** Starts the assignment of links to activities.
	 *
	 * @param args command-line arguments
	 */
	public void run(final String[] args) {
		parseArguments(args);
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().addCoreModules();
		scenario.getConfig().network().setInputFile(this.networkfile);
		scenario.getConfig().plans().setInputFile(this.plansfileIN);
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(scenario);
		sl.loadNetwork();
		NetworkImpl network = sl.getScenario().getNetwork();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(this.plansfileOUT);
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(scenario.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
		System.out.println("done.");
	}

	/**
	 * Main method to start the assignment of links to activities.
	 *
	 * @param args Array of arguments, usually passed on the command line.
	 */
	public static void main(final String[] args) {
		new SimpleXY2Links().run(args);
	}

}
