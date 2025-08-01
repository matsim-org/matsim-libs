package org.matsim.contrib.drt.prebooking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	private final static double DEPARTURE_A = 1000;
	private final static double DEPARTURE_B = 1001;
	private final static double DEPARTURE_C = 1002;

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

	private class CustomConstraintsCalculator implements DrtRouteConstraintsCalculator {
		private final Map<String, DrtRouteConstraints> data = new HashMap<>();

		public CustomConstraintsCalculator define(String personId,
												  double departureTime,
												  double travelTimeConstraint,
												  double waitTimeConstraint
		) {
			data.put(personId, new DrtRouteConstraints(
					departureTime,
					departureTime + waitTimeConstraint,
					departureTime + travelTimeConstraint,
					Double.POSITIVE_INFINITY,
					Double.POSITIVE_INFINITY,
					0.0,
					true
					)
			);
			return this;
		}

		@Override
		public DrtRouteConstraints calculateRouteConstraints(double departureTime, Link accessActLink,
				Link egressActLink, Person person, Attributes tripAttributes, double unsharedRideTime,
				double unsharedDistance) {
			return data.get(person.getId().toString());
		}

		public void install(Controler controller) {
			DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
			DrtRouteConstraintsCalculator self = this;

			controller.addOverridingModule(new AbstractDvrpModeModule(drtConfig.getMode()) {
				@Override
				public void install() {
					bindModal(DrtRouteConstraintsCalculator.class).toInstance(self);
				}
			});
		}
	}

	@Test
	void oneRequest_travelTimeConstraint_ok() {
		/*-
		 * - One agent is dispatched
		 * - We choose the travel time constraint such that the request can barely accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,   270.0, 1000.0)
			.install(controller);

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
	void oneRequest_travelTimeConstraint_excessivePickupDuration() {
		/*-
		 * - See oneRequest_travelTimeConstraint_ok
		 * - We increase the pickup duration by one second
		 * - Request should not be accepted anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 270.0, 1000.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertTrue(Double.isNaN(requestA.pickupTime));
	}

	@Test
	void oneRequest_travelTimeConstraint_excessiveDropoffDuration() {
		/*-
		 * - See oneRequest_travelTimeConstraint_ok
		 * - We increase the dropoff duration by one second
		 * - Request should not be accepted anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 270.0, 1000.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertTrue(Double.isNaN(requestA.pickupTime));
	}

	@Test
	void oneRequest_waitTimeConstraint_ok() {
		/*-
		 * - One agent is dispatched
		 * - We choose the maximum wait time such that it can barely dispatched
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 1000, 83.0)
			.install(controller);

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
	void oneRequest_waitTimeConstraint_excessivePickupDuration() {
		/*-
		 * - See oneRequest_waitTimeConstraint_ok
		 * - We increase the pickup duration by one second
		 * - The request should not be accepted anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 1000.0, 83.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertTrue(Double.isNaN(requestA.pickupTime));
	}

	@Test
	void twoRequests_ABBA_ok() {
		/*-
		 * - We dispatch two requests
		 * - Structure: Pickup A > Pickup B > Dropoff B > Dropoff A
		 * 
		 * - Request A acts as the cosntraint
		 * - Request B acts as the probe that will be modified later
		 * 
		 * - We adjust the travel time and wait time constraints of request A such that 
		 *   both requests can barely be dispatched
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 5, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 83.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);
	}

	@Test
	void twoRequests_ABBA_excessivePickupDuration() {
		/*-
		 * - See twoRequests_ABBA_ok
		 * - We increase the pickup duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 5, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,392.0, 83.0)
			.define("personB", DEPARTURE_B,300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime)); // expecting rejection

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1270.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void twoRequests_ABBA_excessiveDropoffDuration() {
		/*-
		 * - See twoRequests_ABBA_ok
		 * - We increase the dropoff duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 5, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 83.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime)); // expecting rejection

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1270.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void twoRequests_BBAA_ok() {
		/*-
		 * - We dispatch two requests
		 * - Structure: Pickup B > Dropoff B > Pickup A > Dropoff A
		 * 
		 * - Request A request acts as the constraint
		 * - Request B acts as the probe that will be modified later
		 * 
		 * - We adjust the travel time and wait time constraints of request A such that 
		 *   both requests can barely be dispatched
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 5, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 3, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 289.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1289.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1186.0, requestB.dropoffTime, 1e-3);
	}

	@Test
	void twoRequests_BBAA_excessivePickupDuration() {
		/*-
		 * - See twoRequests_BBAA_ok
		 * - We increase the pickup duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 5, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 3, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 289.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime));

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1167.0, requestA.pickupTime, 1e-3); // earlier than before
		assertEquals(1270.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void twoRequests_BBAA_excessiveDropoffDuration() {
		/*-
		 * - See twoRequests_BBAA_ok
		 * - We increase the dropoff duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 5, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 3, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 289.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime));

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1167.0, requestA.pickupTime, 1e-3); // earlier than before
		assertEquals(1270.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void twoRequests_BABA_ok() {
		/*-
		 * - We dispatch two requests
		 * - Structure: Pickup B > Pickup A > Dropoff B > Dropoff A
		 * 
		 * - Request A request acts as the constraint
		 * - Request B acts as the probe that will be modified later
		 * 
		 * - We adjust the travel time and wait time constraints of request A such that 
		 *   both requests can barely be dispatched
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 3, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 5, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 186.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1186.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);
	}

	@Test
	void twoRequests_BABA_excessivePickupDuration() {
		/*-
		 * - See twoRequests_BABA_ok
		 * - We increase the pickup duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 3, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 5, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 186.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime));

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1125.0, requestA.pickupTime, 1e-3); // earlier than before
		assertEquals(1270.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void twoRequests_BABA_excessiveDropoffDuration() {
		/*-
		 * - See twoRequests_BABA_ok
		 * - We increase the dropoff duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 3, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 5, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 186.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime));

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1125.0, requestA.pickupTime, 1e-3); // earlier than before
		assertEquals(1270.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void twoRequests_ABAB_ok() {
		/*-
		 * - We dispatch two requests
		 * - Structure: Pickup A > Pickup B > Dropoff A > Dropoff B
		 * 
		 * - Request A request acts as the constraint
		 * - Request B acts as the probe that will be modified later
		 * 
		 * - We adjust the travel time and wait time constraints of request A such that 
		 *   both requests can barely be dispatched
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 5, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 7, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 289.0, 83.0)
			.define("personB", DEPARTURE_B, 400.0, 200.0)
			.install(controller);
			
		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1289.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1392.0, requestB.dropoffTime, 1e-3);
	}

	@Test
	void twoRequests_ABAB_excessivePickupDuration() {
		/*-
		 * - See twoRequests_ABAB_ok
		 * - We increase the pickup duration of the probe request by one second
		 * - We should not be able to disaptch it anymore
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 5, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 7, 0, DEPARTURE_B) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 289.0, 83.0)
			.define("personB", DEPARTURE_B, 400.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertTrue(Double.isNaN(requestB.pickupTime));

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1228.0, requestA.dropoffTime, 1e-3); // earlier than before
	}

	@Test
	void threeRequests_ABBA_ok() {
		/*-
		 * - See twoRequests_ABAB_ok
		 * - We add a third request in parallel to the probe
		 * - The third one should be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 5, 0, DEPARTURE_B) //
				.addRequest("personC", 3, 0, 5, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);
		
		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 83.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.define("personC", DEPARTURE_C,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isFinite(requestC.pickupTime)); // expecting acceptance
	}

	@Test
	void threeRequests_ABBA_excessivePickupDuration() {
		/*-
		 * - See twoRequests_ABAB_ok
		 * - We add a third request in parallel to the probe with one second longer pickup duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 5, 0, DEPARTURE_B) //
				.addRequest("personC", 3, 0, 5, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 83.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.define("personC", DEPARTURE_C,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime)); // expecting rejection
	}

	@Test
	void threeRequests_ABBA_excessiveDropoffDuration() {
		/*-
		 * - See twoRequests_ABAB_ok
		 * - We add a third request in parallel to the probe with one second longer dropoff duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 5, 0, DEPARTURE_B) //
				.addRequest("personC", 3, 0, 5, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 83.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.define("personC", DEPARTURE_C,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime)); // expecting rejection
	}

	@Test
	void threeRequests_BBAA_ok() {
		/*-
		 * - See threeRequests_BBAA_ok
		 * - We add a third request in parallel to the probe
		 * - The third one should be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 5, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 3, 0, DEPARTURE_B) //
				.addRequest("personC", 1, 0, 3, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 289.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.define("personC", DEPARTURE_C, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1289.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1186.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isFinite(requestC.pickupTime));
	}

	@Test
	void threeRequests_BBAA_excessivePickupDuration() {
		/*-
		 * - See threeRequests_BBAA_ok
		 * - We add a third request in parallel to the probe with one second longer pickup duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 5, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 3, 0, DEPARTURE_B) //
				.addRequest("personC", 1, 0, 3, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 289.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.define("personC", DEPARTURE_C, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1289.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1186.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime));
	}

	@Test
	void threeRequests_BBAA_excessiveDropoffDuration() {
		/*-
		 * - See threeRequests_BBAA_ok
		 * - We add a third request in parallel to the probe with one second longer dropoff duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 5, 0, 7, 0, 1000.0) //
				.addRequest("personB", 1, 0, 3, 0, 1001.0) //
				.addRequest("personC", 1, 0, 3, 0, 1002.0) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 289.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.define("personC", DEPARTURE_C,  300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1289.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1186.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime));
	}

	@Test
	void threeRequests_BABA_ok() {
		/*-
		 * - See twoRequests_BABA_ok
		 * - We add a third request in parallel to the probe
		 * - The third one should be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 3, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 5, 0, DEPARTURE_B) //
				.addRequest("personC", 1, 0, 5, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  392.0, 186.0)
			.define("personB", DEPARTURE_B,  300.0, 200.0)
			.define("personC", DEPARTURE_C,  300.0, 200.0)
			.install(controller);
			
		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1186.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isFinite(requestC.pickupTime));
	}

	@Test
	void threeRequests_BABA_excessivePickupDuration() {
		/*-
		 * - See twoRequests_BABA_ok
		 * - We add a third request in parallel to the probe with one second longer pickup duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 3, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 5, 0, DEPARTURE_B) //
				.addRequest("personC", 1, 0, 5, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 186.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.define("personC", DEPARTURE_C, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1186.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime));
	}

	@Test
	void threeRequests_BABA_excessiveDropoffDuration() {
		/*-
		 * - See twoRequests_BABA_ok
		 * - We add a third request in parallel to the probe with one second longer dropoff duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 3, 0, 7, 0, DEPARTURE_A) //
				.addRequest("personB", 1, 0, 5, 0, DEPARTURE_B) //
				.addRequest("personC", 1, 0, 5, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 392.0, 186.0)
			.define("personB", DEPARTURE_B, 300.0, 200.0)
			.define("personC", DEPARTURE_C, 300.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0 + 1.0) // DROPOFF + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1186.0, requestA.pickupTime, 1e-3);
		assertEquals(1392.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1083.0, requestB.pickupTime, 1e-3);
		assertEquals(1289.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime));
	}

	@Test
	void threeRequests_ABAB_ok() {
		/*-
		 * - See twoRequests_ABAB_ok
		 * - We add a third request in parallel to the probe
		 * - The third one should be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 5, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 7, 0, DEPARTURE_B) //
				.addRequest("personC", 3, 0, 7, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A,  289.0, 83.0)
			.define("personB", DEPARTURE_B,  400.0, 200.0)
			.define("personC", DEPARTURE_C,  400.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0, 60.0) //
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1289.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1392.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isFinite(requestC.pickupTime));
	}

	@Test
	void threeRequests_ABAB_excessivePickupDuration() {
		/*-
		 * - See twoRequests_ABAB_ok
		 * - We add a third request in parallel to the probe with one second longer pickup duration
		 * - The third request should not be accepted
		 */
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.configure(600.0, 1.0, 600.0, 60.0) //
				.addVehicle("vehicleA", 0, 0) //
				.addRequest("personA", 1, 0, 5, 0, DEPARTURE_A) //
				.addRequest("personB", 3, 0, 7, 0, DEPARTURE_B) //
				.addRequest("personC", 3, 0, 7, 0, DEPARTURE_C) //
				.endTime(10.0 * 3600.0) //
				.useExactTravelTimeEstimates();

		Controler controller = environment.build();
		prepare(controller);

		new CustomConstraintsCalculator()
			.define("personA", DEPARTURE_A, 289.0, 83.0)
			.define("personB", DEPARTURE_B, 400.0, 200.0)
			.define("personC", DEPARTURE_C, 400.0, 200.0)
			.install(controller);

		new CustomStopDurationProvider() //
				.define("personA", 60.0, 60.0) //
				.define("personB", 60.0, 60.0) //
				.define("personC", 60.0 + 1.0, 60.0) // PICKUP + 1s
				.install(controller);

		controller.run();

		RequestInfo requestA = environment.getRequestInfo().get("personA");
		assertEquals(1083.0, requestA.pickupTime, 1e-3);
		assertEquals(1289.0, requestA.dropoffTime, 1e-3);

		RequestInfo requestB = environment.getRequestInfo().get("personB");
		assertEquals(1186.0, requestB.pickupTime, 1e-3);
		assertEquals(1392.0, requestB.dropoffTime, 1e-3);

		RequestInfo requestC = environment.getRequestInfo().get("personC");
		assertTrue(Double.isNaN(requestC.pickupTime));
	}
}
