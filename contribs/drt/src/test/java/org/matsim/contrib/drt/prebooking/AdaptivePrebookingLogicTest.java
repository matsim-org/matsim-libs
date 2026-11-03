package org.matsim.contrib.drt.prebooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.prebooking.logic.AdaptivePrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests for {@link AdaptivePrebookingLogic}. The logic is triggered by activity starts, so the person needs a
 * second DRT trip: the first activity of a plan never emits an ActivityStartEvent.
 */
public class AdaptivePrebookingLogicTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private static final double FIRST_DEPARTURE = 1000.0;
	private static final double SECOND_DEPARTURE = 5000.0;

	@Test
	void bookingIsSubmittedInAdvance() {
		/*-
		 * The second trip departs at the end of the intermediate activity. With a submission slack of 1800s, it should
		 * be submitted 1800s before the departure, while the agent is still performing the activity.
		 */
		double slack = 1800.0;
		RequestInfo requestInfo = runWithSecondTrip(slack);

		List<Double> submissionTimes = requestInfo.submissionTimes;
		assertEquals(2, submissionTimes.size());

		// first trip: no activity start before it, so it is submitted upon departure
		assertEquals(FIRST_DEPARTURE, submissionTimes.get(0), 1e-3);

		// second trip: prebooked with the configured slack
		assertEquals(SECOND_DEPARTURE - slack, submissionTimes.get(1), 1e-3);
		assertFalse(requestInfo.rejected);
	}

	@Test
	void departureEstimateIncludesActivityDuration() {
		/*-
		 * With a small slack, the submission must happen shortly before the end of the intermediate activity, not
		 * shortly after its start.
		 */
		double slack = 60.0;
		RequestInfo requestInfo = runWithSecondTrip(slack);

		List<Double> submissionTimes = requestInfo.submissionTimes;
		assertEquals(2, submissionTimes.size());
		assertEquals(SECOND_DEPARTURE - slack, submissionTimes.get(1), 1e-3);
	}

	@Test
	void slackLongerThanActivityIsCappedAtActivityStart() {
		/*-
		 * If the slack is longer than the activity, the booking is submitted when the activity starts.
		 */
		double slack = 24.0 * 3600.0;
		RequestInfo requestInfo = runWithSecondTrip(slack);

		List<Double> submissionTimes = requestInfo.submissionTimes;
		assertEquals(2, submissionTimes.size());

		// submitted at the start of the intermediate activity, i.e. after the first trip, but before the departure
		assertTrue(submissionTimes.get(1) > FIRST_DEPARTURE);
		assertTrue(submissionTimes.get(1) < SECOND_DEPARTURE);
	}

	private RequestInfo runWithSecondTrip(double slack) {
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, FIRST_DEPARTURE) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		addReturnTrip(controller);

		PrebookingTest.installPrebooking(controller, false);
		AdaptivePrebookingLogic.install(controller, DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig()),
				slack);

		controller.run();

		return environment.getRequestInfo().get("personA");
	}

	private static void addReturnTrip(Controler controller) {
		Person person = controller.getScenario().getPopulation().getPersons().get(Id.createPersonId("personA"));
		Plan plan = person.getSelectedPlan();
		PopulationFactory factory = controller.getScenario().getPopulation().getFactory();

		Activity origin = (Activity) plan.getPlanElements().getFirst();
		Activity intermediate = (Activity) plan.getPlanElements().getLast();
		intermediate.setEndTime(SECOND_DEPARTURE);

		plan.addLeg(factory.createLeg("drt"));
		plan.addActivity(factory.createActivityFromLinkId("generic", origin.getLinkId()));
	}
}
