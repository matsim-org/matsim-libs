package cba;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class DemandModel {

	static void initializePopulation(final Scenario scenario, final double approxPopSize) {

		/*
		 * Create one home location per person, according to hard-coded logic.
		 */

		final List<Link> homeLocs = new ArrayList<>();

		// fact = 1 yields 10'000 persons; see below.
		final double fact = approxPopSize / 10000.0;

		// fact = 1 => 4000 persons
		for (int i = 0; i < fact * 1000; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("1_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_1")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("1_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_1")));
		}

		// fact = 1 => 1200 persons
		for (int i = 0; i < fact * 600; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("5_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_5")));
		}

		// fact = 1 => 3000 persons
		for (int i = 0; i < fact * 500; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("3_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_3")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("3_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_3")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("5_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_5")));
		}

		// fact = 1 => 1800 persons
		for (int i = 0; i < fact * 100; i++) {
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("2_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_2")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("6_9")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_6")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("4_9")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_4")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("8_9")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_8")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("9_7")));
			homeLocs.add(scenario.getNetwork().getLinks().get(Id.createLinkId("7_9")));
		}

		/*
		 * Overwrite the population with corresponding only-home plans.
		 */

		scenario.getPopulation().getPersons().clear();

		final UtilityFunction dummyUtilityFunction = new UtilityFunction(scenario, null, 1, 1);
		final ChoiceModel choiceModel = new ChoiceModel(scenario, dummyUtilityFunction);

		for (int personNumber = 0; personNumber < homeLocs.size(); personNumber++) {
			final Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(personNumber));
			scenario.getPopulation().addPerson(person);

			final Plan plan = choiceModel.simulateChoice(homeLocs.get(personNumber), person);
			person.addPlan(plan);
			plan.setPerson(person);
			person.setSelectedPlan(plan);
		}
	}

	static void replanPopulation(final Scenario scenario, final Provider<TripRouter> tripRouterProvider,
			final double replanProba, final String expectationFileName, final int maxTrials, final int maxFailures) {

		final UtilityFunction utilityFunction = new UtilityFunction(scenario, tripRouterProvider, maxTrials, maxFailures);
		final ChoiceModel choiceModel = new ChoiceModel(scenario, utilityFunction);

		final PrintWriter expectationWriter;
		try {
			expectationWriter = new PrintWriter(expectationFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (Math.random() < replanProba) {
				System.out.println("replanning person " + person.getId());
				final Link homeLoc = scenario.getNetwork().getLinks()
						.get(((Activity) person.getSelectedPlan().getPlanElements().get(0)).getLinkId());
				person.getPlans().clear();
				person.setSelectedPlan(null);
				final Plan plan = choiceModel.simulateChoice(homeLoc, person);
				person.addPlan(plan);
				plan.setPerson(person);
				person.setSelectedPlan(plan);
				expectationWriter.println(person.getId() + "\t" + plan.getScore());
			}
		}

		expectationWriter.flush();
		expectationWriter.close();
	}
}
