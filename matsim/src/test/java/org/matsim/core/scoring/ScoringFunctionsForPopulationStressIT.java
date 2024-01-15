/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionsForPopulationStressIT.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ScoringFunctionsForPopulationStressIT {

	static final int MAX = 1000000;

	@Test
	void exceptionInScoringFunctionPropagates() {
		assertThrows(RuntimeException.class, () -> {
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			Id<Person> personId = Id.createPersonId(1);
			scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(personId));
			EventsManager events = EventsUtils.createEventsManager(config);
			ControlerListenerManagerImpl controlerListenerManager = new ControlerListenerManagerImpl();
			ScoringFunctionFactory throwingScoringFunctionFactory = new ThrowingScoringFunctionFactory();
			EventsToActivities e2acts = new EventsToActivities(controlerListenerManager);
			EventsToLegs e2legs = new EventsToLegs(scenario.getNetwork());
			EventsToLegsAndActivities e2legsActs = new EventsToLegsAndActivities(e2legs, e2acts);
			events.addHandler(e2legsActs);
			ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(
					controlerListenerManager,
					events,
					e2acts,
					e2legs,
					scenario.getPopulation(),
					throwingScoringFunctionFactory,
					config
			);
			controlerListenerManager.fireControlerIterationStartsEvent(0, false);
			events.processEvent(new PersonMoneyEvent(3600.0, personId, 3.4, "tollRefund", "motorwayOperator"));
			scoringFunctionsForPopulation.finishScoringFunctions();
		});
	}

	private static class ThrowingScoringFunctionFactory implements ScoringFunctionFactory {
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
				public void handleTrip( final TripStructureUtils.Trip trip ) {
					throw new RuntimeException();
				}

				@Override
				public void addMoney(double amount) {
					throw new RuntimeException();
				}

				@Override
				public void addScore(double amount) {
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

	@Test
	void workWithNewEventsManager() {
		Config config = ConfigUtils.createConfig();
		config.eventsManager().setOneThreadPerHandler(true);
		work(config);
	}

	@Test
	void workWithOldEventsManager() {
		Config config = ConfigUtils.createConfig();
		config.eventsManager().setNumberOfThreads(8);
		work(config);
	}

	private void work(Config config) {
		ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
		work.setTypicalDuration(100.0);
		config.scoring().addActivityParams(work);
		ScoringConfigGroup.ModeParams car = new ScoringConfigGroup.ModeParams("car");
		car.setMarginalUtilityOfTraveling(0.0);
		car.setMarginalUtilityOfDistance(0.0);
		car.setConstant(-1.0);
		config.scoring().addModeParams(car);
		final Scenario scenario = ScenarioUtils.createScenario(config);
		Id<Person> personId = Id.createPersonId(1);
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(personId));
		ControlerListenerManagerImpl controlerListenerManager = new ControlerListenerManagerImpl();
		EventsManager events = EventsUtils.createEventsManager(config);
		ScoringFunctionFactory scoringFunctionFactory = new ScoringFunctionFactory() {
			ScoringFunctionFactory delegate = new CharyparNagelScoringFunctionFactory(scenario);
			@Override
			public ScoringFunction createNewScoringFunction(final Person person) {
				return new ScoringFunction() {
					ScoringFunction delegateFunction = delegate.createNewScoringFunction(person);
					@Override
					public void handleActivity(Activity activity) {
						delegateFunction.handleActivity(activity);
					}

					@Override
					public void handleLeg(Leg leg) {
						delegateFunction.handleLeg(leg);
					}

					@Override
					public void agentStuck(double time) {
						delegateFunction.agentStuck(time);
					}

					@Override
					public void handleTrip( final TripStructureUtils.Trip trip ) {
						delegateFunction.handleTrip(trip);
					}

					@Override
					public void addMoney(double amount) {
						delegateFunction.addMoney(amount);
					}

					@Override
					public void addScore(double amount) {
						delegateFunction.addScore(amount);
					}

					@Override
					public void finish() {
						delegateFunction.finish();
					}

					@Override
					public double getScore() {
						return delegateFunction.getScore();
					}

					@Override
					public void handleEvent(Event event) {
						delegateFunction.handleEvent(event);
					}
				};
			}
		};
		EventsToActivities e2acts = new EventsToActivities(controlerListenerManager);
		EventsToLegs e2legs = new EventsToLegs(scenario.getNetwork());

		ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(
				controlerListenerManager,
				events,
				e2acts,
				e2legs,
				scenario.getPopulation(),
				scoringFunctionFactory,
				config
		);
		controlerListenerManager.fireControlerIterationStartsEvent(0, false);
		events.initProcessing();
		for (int i=0; i<MAX; i++) {
			events.processEvent(new PersonMoneyEvent(i*200, personId, 1.0, "tollRefund", "motorwayOperator", null));
			events.processEvent(new ActivityStartEvent(i*200, personId, Id.createLinkId(0), null, "work", null));
			events.processEvent(new ActivityEndEvent(i*200 + 100, personId, Id.createLinkId(0), null, "work", null));
			events.processEvent(new PersonDepartureEvent(i*200+100, personId, Id.createLinkId(0), "car", "car"));
			events.processEvent(new PersonArrivalEvent(i*200+190, personId, Id.createLinkId(0), "car"));
		}
		events.finishProcessing();
		scoringFunctionsForPopulation.finishScoringFunctions();

		//assert when TypicalDurationScoreComputation.uniform
//		assertEquals(60.0 * MAX, scoringFunctionsForPopulation.getScoringFunctionForAgent(personId).getScore(), 1.0);

		//assert when TypicalDurationScoreComputation.relative
		assertEquals(1.0/6.0 * MAX, scoringFunctionsForPopulation.getScoringFunctionForAgent(personId).getScore(), 1.0);
	}

	/*I (mrieser, 2019-01-09) disabled this test. By definition, events for one person should come in the right sequence,
		so this tests actually tests some additional (and potentially optional) behavior. But, more importantly, it poses
		inherent problems with the addition of trip scoring: to detect trips, it is important that activities and legs
		occur in the correct sequence, which means that also the corresponding events must be in the right sequence.
		If the sequence is disturbed, the trip detection already fails. So, with trip scoring, this test would always fail
		as it tests some non-required functionality.
	 */
	@Test
	@Disabled
	void unlikelyTimingOfScoringFunctionStillWorks() {
		Config config = ConfigUtils.createConfig();
		config.eventsManager().setNumberOfThreads(8);
		config.eventsManager().setOneThreadPerHandler(true);
		config.eventsManager().setSynchronizeOnSimSteps(false);
		ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
		work.setTypicalDuration(100.0);
		config.scoring().addActivityParams(work);
		ScoringConfigGroup.ModeParams car = new ScoringConfigGroup.ModeParams("car");
		car.setMarginalUtilityOfTraveling(0.0);
		car.setMarginalUtilityOfDistance(0.0);
		car.setConstant(-1.0);
		config.scoring().addModeParams(car);
		final Scenario scenario = ScenarioUtils.createScenario(config);
		Id<Person> personId = Id.createPersonId(1);
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(personId));
		EventsManager events = EventsUtils.createEventsManager(config);

		ScoringFunctionFactory scoringFunctionFactory = new ScoringFunctionFactory() {
			ScoringFunctionFactory delegate = new CharyparNagelScoringFunctionFactory(scenario);
			@Override
			public ScoringFunction createNewScoringFunction(final Person person) {
				return new ScoringFunction() {
					ScoringFunction delegateFunction = delegate.createNewScoringFunction(person);
					@Override
					public void handleActivity(Activity activity) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.handleActivity(activity);
					}

					@Override
					public void handleLeg(Leg leg) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.handleLeg(leg);
					}

					@Override
					public void agentStuck(double time) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.agentStuck(time);
					}

					@Override
					public void handleTrip( final TripStructureUtils.Trip trip ) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.handleTrip(trip);
					}

					@Override
					public void addMoney(double amount) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.addMoney(amount);
					}

					@Override
					public void addScore(double amount) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.addScore(amount);
					}

					@Override
					public void finish() {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.finish();
					}

					@Override
					public double getScore() {
						return delegateFunction.getScore();
					}

					@Override
					public void handleEvent(Event event) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(100));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.handleEvent(event);
					}
				};
			}
		};
		ControlerListenerManagerImpl controlerListenerManager = new ControlerListenerManagerImpl();
		ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(
				controlerListenerManager,
				events,
				new EventsToActivities(controlerListenerManager),
				new EventsToLegs(scenario.getNetwork()),
				scenario.getPopulation(),
				scoringFunctionFactory,
				config
		);
		controlerListenerManager.fireControlerIterationStartsEvent(0, false);
		int MAX = 10;
		events.initProcessing();
		for (int i=0; i<MAX; i++) {
			events.processEvent(new PersonMoneyEvent(i*200, personId, 1.0, "tollRefund", "motorwayOperator"));
			events.processEvent(new ActivityStartEvent(i*200, personId, Id.createLinkId(0), null, "work"));
			events.processEvent(new ActivityEndEvent(i*200 + 100, personId, Id.createLinkId(0), null, "work"));
			events.processEvent(new PersonDepartureEvent(i*200+100, personId, Id.createLinkId(0), "car", "car"));
			events.processEvent(new PersonArrivalEvent(i*200+200, personId, Id.createLinkId(0), "car"));
		}
		events.finishProcessing();
		scoringFunctionsForPopulation.finishScoringFunctions();

		//assert when TypicalDurationScoreComputation.uniform
//		assertEquals(60.0 * MAX, scoringFunctionsForPopulation.getScoringFunctionForAgent(personId).getScore(), 1.0);

		//assert when TypicalDurationScoreComputation.relative
		assertEquals(1.0/6.0 * MAX, scoringFunctionsForPopulation.getScoringFunctionForAgent(personId).getScore(), 1.0);
	}

}
