package org.matsim.contrib.msp.capability.structure;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the reduction of a plan to its mandatory activities.
 */
public class FundamentalPlanExtractorTest {

	@Test
	void removesDiscretionaryActivities() {

		// home --car--> shop --car--> work --walk--> leisure --car--> home
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home");
		addLeg(plan, TransportMode.car);
		addActivity(plan, "shop");
		addLeg(plan, TransportMode.car);
		addActivity(plan, "work");
		addLeg(plan, TransportMode.walk);
		addActivity(plan, "leisure");
		addLeg(plan, TransportMode.car);
		addActivity(plan, "home");

		FundamentalPlanExtractor extractor = extractor("home", "work");
		Plan fundamentalPlan = extractor.extract(plan);

		assertEquals(List.of("home", "work", "home"), activityTypes(fundamentalPlan));
		assertEquals(2, extractor.countDiscretionaryActivities(plan), "shop and leisure are discretionary");

		// the two trips home->shop->work collapse into one car leg, work->leisure->home into another
		assertEquals(List.of(TransportMode.car, TransportMode.car), legModes(fundamentalPlan));

		// the original plan must not be touched
		assertEquals(5, TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities).size());
	}

	@Test
	void keepsPlanWithoutDiscretionaryActivitiesAsIs() {

		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home");
		addLeg(plan, TransportMode.pt);
		addActivity(plan, "work");

		FundamentalPlanExtractor extractor = extractor("home", "work");
		Plan fundamentalPlan = extractor.extract(plan);

		assertEquals(List.of("home", "work"), activityTypes(fundamentalPlan));
		assertEquals(List.of(TransportMode.pt), legModes(fundamentalPlan));
		assertEquals(0, extractor.countDiscretionaryActivities(plan));
	}

	@Test
	void returnsEmptyPlanIfNoActivityIsMandatory() {

		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "shop");
		addLeg(plan, TransportMode.bike);
		addActivity(plan, "leisure");

		FundamentalPlanExtractor extractor = extractor("home", "work");
		Plan fundamentalPlan = extractor.extract(plan);

		assertTrue(fundamentalPlan.getPlanElements().isEmpty());
		assertEquals(2, extractor.countDiscretionaryActivities(plan));
	}

	@Test
	void singleMandatoryActivityHasNoLegs() {

		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home");
		addLeg(plan, TransportMode.car);
		addActivity(plan, "shop");
		addLeg(plan, TransportMode.car);
		addActivity(plan, "leisure");

		Plan fundamentalPlan = extractor("home").extract(plan);

		assertEquals(List.of("home"), activityTypes(fundamentalPlan));
		assertTrue(legModes(fundamentalPlan).isEmpty());
	}

	@Test
	void ignoresStageActivities() {

		// a pt trip with its interaction activities, as a router would create it
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home");
		addLeg(plan, TransportMode.walk);
		addActivity(plan, "pt interaction");
		addLeg(plan, TransportMode.pt);
		addActivity(plan, "pt interaction");
		addLeg(plan, TransportMode.walk);
		addActivity(plan, "work");

		FundamentalPlanExtractor extractor = extractor("home", "work");
		Plan fundamentalPlan = extractor.extract(plan);

		assertEquals(List.of("home", "work"), activityTypes(fundamentalPlan));
		assertEquals(List.of(TransportMode.pt), legModes(fundamentalPlan), "pt is the main mode of the trip");
		assertEquals(0, extractor.countDiscretionaryActivities(plan), "stage activities are not discretionary activities");
	}

	@Test
	void keepsOriginalTimesOfMandatoryActivities() {

		// home (ends 8:00) --car 10min--> shop (ends 9:00) --car 20min--> work (ends 17:00)
		//   --car 15min--> leisure (ends 19:00) --car 30min--> home
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home", 8 * 3600);
		addLeg(plan, TransportMode.car, 8 * 3600, 600);
		addActivity(plan, "shop", 9 * 3600);
		addLeg(plan, TransportMode.car, 9 * 3600, 1200);
		addActivity(plan, "work", 17 * 3600);
		addLeg(plan, TransportMode.car, 17 * 3600, 900);
		addActivity(plan, "leisure", 19 * 3600);
		addLeg(plan, TransportMode.car, 19 * 3600, 1800);
		addActivity(plan, "home");

		Plan fundamentalPlan = extractor("home", "work").extract(plan);

		assertEquals(List.of("home", "work", "home"), activityTypes(fundamentalPlan));

		// the first home starts at 00:00, work when the agent arrived there (9:00 + 20min), the last
		// home when it got back there (19:00 + 30min)
		assertEquals(List.of(0.0, 9 * 3600 + 1200.0, 19 * 3600 + 1800.0), startTimes(fundamentalPlan));

		// the end times are the ones of the original plan, so a gap of 9:00-8:00 opens up before work
		// and one of 19:00-17:00 after it
		assertEquals(List.of(8 * 3600.0, 17 * 3600.0, Double.NaN), endTimes(fundamentalPlan));
	}

	@Test
	void prefersStartTimeGivenInThePlan() {

		// an experienced plan carries the start times itself, they win over anything reconstructed
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home", 8 * 3600);
		addLeg(plan, TransportMode.car, 8 * 3600, 600);
		Activity work = addActivity(plan, "work", 17 * 3600);
		work.setStartTime(8 * 3600 + 1500);

		Plan fundamentalPlan = extractor("home", "work").extract(plan);

		assertEquals(List.of(0.0, 8 * 3600 + 1500.0), startTimes(fundamentalPlan));
	}

	@Test
	void reconstructsStartTimesWithoutDepartureTimes() {

		// an unrouted plan has travel times at best, so the times have to be accumulated along the plan
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home", 8 * 3600);
		addLeg(plan, TransportMode.car).setTravelTime(600);
		Activity shop = addActivity(plan, "shop");
		shop.setMaximumDuration(1800);
		addLeg(plan, TransportMode.car).setTravelTime(1200);
		addActivity(plan, "work", 17 * 3600);

		Plan fundamentalPlan = extractor("home", "work").extract(plan);

		// 8:00 + 10min travel + 30min shopping + 20min travel
		assertEquals(List.of(0.0, 8 * 3600 + 600 + 1800 + 1200.0), startTimes(fundamentalPlan));
	}

	@Test
	void fallsBackToTheDepartureTimeWhenTravelTimesAreMissing() {

		// an output plans file whose routes were stripped: the legs say when the agent left but not how
		// long it travelled, so the arrival time can only be bounded from below
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home", 8 * 3600);
		Leg leg = addLeg(plan, TransportMode.car);
		leg.setDepartureTime(8 * 3600);
		addActivity(plan, "work", 17 * 3600);

		Plan fundamentalPlan = extractor("home", "work").extract(plan);

		assertEquals(List.of(0.0, 8 * 3600.0), startTimes(fundamentalPlan),
			"without a travel time, the departure of the last leg of the trip is the best available estimate");
	}

	@Test
	void prefersTheExactArrivalOverTheDepartureFallback() {

		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home", 8 * 3600);
		addLeg(plan, TransportMode.car, 8 * 3600, 900);
		addActivity(plan, "work", 17 * 3600);

		assertEquals(List.of(0.0, 8 * 3600 + 900.0), startTimes(extractor("home", "work").extract(plan)),
			"a leg that carries a travel time gives the exact arrival");
	}

	@Test
	void leavesStartTimeUndefinedIfItCannotBeReconstructed() {

		// no times at all in the plan
		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home");
		addLeg(plan, TransportMode.car);
		addActivity(plan, "work");

		Plan fundamentalPlan = extractor("home", "work").extract(plan);

		assertEquals(0, ((Activity) fundamentalPlan.getPlanElements().get(0)).getStartTime().seconds(),
			"the first activity always starts at 00:00");
		assertTrue(((Activity) fundamentalPlan.getPlanElements().get(2)).getStartTime().isUndefined());
	}

	@Test
	void startTimesSkipStageActivities() {

		Plan plan = PopulationUtils.createPlan();
		addActivity(plan, "home", 8 * 3600);
		addLeg(plan, TransportMode.walk, 8 * 3600, 300);
		addActivity(plan, "pt interaction");
		addLeg(plan, TransportMode.pt, 8 * 3600 + 300, 1500);
		addActivity(plan, "pt interaction");
		addLeg(plan, TransportMode.walk, 8 * 3600 + 1800, 300);
		addActivity(plan, "work", 17 * 3600);

		// one entry per non-stage activity, the walk egress leg is what the agent arrives with
		assertEquals(List.of(OptionalTime.zeroSeconds(), OptionalTime.defined(8 * 3600 + 2100)),
			FundamentalPlanExtractor.startTimes(plan));
	}

	private static FundamentalPlanExtractor extractor(String... mandatoryActivities) {
		CapabilityConfigGroup config = new CapabilityConfigGroup();
		config.setMandatoryActivities(Set.of(mandatoryActivities));
		return new FundamentalPlanExtractor(config);
	}

	private static Activity addActivity(Plan plan, String type) {
		Activity activity = PopulationUtils.createActivityFromCoord(type, new Coord(0, 0));
		plan.addActivity(activity);
		return activity;
	}

	/**
	 * Adds an activity the way a plans file has it: with an end time, but without a start time.
	 */
	private static Activity addActivity(Plan plan, String type, double endTime) {
		Activity activity = addActivity(plan, type);
		activity.setEndTime(endTime);
		return activity;
	}

	private static Leg addLeg(Plan plan, String mode) {
		Leg leg = PopulationUtils.createLeg(mode);
		plan.addLeg(leg);
		return leg;
	}

	/**
	 * Adds a leg the way a routed plan has it: with a departure and a travel time.
	 */
	private static Leg addLeg(Plan plan, String mode, double departureTime, double travelTime) {
		Leg leg = addLeg(plan, mode);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(travelTime);
		return leg;
	}

	private static List<Double> startTimes(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(element -> ((Activity) element).getStartTime().orElse(Double.NaN))
			.toList();
	}

	private static List<Double> endTimes(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(element -> ((Activity) element).getEndTime().orElse(Double.NaN))
			.toList();
	}

	private static List<String> activityTypes(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(element -> ((Activity) element).getType())
			.toList();
	}

	private static List<String> legModes(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Leg.class::isInstance)
			.map(element -> ((Leg) element).getMode())
			.toList();
	}
}
