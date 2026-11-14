/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.prebooking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Dynamic stop selection with prebooking.
 * <p>
 * Pickup: the passenger lives next to stop A, stop B is 200 m away (312 s walk). The vehicle comes from the north and
 * reaches B before A. With one stop candidate the passenger is picked up at A, with two at B, provided the vehicle
 * arrives at B only after the passenger (otherwise waiting for the later passenger at B costs vehicle time). If the
 * request is not yet scheduled when the passenger wants to leave, the passenger waits in the activity for the offer.
 * <p>
 * Dropoff: the passenger's destination is at stop E, stop F lies on the way. With two candidates, the passenger is
 * dropped off at F and walks the rest.
 *
 * @author nkuehnel / MOIA
 */
public class PrebookingStopSelectionTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private static final Id<Link> STOP_A = Id.createLinkId("3:5-4:5");
	private static final Id<Link> STOP_B = Id.createLinkId("3:6-4:6");
	private static final Id<Link> STOP_E = Id.createLinkId("3:9-4:9");
	private static final Id<Link> STOP_F = Id.createLinkId("4:8-5:8");
	private static final Id<Link> DESTINATION = STOP_E;
	private static final double DEPARTURE_TIME = 8 * 3600;

	// slow vehicle (100 s per link) that only leaves when the request is booked 100 s before departure
	private static final double SLOW_SPEED = 2.0;
	private static final double LATE_BOOKING = DEPARTURE_TIME - 100;

	@Test
	void testOneCandidatePicksUpAtTheClosestStop() {
		Result result = runPickup(1, LATE_BOOKING);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_A);
		assertThat(result.finalArrivalLinks).containsExactly(DESTINATION);
		assertThat(result.stuck).isEmpty();
	}

	@Test
	void testTwoCandidatesPickUpAtTheCheaperStop() {
		Result result = runPickup(2, LATE_BOOKING);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_B);
		// the agent walked to B: 200 m beeline * 1.3 at the default walk speed of 3 km/h
		assertThat(result.drtDepartures.getFirst().getTime()).isEqualTo(DEPARTURE_TIME + 312, offset(1.0));
		assertThat(result.finalArrivalLinks).containsExactly(DESTINATION);
		assertThat(result.stuck).isEmpty();

		// the plan in the population keeps the routed stop, so the next iteration books the same request
		assertThat(drtLeg(result.person).getRoute().getStartLinkId()).isEqualTo(STOP_A);
	}

	@Test
	void testAdHocRequestsIgnoreStopCandidates() {
		// retrofit: without a submission time the passenger walks to the routed stop and books there (ad hoc), the
		// candidates in the route are ignored
		Result result = runPickup(2, Double.NaN);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_A);
		assertThat(result.originActivityEnds).containsExactly(DEPARTURE_TIME);
		assertThat(result.finalArrivalLinks).containsExactly(DESTINATION);
		assertThat(result.stuck).isEmpty();
		assertThat(drtLeg(result.person).getRoute()).isInstanceOfSatisfying(DrtRoute.class,
				route -> assertThat(route.getStopCandidates()).isPresent());
	}

	@Test
	void testIdleVehicleWaitingForTheLaterPassengerKeepsTheClosestStop() {
		// booked at the start of the day: the vehicle is at A or B long before the passenger, and the later earliest
		// pickup at B (walk) costs more vehicle time than the shorter drive saves
		Result result = runPickup(2, 0.0);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_A);
		assertThat(result.stuck).isEmpty();
	}

	@Test
	void testBookingJustBeforeTheActivityEndHoldsThePassengerUntilScheduled() {
		// booked 1 s before leaving: the passenger waits in the activity for the offer and then walks to B
		Result result = runPickup(2, DEPARTURE_TIME - 1, new PrebookingParams(), false);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_B);
		assertThat(result.originActivityEnds).singleElement()
				.satisfies(time -> assertThat(time).isBetween(DEPARTURE_TIME + 1, DEPARTURE_TIME + 5));
		assertThat(result.drtDepartures.getFirst().getTime())
				.isEqualTo(result.originActivityEnds.getFirst() + 312, offset(1.0));
		assertThat(result.finalArrivalLinks).containsExactly(DESTINATION);
		assertThat(result.stuck).isEmpty();
	}

	@Test
	void testBookingAtTheActivityEndHoldsThePassengerUntilScheduled() {
		// booked when leaving ("book first, then walk"): the request is submitted before the activity end of the same
		// time step is processed, so the passenger must be held right away, not only after the step
		Result result = runPickup(2, DEPARTURE_TIME, new PrebookingParams(), false);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_B);
		assertThat(result.originActivityEnds).singleElement()
				.satisfies(time -> assertThat(time).isBetween(DEPARTURE_TIME + 1, DEPARTURE_TIME + 5));
		assertThat(result.drtDepartures.getFirst().getTime())
				.isEqualTo(result.originActivityEnds.getFirst() + 312, offset(1.0));
		assertThat(result.finalArrivalLinks).containsExactly(DESTINATION);
		assertThat(result.stuck).isEmpty();
	}

	@Test
	void testScheduledBeforeTheActivityEndDoesNotDelayThePassenger() {
		Result result = runPickup(2, LATE_BOOKING);

		assertThat(result.originActivityEnds).containsExactly(DEPARTURE_TIME);
	}

	@Test
	void testRejectedRequestReleasesTheHeldPassenger() {
		PrebookingParams params = new PrebookingParams();
		params.setAbortRejectedPrebookings(false);
		// the vehicle cannot reach any stop within the maximum wait time of 60 s
		Result result = runPickup(2, DEPARTURE_TIME - 1, params, true);

		assertThat(result.rejected).isTrue();
		// released right after the rejection, not held until the end of the day
		assertThat(result.originActivityEnds).singleElement()
				.satisfies(time -> assertThat(time).isBetween(DEPARTURE_TIME, DEPARTURE_TIME + 5));
	}

	@Test
	void testGroupIsPickedUpTogetherAtTheCheaperStop() {
		PrebookingTestEnvironment environment = pickupEnvironment(2, LATE_BOOKING, false) //
				.addRequest("personB", 3, 5, 3, 9, DEPARTURE_TIME, LATE_BOOKING);
		Controler controller = environment.build();
		PrebookingGroupTest.installPrebooking(controller);
		Result result = run(environment, controller);

		assertThat(result.drtDepartures).extracting(e -> e.getLinkId()).containsExactly(STOP_B, STOP_B);
		assertThat(environment.getRequestInfo().get("personA").drtRequestId)
				.isEqualTo(environment.getRequestInfo().get("personB").drtRequestId);
		assertThat(result.finalArrivalLinks).containsExactly(DESTINATION, DESTINATION);
		assertThat(result.stuck).isEmpty();
	}

	@Test
	void testTwoCandidatesDropOffOnTheWay() {
		Result withOne = runDropoff(1);
		Result withTwo = runDropoff(2);

		assertThat(withOne.drtArrivalLinks).containsExactly(STOP_E);
		assertThat(withTwo.drtArrivalLinks).containsExactly(STOP_F);
		assertThat(withTwo.finalArrivalLinks).containsExactly(DESTINATION);
		assertThat(withTwo.stuck).isEmpty();
		assertThat(drtLeg(withTwo.person).getRoute().getEndLinkId()).isEqualTo(STOP_E);
	}

	private static Leg drtLeg(Person person) {
		return TripStructureUtils.getLegs(person.getSelectedPlan())
				.stream()
				.filter(leg -> leg.getMode().equals("drt"))
				.findFirst()
				.orElseThrow();
	}

	private record Result(List<PersonDepartureEvent> drtDepartures, List<Id<Link>> drtArrivalLinks,
						  List<Id<Link>> finalArrivalLinks, List<Double> originActivityEnds, List<?> stuck,
						  Person person, boolean rejected) {
	}

	private static org.assertj.core.data.Offset<Double> offset(double value) {
		return org.assertj.core.data.Offset.offset(value);
	}

	private Result runPickup(int maxStopCandidates, double submissionTime) {
		return runPickup(maxStopCandidates, submissionTime, new PrebookingParams(), false);
	}

	private Result runPickup(int maxStopCandidates, double submissionTime, PrebookingParams params,
			boolean shortMaxWait) {
		PrebookingTestEnvironment environment = pickupEnvironment(maxStopCandidates, submissionTime, shortMaxWait);
		Controler controller = environment.build();
		PrebookingTest.installPrebooking(controller, params);
		return run(environment, controller);
	}

	private PrebookingTestEnvironment pickupEnvironment(int maxStopCandidates, double submissionTime,
			boolean shortMaxWait) {
		return new PrebookingTestEnvironment(utils) //
				.speed(SLOW_SPEED) //
				.useExactTravelTimeEstimates() //
				.addVehicle("vehicleA", 3, 8) //
				.addStop(3, 5) // A
				.addStop(3, 6) // B
				.addStop(3, 9) // E
				.stopCandidates(500, maxStopCandidates) //
				.addRequest("personA", 3, 5, 3, 9, DEPARTURE_TIME, submissionTime) //
				.configure(shortMaxWait ? 60.0 : 600.0, 1.3, 600.0, 60.0) //
				.endTime(12.0 * 3600.0);
	}

	private Result runDropoff(int maxStopCandidates) {
		return run(new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 3, 4) //
				.addStop(3, 5) // A
				.addStop(3, 9) // E
				.addStop(4, 8) // F
				.stopCandidates(500, maxStopCandidates) //
				.addRequest("personA", 3, 5, 3, 9, DEPARTURE_TIME, 0.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(12.0 * 3600.0));
	}

	private Result run(PrebookingTestEnvironment environment) {
		Controler controller = environment.build();
		PrebookingTest.installPrebooking(controller);
		return run(environment, controller);
	}

	private Result run(PrebookingTestEnvironment environment, Controler controller) {
		Handler handler = new Handler(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(handler);
			}
		});
		controller.run();

		Person person = controller.getScenario().getPopulation().getPersons().get(Id.createPersonId("personA"));
		return new Result(handler.drtDepartures, handler.drtArrivalLinks, handler.finalArrivalLinks,
				handler.originActivityEnds, environment.getStuckInfo(), person,
				environment.getRequestInfo().get("personA").rejected);
	}

	private record Handler(List<PersonDepartureEvent> drtDepartures, List<Id<Link>> drtArrivalLinks,
						   List<Id<Link>> finalArrivalLinks, List<Double> originActivityEnds)
			implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {
		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (event.getPersonId().equals(Id.createPersonId("personA")) && event.getActType().equals("generic")) {
				originActivityEnds.add(event.getTime());
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals("drt")) {
				drtDepartures.add(event);
			}
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLegMode().equals("drt")) {
				drtArrivalLinks.add(event.getLinkId());
			} else if (event.getLegMode().equals(TransportMode.walk) && event.getLinkId().equals(DESTINATION)) {
				finalArrivalLinks.add(event.getLinkId());
			}
		}
	}
}
