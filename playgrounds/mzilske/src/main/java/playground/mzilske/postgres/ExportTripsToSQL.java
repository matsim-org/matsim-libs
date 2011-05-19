package playground.mzilske.postgres;

import java.io.PrintWriter;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.population.algorithms.PersonAlgorithm;

public class ExportTripsToSQL {

	private Config config;
	private String configfile = null;
	private Scenario scenario;
	private String outputFile;
	int nLeg = 0;

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
			configfile = arg;
			if (!argIter.hasNext()) {
				System.out.println("Too few arguments.");
				printUsage();
				System.exit(1);
			}
			outputFile = argIter.next();
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
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(this.configfile);
		sl.loadNetwork();
		final PrintWriter out = new PrintWriter(IOUtils.getBufferedWriter(outputFile, false));
		scenario = sl.getScenario();
		this.config = scenario.getConfig();
		final PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		plans.addAlgorithm(new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				Iterator<? extends Plan> iPlan = person.getPlans().iterator();
				if (iPlan.hasNext()) {
					Plan plan = iPlan.next();
					Iterator<PlanElement> i = plan.getPlanElements().iterator();
					Activity a1;
					Activity a2 = (Activity) i.next();
					while (i.hasNext()) {
						a1 = a2;
						Leg l = (Leg) i.next();
						a2 = (Activity) i.next();
						String line = nLeg + ","
								+ person.getId() + ","
								+ plan.getPlanElements().indexOf(l) + ","
								+ a1.getCoord().getX() + ","
								+ a1.getCoord().getY() + ","
								+ a2.getCoord().getX() + ","
								+ a2.getCoord().getY() + ","
								+ l.getMode();
						out.println(line);
						++nLeg;
					}
				}
			}

		});
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		out.close();
		System.out.println("done.");
	}


	/**
	 * Main method to start the assignment of links to activities.
	 *
	 * @param args Array of arguments, usually passed on the command line.
	 */
	public static void main(final String[] args) {
		new ExportTripsToSQL().run(args);
	}

}
