package org.matsim.contrib.drt.prebooking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.TaskInfo;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * A prebooked request that is submitted exactly when the passenger departs carries no information in advance. It must
 * therefore be served exactly like an immediate (ad hoc) request: same rejections, same pickup and dropoff times, same
 * vehicle schedules. Each test runs the same scenario twice, once without prebooking and once with all requests
 * prebooked at their departure time, and compares the outcomes.
 */
public class PrebookingAtDepartureEquivalenceTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void sequentialRequestsOnOneVehicle() {
		Map<String, RequestInfo> adHoc = assertEquivalent(environment -> environment //
				.addVehicle("vehicleA", 1, 1) //
				.configure(1800.0, 1.3, 600.0, 60.0), //
				new Trip("personA", 0, 0, 5, 5, 2000.0), //
				new Trip("personB", 5, 5, 0, 0, 2100.0));

		// the vehicle serves personB only after dropping off personA
		assertThat(adHoc.get("personB").pickupTime).isGreaterThanOrEqualTo(adHoc.get("personA").dropoffTime);
	}

	@Test
	void pooledRequests() {
		Map<String, RequestInfo> adHoc = assertEquivalent(environment -> environment //
				.addVehicle("vehicleA", 1, 1) //
				.setVehicleCapacity(2) //
				.configure(600.0, 2.0, 600.0, 60.0), //
				new Trip("personA", 0, 0, 5, 5, 2000.0), //
				new Trip("personB", 1, 0, 5, 5, 2010.0));

		// both passengers are on board at the same time
		assertThat(adHoc.get("personB").pickupTime).isLessThan(adHoc.get("personA").dropoffTime);
	}

	@Test
	void requestRejectedForWaitTime() {
		Map<String, RequestInfo> adHoc = assertEquivalent(environment -> environment //
				.addVehicle("vehicleA", 8, 8) //
				.configure(60.0, 1.3, 600.0, 60.0), //
				new Trip("personA", 0, 0, 5, 5, 2000.0));

		assertThat(adHoc.get("personA").rejected).isTrue();
	}

	@Test
	void secondRequestRejectedForCapacity() {
		Map<String, RequestInfo> adHoc = assertEquivalent(environment -> environment //
				.addVehicle("vehicleA", 1, 1) //
				.setVehicleCapacity(1) //
				.configure(300.0, 1.3, 600.0, 60.0), //
				new Trip("personA", 0, 0, 5, 5, 2000.0), //
				new Trip("personB", 0, 1, 5, 5, 2005.0));

		assertThat(adHoc.get("personA").rejected).isFalse();
		assertThat(adHoc.get("personB").rejected).isTrue();
	}

	private record Trip(String personId, int originX, int originY, int destinationX, int destinationY,
			double departureTime) {
	}

	private record Outcome(Map<String, RequestInfo> requestInfo, Map<String, String> requests,
			Map<String, String> schedules) {
	}

	/**
	 * @return the request outcomes of the ad hoc run, to check that the scenario exercises what it is meant to
	 */
	private Map<String, RequestInfo> assertEquivalent(Consumer<PrebookingTestEnvironment> scenario, Trip... trips) {
		Outcome adHoc = run(scenario, false, trips);
		Outcome prebookedAtDeparture = run(scenario, true, trips);

		assertThat(prebookedAtDeparture.requests()).isEqualTo(adHoc.requests());
		assertThat(prebookedAtDeparture.schedules()).isEqualTo(adHoc.schedules());
		return adHoc.requestInfo();
	}

	private Outcome run(Consumer<PrebookingTestEnvironment> scenario, boolean prebooked, Trip... trips) {
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils).endTime(3600.0);
		scenario.accept(environment);

		for (Trip trip : trips) {
			if (prebooked) {
				environment.addRequest(trip.personId(), trip.originX(), trip.originY(), trip.destinationX(),
						trip.destinationY(), trip.departureTime(), trip.departureTime());
			} else {
				environment.addRequest(trip.personId(), trip.originX(), trip.originY(), trip.destinationX(),
						trip.destinationY(), trip.departureTime());
			}
		}

		Controler controller = environment.build();
		if (prebooked) {
			PrebookingTest.installPrebooking(controller);
		}
		controller.run();

		Map<String, String> requests = new TreeMap<>();
		environment.getRequestInfo().forEach((personId, info) -> requests.put(personId, describe(info)));

		Map<String, String> schedules = new TreeMap<>();
		environment.getTaskInfo().forEach((vehicleId, tasks) -> schedules.put(vehicleId,
				tasks.stream().map(PrebookingAtDepartureEquivalenceTest::describe).collect(Collectors.joining(", "))));

		return new Outcome(Map.copyOf(environment.getRequestInfo()), requests, schedules);
	}

	private static String describe(RequestInfo info) {
		return "submitted=" + info.submissionTimes + " rejected=" + info.rejected + " pickup=" + info.pickupTime
				+ " dropoff=" + info.dropoffTime;
	}

	private static String describe(TaskInfo task) {
		return task.type + "[" + task.startTime + "-" + task.endTime + "]";
	}
}
