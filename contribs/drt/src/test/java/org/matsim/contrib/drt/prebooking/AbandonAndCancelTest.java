package org.matsim.contrib.drt.prebooking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Sebastian Hörl (sebhoerl) / IRT SystemX
 */
public class AbandonAndCancelTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void noAbandonTest() {
		/*
		 * One person requests to depart at 2000 and also is there at 2000. Another
		 * person asks also to depart at 2000, but only arrives at 4000, i.e. the person
		 * has 1000s delay. The vehicle should wait accordingly.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.addRequest("personOk", 0, 0, 5, 5, 2000.0, 0.0, 2000.0) //
				.addRequest("personLate", 0, 0, 5, 5, 4000.0, 0.0, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		PrebookingTest.installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personOk");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2061.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4271.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personLate");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(4060.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4271.0, requestInfo.dropoffTime, 1e-3);
		}
	}

	@Test
	public void abandonTest() {
		/*
		 * One person requests to depart at 2000 and also is there at 2000. Another
		 * person asks also to depart at 2000, but only arrives at 4000, i.e. the person
		 * has 1000s delay.
		 * 
		 * We configure that the vehicle should leave without the passenger if it waits
		 * longer than 500s. The late request will be rejected!
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.addRequest("personOk", 0, 0, 5, 5, 2000.0, 0.0, 2000.0) //
				.addRequest("personLate", 0, 0, 5, 5, 4000.0, 0.0, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		PrebookingParams parameters = PrebookingTest.installPrebooking(controller);
		parameters.maximumPassengerDelay = 500.0;
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personOk");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2061.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2713.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personLate");
			assertEquals(0.0, requestInfo.submissionTimes.get(0), 1e-3);
			// agent tries a non-prebooked request upon arrival
			assertEquals(4000.0, requestInfo.submissionTimes.get(1), 1e-3);
			assertTrue(requestInfo.rejected);
		}
	}

	@Test
	public void abandonThenImmediateTest() {
		/*
		 * One person requests to depart at 2000 and also is there at 2000. Another
		 * person asks also to depart at 2000, but only arrives at 4000, i.e. the person
		 * has 1000s delay.
		 * 
		 * We configure that the vehicle should leave without the passenger if it waits
		 * longer than 500s. The person will, however, send a new request when arriving
		 * at the departure point and get an immediate vehicle.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.addVehicle("vehicle2", 1, 1) //
				.addRequest("personOk", 0, 0, 5, 5, 2000.0, 0.0, 2000.0) //
				.addRequest("personLate", 0, 0, 5, 5, 4000.0, 0.0, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		PrebookingParams parameters = PrebookingTest.installPrebooking(controller);
		parameters.maximumPassengerDelay = 500.0;
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personOk");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2061.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2713.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personLate");
			assertEquals(0.0, requestInfo.submissionTimes.get(0), 1e-3);
			// agent tries a non-prebooked request upon arrival
			assertEquals(4000.0, requestInfo.submissionTimes.get(1), 1e-3);
			assertEquals(4146.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4357.0, requestInfo.dropoffTime, 1e-3);
			assertTrue(requestInfo.rejected);
		}
	}

	@Test
	public void cancelEarlyTest() {
		/*
		 * One person requests to depart at 2000 and also is there at 2000. Another
		 * person asks also to depart at 2000, but only arrives at 4000, i.e. the person
		 * has 1000s delay.
		 * 
		 * In this test we manually cancel the second request at 500.0 (so before
		 * departure of any agent).
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.addRequest("personOk", 0, 0, 5, 5, 2000.0, 0.0, 2000.0) //
				.addRequest("personLate", 0, 0, 5, 5, 4000.0, 0.0, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		PrebookingTest.installPrebooking(controller);

		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(getter -> {
					PrebookingManager prebookingManager = getter.getModal(PrebookingManager.class);
					QSim qsim = getter.get(QSim.class);

					return new MobsimBeforeSimStepListener() {
						@Override
						public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
							if (e.getSimulationTime() == 500.0) {
								PlanAgent planAgent = (PlanAgent) qsim.getAgents()
										.get(Id.createPersonId("personLate"));
								
								Leg leg = TripStructureUtils.getLegs(planAgent.getCurrentPlan()).get(1);
								
								prebookingManager.cancel(leg);
							}
						}
					};
				}));
			}
		});

		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personOk");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2061.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2272.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personLate");
			assertEquals(0.0, requestInfo.submissionTimes.get(0), 1e-3);
			// agent tries a non-prebooked request upon arrival
			assertEquals(4000.0, requestInfo.submissionTimes.get(1), 1e-3);
			assertTrue(requestInfo.rejected);
		}
	}
	
	@Test
	public void cancelLateTest() {
		/*
		 * One person requests to depart at 2000 and also is there at 2000. Another
		 * person asks also to depart at 2000, but only arrives at 4000, i.e. the person
		 * has 1000s delay.
		 * 
		 * In this test we manually cancel the second request at 3000.0 (so after
		 * departure of the first agent).
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicle", 1, 1) //
				.addRequest("personOk", 0, 0, 5, 5, 2000.0, 0.0, 2000.0) //
				.addRequest("personLate", 0, 0, 5, 5, 4000.0, 0.0, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		PrebookingTest.installPrebooking(controller);

		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(getter -> {
					PrebookingManager prebookingManager = getter.getModal(PrebookingManager.class);
					QSim qsim = getter.get(QSim.class);

					return new MobsimBeforeSimStepListener() {
						@Override
						public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
							if (e.getSimulationTime() == 3000.0) {
								PlanAgent planAgent = (PlanAgent) qsim.getAgents()
										.get(Id.createPersonId("personLate"));
								
								Leg leg = TripStructureUtils.getLegs(planAgent.getCurrentPlan()).get(1);
								
								prebookingManager.cancel(leg);
							}
						}
					};
				}));
			}
		});

		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personOk");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2061.0, requestInfo.pickupTime, 1e-3);
			assertEquals(3212.0, requestInfo.dropoffTime, 1e-3); // still waited quite a bit
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("personLate");
			assertEquals(0.0, requestInfo.submissionTimes.get(0), 1e-3);
			// agent tries a non-prebooked request upon arrival
			assertEquals(4000.0, requestInfo.submissionTimes.get(1), 1e-3);
			assertTrue(requestInfo.rejected);
		}
	}
}
