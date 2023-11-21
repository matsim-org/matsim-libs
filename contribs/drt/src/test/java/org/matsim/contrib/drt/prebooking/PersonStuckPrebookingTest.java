package org.matsim.contrib.drt.prebooking;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Sebastian Hörl (sebhoerl) / IRT SystemX
 */
public class PersonStuckPrebookingTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void baselineTest() {
		/*
		 * Agent personA is performing three drt legs during the day. Agent personB does
		 * exactly the same in parallel, both prebook their requests.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(20000.0);

		Controler controller = environment.build();

		implementPopulation(controller.getScenario().getPopulation());
		PrebookingTest.installPrebooking(controller, false);
		ProbabilityBasedPrebookingLogic.install(controller,
				DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig()), 1.0, 20000.0);

		EventCounter eventCounterA = EventCounter.install(controller, Id.createPersonId("personA"));
		EventCounter eventCounterB = EventCounter.install(controller, Id.createPersonId("personB"));

		controller.run();

		assertEquals(3, eventCounterA.submittedCount);
		assertEquals(3, eventCounterA.dropoffCount);

		assertEquals(3, eventCounterB.submittedCount);
		assertEquals(3, eventCounterB.dropoffCount);
	}

	@Test
	public void cancelTest() {
		/*
		 * Agent personA is performing three drt legs during the day. Agent personB does
		 * exactly the same in parallel, both prebook there requests.
		 *
		 * We cancel the first request of personA. We check that the other reservations
		 * are automatically rejected as soon as the person is stuck.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(20000.0);

		Controler controller = environment.build();

		implementPopulation(controller.getScenario().getPopulation());
		PrebookingTest.installPrebooking(controller, false);
		ProbabilityBasedPrebookingLogic.install(controller,
				DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig()), 1.0, 20000.0);

		EventCounter eventCounterA = EventCounter.install(controller, Id.createPersonId("personA"));
		EventCounter eventCounterB = EventCounter.install(controller, Id.createPersonId("personB"));

		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(getter -> {
					PrebookingManager prebookingManager = getter.getModal(PrebookingManager.class);

					return new MobsimBeforeSimStepListener() {
						@Override
						public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
							if (e.getSimulationTime() == 500.0) {
								prebookingManager.cancel(Id.create("drt_prebooked_0", Request.class));
							}
						}
					};
				}));

				bindModal(PassengerRequestValidator.class).toProvider(modalProvider(getter -> {
					return new PassengerRequestValidator() {
						@Override
						public Set<String> validateRequest(PassengerRequest request) {
							if (!request.getId().toString().contains("prebooked")) {
								return Collections.singleton("anything");
							}

							return Collections.emptySet();
						}
					};
				}));
			}
		});

		controller.run();

		assertEquals(4, eventCounterA.submittedCount);
		assertEquals(4, eventCounterA.rejectedCount);
		assertEquals(0, eventCounterA.dropoffCount);

		assertEquals(3, eventCounterB.submittedCount);
		assertEquals(0, eventCounterB.rejectedCount);
		assertEquals(3, eventCounterB.dropoffCount);
	}

	private void implementPopulation(Population population) {
		PopulationFactory populationFactory = population.getFactory();

		for (String personId : Arrays.asList("personA", "personB")) {
			Person person = populationFactory.createPerson(Id.createPersonId(personId));
			population.addPerson(person);

			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			Activity firstActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("1:1-2:1"));
			firstActivity.setEndTime(2000.0);
			plan.addActivity(firstActivity);

			// departure at 2000
			Leg firstLeg = populationFactory.createLeg("drt");
			plan.addLeg(firstLeg);

			Activity secondActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("5:5-6:5"));
			secondActivity.setEndTime(6000.0);
			plan.addActivity(secondActivity);

			// departure at 6000
			Leg secondLeg = populationFactory.createLeg("drt");
			plan.addLeg(secondLeg);

			Activity thirdActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("1:1-2:1"));
			thirdActivity.setEndTime(10000.0);
			plan.addActivity(thirdActivity);

			// departure at 10000
			Leg thirdLeg = populationFactory.createLeg("drt");
			plan.addLeg(thirdLeg);

			Activity finalActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("5:5-6:5"));
			plan.addActivity(finalActivity);
		}
	}

	static private class EventCounter implements PassengerDroppedOffEventHandler, PassengerRequestSubmittedEventHandler,
			PassengerRequestRejectedEventHandler {
		private final Id<Person> personId;

		private EventCounter(Id<Person> personId) {
			this.personId = personId;
		}

		int dropoffCount = 0;
		int submittedCount = 0;
		int rejectedCount = 0;

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			if (event.getPersonId().equals(personId)) {
				dropoffCount++;
			}
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			if (event.getPersonIds().contains(personId)) {
				submittedCount++;
			}
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			if (event.getPersonIds().contains(personId)) {
				rejectedCount++;
			}
		}

		static EventCounter install(Controler controller, Id<Person> personId) {
			EventCounter instance = new EventCounter(personId);

			controller.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					addEventHandlerBinding().toInstance(instance);
				}
			});

			return instance;
		}
	}
}
