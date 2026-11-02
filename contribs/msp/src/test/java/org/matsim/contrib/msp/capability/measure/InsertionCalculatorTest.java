package org.matsim.contrib.msp.capability.measure;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.msp.capability.DiscretionaryActivityParams;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.structure.FreePeriod;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.facilities.OpeningTimeImpl;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the core of the measure: whether a discretionary activity fits into a stretch of the day, how much
 * freedom the agent has in when to start it, and what the insertion is worth. Travel times are stubbed
 * so that every number here can be checked by hand.
 */
public class InsertionCalculatorTest {

	private static final double TO_SHOP = 600;    // 10 min to the shop from anywhere
	private static final double DIRECT = 300;     // 5 min for the trip the agent would have made anyway

	/**
	 * The default of the config group: 6 utils per hour of travel. Durations stay in seconds, so the
	 * expected costs below divide by 3600 - which is exactly what {@link InsertionCost#ofTime} does.
	 */
	private static final double VALUE_OF_TIME_PER_HOUR = 6.0;

	private final DiscretionaryActivityParams params = new DiscretionaryActivityParams("shop");
	private final InsertionCost cost = InsertionCost.ofTime(VALUE_OF_TIME_PER_HOUR);

	/**
	 * Travel times that do not depend on the network: 10 minutes to or from the shop, 5 minutes for
	 * anything else.
	 */
	private CapabilityTravelTimes travelTimes(DiscretionaryActivityFacility shop) {
		return (from, to, departureTime) -> (from == shop || to == shop) ? TO_SHOP : DIRECT;
	}

	private InsertionCalculator calculator(CapabilityTravelTimes travelTimes) {
		return new InsertionCalculator(travelTimes, this.params, this.cost);
	}

	@Test
	void computesSlackCostAndUtilityOfOneInsertion() {

		// the evening of a commuter: leaves work at 17:00, has until the end of the day to get home
		DiscretionaryActivityFacility shop = shop(8 * 3600, 21 * 3600);
		FreePeriod evening = new FreePeriod(place(), 17 * 3600, place(), FreePeriod.END_OF_DAY);

		InsertionCalculator.Insertion insertion = calculator(travelTimes(shop)).evaluate(evening, shop);

		assertTrue(insertion.possible());

		// arrives 17:10, has to leave by 23:50, but the shop shuts at 21:00
		// -> 3 h 50 min on site, of which 30 min are the minimum duration
		assertEquals(12000, insertion.slack(), "3 h 20 min of freedom in when to start");

		// the detour is there and back minus the trip that would have been made anyway
		assertEquals(TO_SHOP + TO_SHOP - DIRECT, insertion.extraTravelTime());
		assertEquals(900 * VALUE_OF_TIME_PER_HOUR / 3600, insertion.cost(), 1e-9,
			"15 min of detour at 6 utils per hour");

		// u_potential = u_0 + k * ln(1 + slack / t_typ) - c_insertion
		assertEquals(3 + Math.log(1 + 12000 / 3600.0) - 900 * VALUE_OF_TIME_PER_HOUR / 3600,
			insertion.utility(), 1e-9);
	}

	@Test
	void anActivityThatJustFitsIsWorthTheBaseValueMinusTheDetour() {

		DiscretionaryActivityFacility shop = shop(8 * 3600, 21 * 3600);

		// exactly 10 + 30 + 10 minutes
		FreePeriod exact = new FreePeriod(place(), 9 * 3600, place(), 9 * 3600 + 3000);

		InsertionCalculator.Insertion insertion = calculator(travelTimes(shop)).evaluate(exact, shop);

		assertTrue(insertion.possible());
		assertEquals(0, insertion.slack(), 1e-9, "it fits, but there is no freedom in when to go");

		// the point of the '1 +' in the logarithm: no slack is worth nothing extra, not minus infinity
		assertEquals(this.params.getBaseUtility() - 900 * VALUE_OF_TIME_PER_HOUR / 3600,
			insertion.utility(), 1e-9);
	}

	@Test
	void theFacilityIsWorthItsBestInsertionNotTheSumOfThem() {

		DiscretionaryActivityFacility shop = shop(8 * 3600, 21 * 3600);

		// two chances to go: a tight gap in the morning and the whole evening
		FreePeriod morning = new FreePeriod(place(), 6 * 3600, place(), 9 * 3600);
		FreePeriod evening = new FreePeriod(place(), 17 * 3600, place(), FreePeriod.END_OF_DAY);

		InsertionCalculator calculator = calculator(travelTimes(shop));
		InsertionCalculator.Totals totals = calculator.evaluate(List.of(morning, evening), shop);

		double morningSlack = calculator.evaluate(morning, shop).slack();
		double eveningSlack = calculator.evaluate(evening, shop).slack();
		assertTrue(eveningSlack > morningSlack);

		assertEquals(2, totals.insertions());
		assertEquals(1, totals.bestIndex(), "the evening is the better of the two");
		assertEquals(calculator.evaluate(evening, shop).utility(), totals.maxUtility(), 1e-9);

		// the summed slack is reported, but it is not what the utility is built from: no single trip
		// offers the freedom of both positions
		assertEquals(morningSlack + eveningSlack, totals.sumSlack(), 1e-9);
		assertTrue(totals.maxUtility() < this.params.utility(totals.sumSlack()) - totals.minCost(),
			"the utility of the summed slack would be higher than anything the person can realize");
	}

