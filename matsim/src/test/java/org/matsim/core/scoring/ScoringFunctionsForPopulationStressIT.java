package org.matsim.core.scoring;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import static org.junit.Assert.assertEquals;

public class ScoringFunctionsForPopulationStressIT {

	static final int MAX = 1000000;

	@Test(expected = RuntimeException.class)
	public void exceptionInScoringFunctionPropagates() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Id<Person> personId = Id.createPersonId(1);
		scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(personId));
		EventsManager events = EventsUtils.createEventsManager(config);
		ControlerListenerManagerImpl controlerListenerManager = new ControlerListenerManagerImpl();
		ScoringFunctionFactory throwingScoringFunctionFactory = new ThrowingScoringFunctionFactory();
		ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(controlerListenerManager, events, new EventsToActivities(controlerListenerManager, events), new EventsToLegs(scenario.getNetwork(), events), config.plans(), scenario.getNetwork(), scenario.getPopulation(), throwingScoringFunctionFactory);
		controlerListenerManager.fireControlerIterationStartsEvent(0);
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

	@Test
	public void workWithNewEventsManager() {
		Config config = ConfigUtils.createConfig();
		config.parallelEventHandling().setOneThreadPerHandler(true);
		work(config);
	}

	@Test
	public void workWithOldEventsManager() {
		Config config = ConfigUtils.createConfig();
		config.parallelEventHandling().setNumberOfThreads(8);
		work(config);
	}

	private void work(Config config) {
		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setTypicalDuration(100.0);
		config.planCalcScore().addActivityParams(work);
		PlanCalcScoreConfigGroup.ModeParams car = new PlanCalcScoreConfigGroup.ModeParams("car");
		car.setMarginalUtilityOfTraveling(0.0);
		car.setMarginalUtilityOfDistance(0.0);
		car.setConstant(-1.0);
		config.planCalcScore().addModeParams(car);
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
					public void addMoney(double amount) {
						delegateFunction.addMoney(amount);
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
		ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(controlerListenerManager, events, new EventsToActivities(controlerListenerManager, events), new EventsToLegs(scenario.getNetwork(), events), config.plans(), scenario.getNetwork(), scenario.getPopulation(), scoringFunctionFactory);
		controlerListenerManager.fireControlerIterationStartsEvent(0);
		events.initProcessing();
		for (int i=0; i<MAX; i++) {
			events.processEvent(new PersonMoneyEvent(i*200, personId, 1.0));
			events.processEvent(new ActivityStartEvent(i*200, personId, Id.createLinkId(0), null, "work"));
			events.processEvent(new ActivityEndEvent(i*200 + 100, personId, Id.createLinkId(0), null, "work"));
			events.processEvent(new PersonDepartureEvent(i*200+100, personId, Id.createLinkId(0), "car"));
			events.processEvent(new PersonArrivalEvent(i*200+200, personId, Id.createLinkId(0), "car"));
			events.afterSimStep(i*200+200);
		}
		events.finishProcessing();
		scoringFunctionsForPopulation.finishScoringFunctions();
		assertEquals(60.0 * MAX, scoringFunctionsForPopulation.getScoringFunctionForAgent(personId).getScore(), 1.0);
	}

	@Test
	public void unlikelyTimingOfScoringFunctionStillWorks() {
		Config config = ConfigUtils.createConfig();
		config.parallelEventHandling().setNumberOfThreads(8);
		config.parallelEventHandling().setOneThreadPerHandler(true);
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setTypicalDuration(100.0);
		config.planCalcScore().addActivityParams(work);
		PlanCalcScoreConfigGroup.ModeParams car = new PlanCalcScoreConfigGroup.ModeParams("car");
		car.setMarginalUtilityOfTraveling(0.0);
		car.setMarginalUtilityOfDistance(0.0);
		car.setConstant(-1.0);
		config.planCalcScore().addModeParams(car);
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
					public void addMoney(double amount) {
						try {
							Thread.sleep(MatsimRandom.getRandom().nextInt(1000));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						delegateFunction.addMoney(amount);
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
		ScoringFunctionsForPopulation scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(controlerListenerManager, events, new EventsToActivities(controlerListenerManager, events), new EventsToLegs(scenario.getNetwork(), events), config.plans(), scenario.getNetwork(), scenario.getPopulation(), scoringFunctionFactory);
		controlerListenerManager.fireControlerIterationStartsEvent(0);
		int MAX = 10;
		events.initProcessing();
		for (int i=0; i<MAX; i++) {
			events.processEvent(new PersonMoneyEvent(i*200, personId, 1.0));
			events.processEvent(new ActivityStartEvent(i*200, personId, Id.createLinkId(0), null, "work"));
			events.processEvent(new ActivityEndEvent(i*200 + 100, personId, Id.createLinkId(0), null, "work"));
			events.processEvent(new PersonDepartureEvent(i*200+100, personId, Id.createLinkId(0), "car"));
			events.processEvent(new PersonArrivalEvent(i*200+200, personId, Id.createLinkId(0), "car"));
		}
		events.finishProcessing();
		scoringFunctionsForPopulation.finishScoringFunctions();
		assertEquals(60.0 * MAX, scoringFunctionsForPopulation.getScoringFunctionForAgent(personId).getScore(), 1.0);
	}

}
