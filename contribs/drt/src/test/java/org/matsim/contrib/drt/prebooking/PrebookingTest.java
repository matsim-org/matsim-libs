package org.matsim.contrib.drt.prebooking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Sebastian Hörl (sebhoerl) / IRT SystemX
 */
public class PrebookingTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void withoutPrebookedRequests() {
		/*-
		 * Standard test running with prebooking but without any prebooked requests
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		controller.run();

		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(2000.0, requestInfo.submissionTime, 1e-3);
		assertEquals(2146.0, requestInfo.pickupTime, 1e-3);
		assertEquals(2357.0, requestInfo.dropoffTime, 1e-3);

		var taskInfo = environment.getTaskInfo().get("vehicleA");
		assertEquals("STAY", taskInfo.get(0).type);
		assertEquals("DRIVE", taskInfo.get(1).type);
		assertEquals("STOP", taskInfo.get(2).type);
		assertEquals("DRIVE", taskInfo.get(3).type);
		assertEquals("STOP", taskInfo.get(4).type);

		assertEquals(2001.0, taskInfo.get(1).startTime, 1e-3);

		assertEquals(2086.0, taskInfo.get(2).startTime, 1e-3);
		assertEquals(2146.0, taskInfo.get(2).endTime, 1e-3);
	}

	static PrebookingParams installPrebooking(Controler controller) {
		return installPrebooking(controller, true, new PrebookingParams());
	}

	static PrebookingParams installPrebooking(Controler controller, PrebookingParams prebookingParams) {
		return installPrebooking(controller, true, prebookingParams);
	}

	static PrebookingParams installPrebooking(Controler controller, boolean installLogic) {
		return installPrebooking(controller, installLogic, new PrebookingParams());
	}

	static PrebookingParams installPrebooking(Controler controller, boolean installLogic,
			PrebookingParams prebookingParams) {
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(prebookingParams);

		if (installLogic) {
			AttributeBasedPrebookingLogic.install(controller, drtConfig);
		}

		return drtConfig.getPrebookingParams().get();
	}

	@Test
	void oneRequestArrivingLate() {
		/*-
		 * One request arriving after the requested departure time. Vehicle should wait
		 * and depart with appropriate delay (taking into account a fixed duration for
		 * the person to enter instead of a fixed stop duration).
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				// 1800 indicated but only departing 2000
				.addRequest("personA", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 - 200.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(0.0, requestInfo.submissionTime, 1e-3);
		assertEquals(2060.0, requestInfo.pickupTime, 1e-3);
		assertEquals(2271.0, requestInfo.dropoffTime, 1e-3);

		var taskInfo = environment.getTaskInfo().get("vehicleA");
		assertEquals("STAY", taskInfo.get(0).type);
		assertEquals("DRIVE", taskInfo.get(1).type);
		assertEquals("STAY", taskInfo.get(2).type);
		assertEquals("STOP", taskInfo.get(3).type);
		assertEquals("DRIVE", taskInfo.get(4).type);
		assertEquals("STOP", taskInfo.get(5).type);

		assertEquals(1.0, taskInfo.get(1).startTime, 1e-3); // Pickup drive
		assertEquals(86.0, taskInfo.get(2).startTime, 1e-3); // Starting to wait
		assertEquals(1800.0, taskInfo.get(3).startTime, 1e-3); // Starting stop
		assertEquals(2060.0, taskInfo.get(3).endTime, 1e-3); // Ending stop (260s duration)
		assertEquals(2060.0, taskInfo.get(4).startTime, 1e-3); // Starting drive (ending stop)
	}

	@Test
	void oneRequestArrivingEarly() {
		/*-
		 * One request arriving in advance before the requested departure time. Vehicle
		 * will pickup up agent and then depart after the duration to enter the vehicle.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				// 2200 indicated but already departing 2000
				.addRequest("personA", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 + 200.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(0.0, requestInfo.submissionTime, 1e-3);
		assertEquals(2260.0 + 1.0, requestInfo.pickupTime, 1e-3); // One second for notifying vehicle
		assertEquals(2472.0, requestInfo.dropoffTime, 1e-3);

		var taskInfo = environment.getTaskInfo().get("vehicleA");
		assertEquals("STAY", taskInfo.get(0).type);
		assertEquals("DRIVE", taskInfo.get(1).type);
		assertEquals("STAY", taskInfo.get(2).type);
		assertEquals("STOP", taskInfo.get(3).type);
		assertEquals("DRIVE", taskInfo.get(4).type);
		assertEquals("STOP", taskInfo.get(5).type);

		assertEquals(1.0, taskInfo.get(1).startTime, 1e-3); // Pickup drive
		assertEquals(86.0, taskInfo.get(2).startTime, 1e-3); // Starting to wait
		assertEquals(2200.0, taskInfo.get(3).startTime, 1e-3); // Starting stop
		assertEquals(2261.0, taskInfo.get(3).endTime, 1e-3); // Ending stop (60s)
		assertEquals(2261.0, taskInfo.get(4).startTime, 1e-3); // Starting drive (ending stop)
	}

	@Test
	void twoSequentialRequests() {
		/*-
		 * Two requests that are scheduled in advance.
		 * - First the early one is submitted, then the late one
		 * - First the early one is picked up and dropped off, then the late one.
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("earlyRequest", 0, 0, 5, 5, 2000.0, 0.0) //
				.addRequest("lateRequest", 2, 2, 3, 3, 4000.0, 1000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("earlyRequest");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2272.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("lateRequest");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			assertEquals(4000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4104.0, requestInfo.dropoffTime, 1e-3);
		}
	}

	@Test
	void twoSequentialRequests_inverseSubmission() {
		/*-
		 * Two requests that are scheduled in advance.
		 * - First the late one is submitted, then the early one (inverse of above test).
		 * - First the early one is picked up and dropped off, then the late one.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("earlyRequest", 0, 0, 5, 5, 2000.0, 1000.0) //
				.addRequest("lateRequest", 2, 2, 3, 3, 4000.0, 0.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("earlyRequest");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2272.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("lateRequest");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(4000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4104.0, requestInfo.dropoffTime, 1e-3);
		}
	}

	@Test
	void sameTrip_differentDepartureTime() {
		/*-
		 * Two requests with the same origin and destination, but distinct departure time.
		 * - First, early one is submitted, then late one.
		 * - Vehicle picks up and drops off early one, then late one.
		 * - In total four stops (P,D,P,D)
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("earlyRequest", 1, 1, 5, 5, 2000.0, 1000.0) //
				.addRequest("lateRequest", 1, 1, 5, 5, 4000.0, 1100.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("earlyRequest");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2230.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("lateRequest");
			assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
			assertEquals(4000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4230.0, requestInfo.dropoffTime, 1e-3);
		}

		// Four stops, 2x pickup, 2x dropoff
		assertEquals(4, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void sameTrip_sameDepartureTime() {
		/*-
		 * Two requests with the same origin and destination, and same departure time.
		 * - First, A is submitted, then B.
		 * - Vehicle picks up and A and B, then drops off A and B.
		 * - In total two stops (P,D)
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 1, 1, 5, 5, 2000.0, 1000.0) //
				.addRequest("requestB", 1, 1, 5, 5, 2000.0, 1100.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2230.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2230.0, requestInfo.dropoffTime, 1e-3);
		}

		// Two stops, 1x pickup, 1x dropoff
		assertEquals(2, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void sameTrip_extendPickupDuration() {
		/*-
		 * Two requests with the same origin and destination, different departure times.
		 * - First, A is submitted with departure time 2000
		 * - Then, B is submitted with departure time 2020
		 * - Scheduling A then B would give a total duration of 60 + 60 = 120s stop duration
		 * - Scheduling A then merging in B gives a total duration of 60 + 20 = 80s stop duration
		 * - Expected behavior is to merge the requests
		 * - In total two stops (P,D)
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 1, 1, 5, 5, 2000.0, 1000.0) //
				.addRequest("requestB", 1, 1, 5, 5, 2020.0, 1100.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			// We always add 1s for first pickup
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			// We have additional 19s because stop task is extended!
			assertEquals(2230.0 + 19.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
			// Second pickup does not have one second offset
			assertEquals(2020.0 + 60.0 + 0.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2230.0 + 19.0, requestInfo.dropoffTime, 1e-3);
		}

		// Two stops, 1x pickup, 1x dropoff
		assertEquals(2, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void sameTrip_splitPickup() {
		/*-
		 * Two requests with the same origin and destination, different departure times.
		 * - First, A is submitted with departure time 2000
		 * - Then, B is submitted with departure time 2080
		 * - Stop for A goes from 2000 to 2060
		 * - Stop for B goes from 2080 to 20140
		 * - The requests don't overlap, so we schedule individual stops (in a more extreme use case we could schedule things in between then)
		 * - In total three stops (P, P, D)
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 1, 1, 5, 5, 2000.0, 1000.0) //
				.addRequest("requestB", 1, 1, 5, 5, 2080.0, 1100.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			// Pickup as planned
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);

			// Dropoff a bit later than before because we pickup another one on the way
			assertEquals(2310.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
			// We insert a later pickup
			assertEquals(2080.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);

			// Dropoff as planned
			assertEquals(2310.0, requestInfo.dropoffTime, 1e-3);
		}

		// Three stops, 2x pickup, 1x dropoff
		assertEquals(3, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void sameTrip_inverseSubmission_noPrepending() {
		/*-
		 * Two requests with the same origin and destination, different departure times.
		 * - First, A is submitted with departure time 2020
		 * - Then, B is submitted with departure time 2000
		 *
		 * - This is inverse of sameTrip_extendPickupDuration above, the request with the earlier departure
		 *   time is submitted second.
		 * - We would expect the requests to be merged, but this is not so trivial with the current code base:
		 *   It would be necessary to shift the stop task to the past and extend it. Theoretically, this is
		 *   possible but it would be nice to embed this in a more robust way in the code base.
		 * - Plus, it may change the current DRT behavior because we have these situations also in non-prebooked
		 *   simulations: We may submit an immediate request and find an insertion for a stop task that starts
		 *   in 5 seconds. This case is treated in standard DRT as any other request (extending the task but
		 *   keeping the start time fixed).
		 * - We follow this default behaviour here: Instead of *prepending* the task with the new request, we
		 *   *append* it as usual. If there is at least one pickup in the stop task with the standard stop
		 *   durations, there is no additional cost of adding the request, but the person experiences a wait
		 *   delay that could be minimized if we were able to prepend the task.
		 * - (Then again, do we actually want to do this, it would reserve the vehicle a few seconds more than
		 *   necessary for a stop that is potentially prebooked in a long time).
		 *
		 * - Expected behavior in current implementation: Merge new request with the later departing existing
		 *   one, which generates wait time for the customer.
		 *
		 * - In total two stops (P, D)
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 1, 1, 5, 5, 2020.0, 1000.0) //
				.addRequest("requestB", 1, 1, 5, 5, 2000.0, 1100.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2020.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2250.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 20.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2250.0, requestInfo.dropoffTime, 1e-3);
		}

		// Three stops, 1x pickup, 1x dropoff
		assertEquals(2, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void sameTrip_inverseSubmission_splitPickup() {
		/*-
		 * Two requests with the same origin and destination, different departure times.
		 * - First, A is submitted with departure time 2020
		 * - Then, B is submitted with departure time 1770
		 * - B's departure is 250s before A, so appending it to A would lead to more than 300s
		 *   of wait time, which is not a valid insertion
		 * - Hence, the insertion *after* the pickup of A is not valid
		 * - But the insertion *before* A is possible (it is usually evaluated but dominated by the insertion after)
		 * - TODO: May think this through and eliminate these insertion upfront
		 *
		 * - Expectation: Standard scheduling a prebooked request before another one (as if they had not the
		 *   same origin link).
		 * - In total three stops (P,P,D)
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 1, 1, 5, 5, 2020.0, 1000.0) //
				.addRequest("requestB", 1, 1, 5, 5, 1470.0, 1100.0) //
				.configure(300.0, 2.0, 1800.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1000.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2020.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2249.0 + 1.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
			assertEquals(1470.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2249.0 + 1.0, requestInfo.dropoffTime, 1e-3);
		}

		// Three stops, 2x pickup, 1x dropoff
		assertEquals(3, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void interactionTimes() {
		/*-
		 * Here we test prebookings in combination with non-zero interaction times for pickup and dropoff
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				// 1800 indicated but only departing 2000
				.addRequest("personA", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 - 200.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(PassengerStopDurationProvider.class)
						.toInstance(StaticPassengerStopDurationProvider.of(60.0, 30.0));
			}
		});

		controller.run();

		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(0.0, requestInfo.submissionTime, 1e-3);
		assertEquals(2060.0, requestInfo.pickupTime, 1e-3);
		assertEquals(2301.0, requestInfo.dropoffTime, 1e-3);

		var taskInfo = environment.getTaskInfo().get("vehicleA");
		assertEquals("STAY", taskInfo.get(0).type);
		assertEquals("DRIVE", taskInfo.get(1).type);
		assertEquals("STAY", taskInfo.get(2).type);
		assertEquals("STOP", taskInfo.get(3).type);
		assertEquals("DRIVE", taskInfo.get(4).type);
		assertEquals("STOP", taskInfo.get(5).type);

		assertEquals(1.0, taskInfo.get(1).startTime, 1e-3); // Pickup drive
		assertEquals(86.0, taskInfo.get(2).startTime, 1e-3); // Starting to wait
		assertEquals(1800.0, taskInfo.get(3).startTime, 1e-3); // Starting stop
		assertEquals(2060.0, taskInfo.get(3).endTime, 1e-3); // Ending stop (260s duration)
		assertEquals(2060.0, taskInfo.get(4).startTime, 1e-3); // Starting drive (ending stop)
	}

	@Test
	void destinationEqualsPrebookedOrigin_twoRequests() {
		/*-
		 * In this test, we have two prebooked requests:
		 * P[A] ---------> D[A]     P[B] --------> D[B]
		 * 
		 * The dropoff of A happens at the same place as the pickup of B. Then we dispatch a new request C 
		 * traveling the same trip as A. Without an implemented fix, inserting the dropfof between D[A] and P[B] 
		 * was not an option as P[B] was the same link as the destination of C. The only viable dropoff insertion 
		 * was after P[B], which, however, was too late.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 1, 1, 4, 4, 1000.0, 0.0) //
				.addRequest("requestB", 4, 4, 8, 8, 8000.0, 1.0) //
				.addRequest("requestC", 1, 1, 4, 4, 1000.0, 2.0) //
				.configure(300.0, 2.0, 1800.0, 60.0) //
				.endTime(12.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(0.0, requestInfo.submissionTime, 1e-3);
			assertEquals(1000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(1188.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(1.0, requestInfo.submissionTime, 1e-3);
			assertEquals(8000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(8230.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestC");
			assertEquals(2.0, requestInfo.submissionTime, 1e-3);
			assertEquals(1000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(1188.0, requestInfo.dropoffTime, 1e-3);
		}

		assertEquals(4, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void destinationEqualsPrebookedOrigin_oneRequest() {
		/*-
		 * In this test, we aprebooked requests:
		 * P[A] ---------> D[A]
		 * 
		 * Then we dispatch a new request C before A. The destination of C is the origin of A. Without an implemented fix, 
		 * inserting the dropoff before P[A] was not allowed as it is the same link, but inserting after D[A] was too late.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("requestA", 4, 4, 8, 8, 4000.0, 1.0) //
				.addRequest("requestB", 1, 1, 4, 4, 1000.0, 2.0) //
				.configure(300.0, 2.0, 1800.0, 60.0) //
				.endTime(12.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1.0, requestInfo.submissionTime, 1e-3);
			assertEquals(4000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(4230.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB");
			assertEquals(2.0, requestInfo.submissionTime, 1e-3);
			assertEquals(1000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(1188.0, requestInfo.dropoffTime, 1e-3);
		}

		assertEquals(4, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void intraStopTiming_pickupTooEarly() {
		/*-
		 * In this test, we cover the intra stop timing when we use customizable stop
		 * durations. Before the fix, there was a bug described by the following
		 * situation: 
		 * 
		 * - We look for an insertion for a pickup
		 * - We find an existing stop that already contains some dropoffs
		 * - Inserting the pickup overall will fit (assuming that the persons are dropped off, then picked up)
		 * - But because of variable stop durations, the pickup happens *before* the dropoffs
		 * - Leading to a few seconds in which the occupancy is higher than the vehicle capacity
		 * 
		 * This test led to a VerifyException in VehicleOccupancyProfileCalculator.processOccupancyChange
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.setVehicleCapacity(2) //
				.addRequest("requestA1", 1, 1, 8, 8, 2000.0, 1.0) // forward
				.addRequest("requestA2", 1, 1, 8, 8, 2000.0, 2.0) // forward
				.addRequest("requestB1", 8, 8, 1, 1, 2356.0, 3.0) // backward
				.configure(300.0, 2.0, 1800.0, 60.0) //
				.endTime(12.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(PassengerStopDurationProvider.class).toInstance(new PassengerStopDurationProvider() {
					@Override
					public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
						if (request.getPassengerIds().get(0).toString().startsWith("requestA")) {
							return 60.0;
						} else {
							return 30.0; // shorter than the dropoff duration (see below)
						}
					}

					@Override
					public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
						return 60.0;
					}
				});
			}
		});

		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA1");
			assertEquals(1.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2356.0 + 60.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA2");
			assertEquals(2.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2356.0 + 60.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB1");
			assertEquals(3.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2356.0 + 60.0, requestInfo.pickupTime, 1e-3); // NOT 30s because we need to wait for the
																		// dropoffs
			assertEquals(2753.0 + 60.0, requestInfo.dropoffTime, 1e-3);
		}

		assertEquals(3, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void intraStopTiming_dropoffTooLate() {
		/*-
		 * Inverse situation of the previous test: A new request is inserted, but the dropoff 
		 * happens too late compared to the pickups in the following stop task.
		 * 
		 * This test led to a VerifyException in VehicleOccupancyProfileCalculator.processOccupancyChange
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.setVehicleCapacity(2) //
				.addRequest("requestA", 1, 1, 8, 8, 2000.0, 1.0) // forward
				.addRequest("requestB1", 8, 8, 1, 1, 2356.0, 2.0) // backward
				.addRequest("requestB2", 8, 8, 1, 1, 2356.0, 3.0) // backward
				.configure(300.0, 2.0, 1800.0, 60.0) //
				.endTime(12.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(PassengerStopDurationProvider.class).toInstance(new PassengerStopDurationProvider() {
					@Override
					public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
						return 60.0;
					}

					@Override
					public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
						if (request.getPassengerIds().get(0).toString().equals("requestA")) {
							return 90.0; // longer than the pickups
						} else {
							return 60.0;
						}
					}
				});
			}
		});

		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2000.0 + 60.0 + 1.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2356.0 + 90.0, requestInfo.dropoffTime, 1e-3);
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB1");
			assertEquals(2.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2356.0 + 60.0, requestInfo.pickupTime, 1e-3);
			assertEquals(2753.0 + 60.0 + 30.0, requestInfo.dropoffTime, 1e-3); // +30 because we wait for dropoff of A
																				// for B2 to enter
		}

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestB2");
			assertEquals(3.0, requestInfo.submissionTime, 1e-3);
			assertEquals(2356.0 + 60.0 + 30.0, requestInfo.pickupTime, 1e-3); // +30 because we wait for dropoff of A
			assertEquals(2753.0 + 60.0 + 30.0, requestInfo.dropoffTime, 1e-3); // +30 because we wait for dropoff of A
		}

		assertEquals(3, environment.getTaskInfo().get("vehicleA").stream().filter(t -> t.type.equals("STOP")).count());
	}

	@Test
	void abortAfterRejection_onActivity() {
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.setVehicleCapacity(1) //
				.addRequest("requestA", 1, 1, 8, 8, 2000.0, 1800.0)
				.configure(10.0, 1.0, 0.0, 5.0)
				.endTime(12.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);

		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1800.0, requestInfo.submissionTime);
			assertEquals(Double.NaN, requestInfo.pickupTime, 1e-3);
			assertEquals(1, requestInfo.submissionTimes.size());
			assertEquals(1, environment.getStuckInfo().size());
		}
	}

	@Test
	void abortAfterRejection_onLeg() {
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.setVehicleCapacity(1) //
				.addRequest("requestA", 1, 1, 8, 8, 2000.0, 1800.0)
				.configure(10.0, 1.0, 0.0, 5.0)
				.endTime(12.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);

		// make sure the agent will be on a leg
		for (Person person : controller.getScenario().getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			Activity activity = PopulationUtils.createActivityFromCoord("generic", new Coord(-50000.0, -50000.0));
			activity.setEndTime(0.0);
			Leg leg = PopulationUtils.createLeg("walk");

			plan.getPlanElements().add(0, activity);
			plan.getPlanElements().add(1, leg);
		}

		controller.run();

		{
			RequestInfo requestInfo = environment.getRequestInfo().get("requestA");
			assertEquals(1800.0, requestInfo.submissionTime);
			assertEquals(Double.NaN, requestInfo.pickupTime, 1e-3);
			assertEquals(1, requestInfo.submissionTimes.size());
			assertEquals(1, environment.getStuckInfo().size());
		}
	}
}
