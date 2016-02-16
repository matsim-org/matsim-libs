package org.matsim.core.scoring;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class ScoringFunctionsForPopulationTest {

	@Test(expected = RuntimeException.class)
	public void exceptionInScoringFunctionPropagates() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Id<Person> personId = Id.createPersonId(1);
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(personId));
		EventsManager events = EventsUtils.createEventsManager(config);

		ScoringFunctionFactory throwingScoringFunctionFactory = new ThrowingScoringFunctionFactory();
		ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(events, new EventsToActivities(events), new EventsToLegs(scenario.getNetwork(), events), config.plans(), scenario.getNetwork(), scenario.getPopulation(), throwingScoringFunctionFactory);

		events.processEvent(new PersonMoneyEvent(3600.0, personId, 3.4));
		scoringFunctionsForPopulation.finishScoringFunctions();
	}

	private class ThrowingScoringFunctionFactory implements ScoringFunctionFactory {
		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			return new ScoringFunction() {
				@Override
				public void handleActivity(Activity activity) {
					throw new RuntimeException();
				}

				@Override
				public void handleLeg(Leg leg) {
					throw new RuntimeException();
				}

				@Override
				public void agentStuck(double time) {
					throw new RuntimeException();
				}

				@Override
				public void addMoney(double amount) {
					throw new RuntimeException();
				}

				@Override
				public void finish() {
					throw new RuntimeException();
				}

				@Override
				public double getScore() {
					return 0;
				}

				@Override
				public void handleEvent(Event event) {
					throw new RuntimeException();
				}
			};
		}
	}
}
