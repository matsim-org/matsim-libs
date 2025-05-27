package org.matsim.contrib.drt.prebooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Sebastian Hörl (sebhoerl) / IRT SystemX
 */
public class VariableStopDurationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	static void prepare(Controler controller) {
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(new PrebookingParams());
	}

	private class CustomStopDurationProvider implements PassengerStopDurationProvider {
		private final Map<String, Tuple<Double, Double>> data = new HashMap<>();

		@Override
		public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
			return data.get(request.getPassengerIds().get(0).toString()).getFirst();
		}

		@Override
		public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
			return data.get(request.getPassengerIds().get(0).toString()).getSecond();
		}

		public CustomStopDurationProvider define(String personId, double pickupDuration, double dropoffDuration) {
			data.put(personId, Tuple.of(pickupDuration, dropoffDuration));
			return this;
		}

		public void install(Controler controller) {
			DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
			PassengerStopDurationProvider self = this;

			controller.addOverridingModule(new AbstractDvrpModeModule(drtConfig.getMode()) {
				@Override
				public void install() {
					bindModal(PassengerStopDurationProvider.class).toInstance(self);
				}
			});
		}
	}

	@Test
	void oneRequestAtDetourLimit() {
		/*-
		 * One agent is dispatched and we set the absolute allowed detour 
		 * such that he can just arrive, one second later, and we need to reject him.
		 * 
		 * This means if we increase the pickup duration or the dropoff duration, the
		 * request should be rejected (see following tests).
		 */
		double absoluteDetour = 292.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, absoluteDetour, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1000.0, requestA.submissionTime, 1e-3);
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1270.0, requestA.dropoffTime, 1e-3);
	}

	@Test
	void oneRequestExceedingDetourLimitThroughPickup() {
		// see previous test
		double absoluteDetour = 292.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, absoluteDetour, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0 + 1.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertTrue(Double.isNaN(requestA.pickupTime));
	}

	@Test
	void oneRequestExceedingDetourLimitThroughDropoff() {
		// see previous test
		double absoluteDetour = 292.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, absoluteDetour, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0 + 1.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertTrue(Double.isNaN(requestA.pickupTime));
	}

	@Test
	void oneRequestAtWaitTimeLimit() {
		/*-
		 * One agent is dispatched and we set the allowed wait time
		 * such that he can just be picked up. One second later, and we need to reject him.
		 * 
		 * This means if we increase the pickup duration, the
		 * request should be rejected (see following test).
		 */
		double maximumWaitTime = 105.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(maximumWaitTime, 1.0, 1000.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1000.0, requestA.submissionTime, 1e-3);
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1270.0, requestA.dropoffTime, 1e-3);
	}

	@Test
	void oneRequestOverWaitTimeLimit() {
		// see above
		double maximumWaitTime = 105.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(maximumWaitTime, 1.0, 1000.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0 + 1.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertTrue(Double.isNaN(requestA.pickupTime));
	}

	@Test
	void twoParallelRequests() {
		/*-
		 * - We dispatch the first request
		 * - Another request is picked up and dropped off along the way
		 * - We adjust the maximum detour of the first request such that
		 *   we find a viable insertion of the second one (with fixed dropoff time)
		 * - If we now increase the dropoff time of the second request, we should not 
		 *   find an insertion anymore (see next test).
		 * 
		 * - Same concept for the wait time
		 */
		double absoluteDetour = 327.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, absoluteDetour, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.addRequest("personB", 3, 0, 5, 0, 1005.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
	}

	@Test
	void twoParallelRequestsPushingDropoffViaIncreasingDropoff() {
		// see above
		double absoluteDetour = 327.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, absoluteDetour, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.addRequest("personB", 3, 0, 5, 0, 1005.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0 + 5.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime)); // expecting rejection
	}

	@Test
	void twoParallelRequestsPushingDropoffViaIncreasingPickup() {
		// see above
		double absoluteDetour = 327.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, absoluteDetour, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, 1000.0) //
				.addRequest("personB", 3, 0, 5, 0, 1005.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0 + 5.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime)); // expecting rejection
	}

	@Test
	void twoSequentialRequests() {
		/*-
		 * - We dispatch the first request
		 * - Another request is picked up and dropped off before
		 * - We adjust the maximum wait time of the first request such that
		 *   we find a viable insertion of the second one (with fixed dropoff time)
		 * - If we now increase the dropoff time of the second request, we should not 
		 *   find an insertion anymore (see next test).
		 * 
		 * - Same concept for the wait time
		 */
		double maximumWaitTime = 315.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(maximumWaitTime, 1.0, 1000.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 6, 0, 8, 0, 1000.0) //
				.addRequest("personB", 3, 0, 5, 0, 1001.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1310.0, requestA.pickupTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1125.0, requestB.pickupTime, 1e-3);
	}

	@Test // FAILING
	void twoSequentialRequestsPushingPickupViaIncreasingDropoff() {
		// see above
		double maximumWaitTime = 315.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(maximumWaitTime, 1.0, 1000.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 6, 0, 8, 0, 1000.0) //
				.addRequest("personB", 3, 0, 5, 0, 1001.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0 + 1.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1311.0, requestA.pickupTime, 1e-3); // expecting no delay

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime)); // expecting rejection
	}

	@Test // FAILING
	void twoSequentialRequestsPushingPickupViaIncreasingPickup() {
		// see above
		double maximumWaitTime = 315.0;

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(maximumWaitTime, 1.0, 1000.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 6, 0, 8, 0, 1000.0) //
				.addRequest("personB", 3, 0, 5, 0, 1001.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		prepare(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0 + 1.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1310.0, requestA.pickupTime, 1e-3); // expecting no delay

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime)); // expecting rejection
	}
}
