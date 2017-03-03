package cba.toynet2;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import cba.resampling.ChoiceManager;
import cba.resampling.ChoiceSetFactory;
import floetteroed.utilities.commandlineparser.CommandLineParser;
import matsimintegration.TimeDiscretizationInjection;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Main {

	/*-
	 * ============================================================ 
	 *      PARAMETER SETTINGS
	 * ============================================================
	 */

	private static final String matsimConfigFileName = "./input/cba/toynet/config.xml";
	private static final String populationDotXML = "population.xml";
	private static final String populationFileName = "./input/cba/toynet/" + populationDotXML;
	private static final String populationLogFileName = "./output/cba/toynet/populationLog.txt";
	private static final Random rnd = new Random();

	private static final int outerIts = 15;
	private static final int popSize = 1000;

	private static final double sampersLogitScale = 1.0;
	private static final double sampersDefaultTimeUtil = 0.0;
	private static final double sampersDefaultDestModeUtil = 150.0;

	private static final int numberOfDrawsToGenerateChoiceSet = 10 * 1000;
	private static final int numberOfDrawsToEstimateLogsum = 1 * 1000;

	private static final double explorationProba = 0.1;
	private static final double innovationWeight = 1.0;

	/*-
	 * ============================================================ 
	 *      DEMAND MODEL
	 * ============================================================
	 */

	private static void runDemandModel(final ChoiceManager choiceManager, final Scenario scenario, final int outerIt) {

		if (outerIt == 1) {

			// create population

			scenario.getPopulation().getPersons().clear();
			for (int personNumber = 0; personNumber < popSize; personNumber++) {
				final Person person = scenario.getPopulation().getFactory()
						.createPerson(Id.createPersonId(personNumber));
				scenario.getPopulation().addPerson(person);
			}

			// create choice sets

			final ChoiceSetFactory choiceSetProvider = new VanillaChoiceSetFactory(sampersLogitScale,
					sampersDefaultDestModeUtil, sampersDefaultTimeUtil, rnd, scenario);
			choiceManager.createChoiceSets(scenario.getPopulation(), choiceSetProvider);
		}

		choiceManager.simulateChoices(scenario.getPopulation(), explorationProba);

		final PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popwriter.write(populationFileName);
	}

	/*-
	 * ============================================================ 
	 *      SUPPLY MODEL
	 * ============================================================
	 */

	private static void runSupplyModel(final Scenario scenario, final int outerIt) {
		final Controler controler = new Controler(scenario);

		// The following is most likely needed only for best-response time
		// choice.
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TimeDiscretizationInjection.class);
			}
		});

		controler.run();
	}

	/*-
	 * ============================================================ 
	 *      MAIN LOOP
	 * ============================================================
	 */

	public static void main(String[] args) {
		
		System.out.println("STARTED");

		final ChoiceManager choiceManager = new ChoiceManager(rnd, numberOfDrawsToGenerateChoiceSet,
				numberOfDrawsToEstimateLogsum);
		final PopulationAnalyzer populationAnalyzer = new PopulationAnalyzer(populationLogFileName);

		for (int outerIt = 1; outerIt <= outerIts; outerIt++) {

			System.out.println("OUTER ITERATION " + outerIt + ", running DEMAND model");
			{
				final Config config = ConfigUtils.loadConfig(matsimConfigFileName);
				config.global().setRandomSeed(rnd.nextLong());
				if (outerIt > 1) {
					config.getModule("plans").addParam("inputPlansFile", populationDotXML);
				}
				final Scenario scenario = ScenarioUtils.loadScenario(config);
				runDemandModel(choiceManager, scenario, outerIt);

				for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
					populationAnalyzer.registerChosenPlan((PlanForResampling) choiceManager.getChoice(personId));
				}
			}

			System.out.println("OUTER ITERATION " + outerIt + ", running SUPPLY model");
			{
				final Config config = ConfigUtils.loadConfig(matsimConfigFileName);
				config.global().setRandomSeed(rnd.nextLong());
				config.controler()
						.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
				config.getModule("plans").addParam("inputPlansFile", populationDotXML);
				final Scenario scenario = ScenarioUtils.loadScenario(config);
				runSupplyModel(scenario, outerIt);

				for (Person person : scenario.getPopulation().getPersons().values()) {
					populationAnalyzer.registerExperiencedScore(person);
				}

				choiceManager.updateMATSimTimeScores(scenario.getPopulation(), outerIt == 1 ? 1.0 : innovationWeight);

				populationAnalyzer.registerEstimatedMaximumUtility(
						choiceManager.getEstimatedMaximumUtility(scenario.getPopulation()));
				populationAnalyzer.registerRealizedMaximumUtility(
						choiceManager.getRealizedMaximumUtility(scenario.getPopulation()));
				populationAnalyzer.dumpAnalysis();
				populationAnalyzer.clear();
			}

			System.out.println("DONE");
		}
	}
}
