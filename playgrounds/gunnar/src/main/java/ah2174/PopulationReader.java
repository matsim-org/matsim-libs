package ah2174;

import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationReader {

	// -------------------- MEMBERS --------------------

	private final String configFileName;

	private final String populationFileName;

	private final String matlabFileName;

	private Population population;

	// -------------------- RUNTIME VARIABLES --------------------

	private double totalTime;

	private double totalDist;

	// -------------------- CONSTRUCTION --------------------

	public PopulationReader(final String configFileName,
			final String populationFileName, final String matlabFileName) {
		this.configFileName = configFileName;
		this.populationFileName = populationFileName;
		this.matlabFileName = matlabFileName;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() throws IOException {

		/*
		 * LOAD CONFIGURATION AND SCENARIO
		 */
		final Config config = ConfigUtils.loadConfig(this.configFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();

		/*
		 * OVERLOAD POPULATION
		 */
		final MatsimPopulationReader popReader = new MatsimPopulationReader(
				scenario);
		popReader.readFile(this.populationFileName);
		this.population = scenario.getPopulation();

		/*
		 * PREPARE WRITING
		 */
		final PrintWriter matlabWriter = new PrintWriter(this.matlabFileName);

		/*
		 * ITERATE THROUGH POPULATION AND WRITE OUT PROPERTIES
		 */
		for (Person person : this.population.getPersons().values()) {

			final Activity home = this.firstActivity(person, "h");
			final Activity work = this.firstActivity(person, "w");

			this.computeTravelStatistics(person);

			matlabWriter.print(person.getId() + ",");
			if (home != null) {
				matlabWriter.print(home.getCoord().getX() + ",");
				matlabWriter.print(home.getCoord().getY() + ",");
			} else {
				matlabWriter.print("NaN,NaN,");
			}

			if (work != null) {
				matlabWriter.print(work.getCoord().getX() + ",");
				matlabWriter.print(work.getCoord().getY() + ",");
			} else {
				matlabWriter.print("NaN,NaN,");
			}
			matlabWriter.print((work != null) ? "1," : "0,");
			if (person.getPlans() == null) {
				matlabWriter.print("0,");
			} else {
				matlabWriter.print(person.getPlans().size() + ",");
			}

			if (person.getSelectedPlan() == null
					|| person.getSelectedPlan().getScore() == null) {
				matlabWriter.print("NaN,");
			} else {
				matlabWriter.print(person.getSelectedPlan().getScore() + ",");
			}

			if (person.getSelectedPlan() == null) {
				matlabWriter.print("NaN,NaN");
			} else {
				matlabWriter.print(this.totalTime + ",");
				matlabWriter.print(this.totalDist);
			}
			matlabWriter.println();
		}

		/*
		 * FINALIZE WRITING
		 */
		matlabWriter.flush();
		matlabWriter.close();
	}

	private Activity firstActivity(final Person person, final String type) {
		final Plan plan = person.getSelectedPlan();
		if (plan == null) {
			return null;
		}
		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Activity) {
				final Activity act = (Activity) element;
				if (act.getType() != null && act.getType().startsWith(type)) {
					return act;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private void computeTravelStatistics(final Person person) {
		this.totalDist = 0.0;
		this.totalTime = 0.0;
		final Plan plan = person.getSelectedPlan();
		if (plan == null) {
			return;
		}
		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Leg) {
				final Leg leg = (Leg) element;
				this.totalTime += leg.getTravelTime();
				this.totalDist += leg.getRoute().getDistance();
			}
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	private static void processOldResults() throws IOException {

		for (int run = 1; run <= 10; run++) {

			final String configFileBaseCase = "config_base-case_" + run
					+ ".xml";
			final String populationFileBaseCase = "output_base-case_" + run
					+ "\\ITERS\\it.100\\run0.100.plans.xml.gz";
			final String matlabFileBaseCase = "matlab_base-case_" + run
					+ ".data";
			final PopulationReader readerBaseCase = new PopulationReader(
					configFileBaseCase, populationFileBaseCase,
					matlabFileBaseCase);
			readerBaseCase.run();

			final String configFileWithTunnel = "config_with-tunnel_" + run
					+ ".xml";
			final String populationFileWithTunnel = "output_with-tunnel_" + run
					+ "\\ITERS\\it.100\\run0.100.plans.xml.gz";
			final String matlabFileWithTunnel = "matlab_with-tunnel_" + run
					+ ".data";
			final PopulationReader readerWithTunnel = new PopulationReader(
					configFileWithTunnel, populationFileWithTunnel,
					matlabFileWithTunnel);
			readerWithTunnel.run();
		}
	}

	public static void main(String[] args) throws IOException {

		System.out.println("UNCOMMENT");
		System.exit(-1);

		processOldResults();

		// final int it = 100;
		//
		// final String path =
		// "C:\\Users\\gunnarfl\\Documents\\teaching\\2013\\simulation\\ex3_ZurichCaseStudy\\exercise3\\";
		// final String configFile = path + "config_base-case_1.xml";
		// final String populationFile = path + "output_base-case_1\\ITERS\\it."
		// + it + "\\run0." + it + ".plans.xml.gz";
		// final String matlabFile = "matlab_base-case_1.data";
		//
		// final PopulationReader reader = new PopulationReader(configFile,
		// populationFile, matlabFile);
		// reader.run();

		// final int it = 0;
		//
		// final String path =
		// "C:\\Users\\gunnarfl\\Documents\\teaching\\2013\\simulation\\ex3_ZurichCaseStudy\\exercise3\\";
		// final String configFile = path + "config_base-case.xml";
		// final String populationFile = path + "output_base-case\\ITERS\\it."
		// + it + "\\run0." + it + ".plans.xml.gz";
		// final String matlabFile = "matlab." + it + ".data";
		//
		// final PopulationReader reader = new PopulationReader(configFile,
		// populationFile, matlabFile);
		// reader.run();

		System.out.println("DONE");

	}
}