	@Test
	void theBestInsertionIsNotNecessarilyTheCheapestOne() {

		DiscretionaryActivityFacility shop = shop(8 * 3600, 21 * 3600);

		// the shop is 10 min away in the morning and 15 min away in the evening
		CapabilityTravelTimes travelTimes = (from, to, departureTime) -> {
			if (from != shop && to != shop) {
				return DIRECT;
			}
			return departureTime < 12 * 3600 ? 600 : 900;
		};

		// a gap that the activity only just fits into, and an evening with hours to spare
		FreePeriod tightMorning = new FreePeriod(place(), 9 * 3600, place(), 9 * 3600 + 3000);
		FreePeriod evening = new FreePeriod(place(), 17 * 3600, place(), FreePeriod.END_OF_DAY);

		InsertionCalculator.Totals totals =
			calculator(travelTimes).evaluate(List.of(tightMorning, evening), shop);

		assertEquals(2, totals.insertions());
		assertEquals(1, totals.bestIndex(), "the longer evening beats the cheaper morning");
		assertNotEquals(totals.minCost(), totals.best().cost(),
			"the best insertion costs more than the cheapest one, which is exactly why the two must not "
				+ "be combined into one utility");
		assertTrue(totals.best().cost() > totals.minCost());
	}

	@Test
	void doesNotFitWhenThereIsNotEnoughTime() {

		DiscretionaryActivityFacility shop = shop(8 * 3600, 21 * 3600);

		// 40 minutes between two fixed activities: 20 min of travel leaves 20 min on site, below the
		// 30 min minimum duration
		FreePeriod tightGap = new FreePeriod(place(), 9 * 3600, place(), 9 * 3600 + 2400);

		InsertionCalculator calculator = calculator(travelTimes(shop));

		assertFalse(calculator.evaluate(tightGap, shop).possible());
		assertFalse(calculator.evaluate(List.of(tightGap), shop).possible(), "so no row is written at all");
	}

	@Test
	void doesNotFitWhenTheShopIsShut() {

		// the shop shuts at 16:00, the agent is only free from 17:00
		DiscretionaryActivityFacility shop = shop(8 * 3600, 16 * 3600);
		FreePeriod evening = new FreePeriod(place(), 17 * 3600, place(), FreePeriod.END_OF_DAY);

		assertFalse(calculator(travelTimes(shop)).evaluate(evening, shop).possible());
	}

	@Test
	void prunesFacilitiesThatAreTooFarAway() {

		DiscretionaryActivityFacility shop = shop(8 * 3600, 21 * 3600);
		FreePeriod evening = new FreePeriod(place(), 17 * 3600, place(), FreePeriod.END_OF_DAY);

		// there and back is 2 h of detour, well past the 30 min the search allows
		CapabilityTravelTimes far = (from, to, time) -> (from == shop || to == shop) ? 3600 : DIRECT;

		assertFalse(calculator(far).evaluate(evening, shop).possible(),
			"the detour exceeds maxExtraTravelTime");
	}

	@Test
	void theRoomiestOpeningWindowIsTheOneUsed() {

		// a restaurant with a lunch and a dinner service, and an agent that is free all day
		DiscretionaryActivityFacility restaurant = new DiscretionaryActivityFacility(facility("gasthaus"), "shop", "German",
			List.of(new OpeningTimeImpl(11 * 3600, 14 * 3600), new OpeningTimeImpl(17 * 3600, 23 * 3600)));
		FreePeriod wholeDay = new FreePeriod(place(), 0, place(), FreePeriod.END_OF_DAY);

		InsertionCalculator.Insertion insertion =
			calculator(travelTimes(restaurant)).evaluate(wholeDay, restaurant);

		// the windows are alternatives, not something to add up: the agent eats once, and the 6 h dinner
		// service leaves more freedom than the 3 h lunch service
		assertEquals(6 * 3600 - 1800, insertion.slack(), 1e-9);
		assertEquals(17 * 3600, insertion.earliestStart(), 1e-9);
		assertEquals(23 * 3600 - 1800, insertion.latestStart(), 1e-9);
	}

	@Test
	void anAlwaysOpenPlaceIsBoundedOnlyByTheDay() {

		DiscretionaryActivityFacility park = DiscretionaryActivityFacility.create(facility("park"), "shop");
		FreePeriod wholeDay = new FreePeriod(place(), 0, place(), FreePeriod.END_OF_DAY);

		InsertionCalculator.Insertion insertion = calculator(travelTimes(park)).evaluate(wholeDay, park);

		// arrives at 00:10, has to be back by 23:50, minus the 30 min minimum duration
		assertEquals(FreePeriod.END_OF_DAY - TO_SHOP - TO_SHOP - 1800, insertion.slack(), 1e-9);
	}

	private static DiscretionaryActivityFacility shop(double opening, double closing) {
		return new DiscretionaryActivityFacility(facility("shop-1"), "shop", "Konsum",
			List.of(new OpeningTimeImpl(opening, closing)));
	}

	private static ActivityFacility facility(String id) {
		return ScenarioUtils.createScenario(ConfigUtils.createConfig()).getActivityFacilities().getFactory()
			.createActivityFacility(Id.create(id, ActivityFacility.class), new Coord(0, 0));
	}

	/**
	 * A stand-in for one of the person's own activities; only its identity matters here, since the travel
	 * times are stubbed.
	 */
	private static Facility place() {
		return new Facility() {
			@Override
			public Coord getCoord() {
				return new Coord(0, 0);
			}

			@Override
			public Id<Link> getLinkId() {
				return null;
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				return Map.of();
			}
		};
	}
}
