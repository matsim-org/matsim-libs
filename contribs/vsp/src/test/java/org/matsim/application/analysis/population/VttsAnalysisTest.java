package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.application.analysis.population.VttsAttributes.*;
import static org.matsim.application.analysis.population.kn.KNTestFixture.*;

/** Uses the hand-computable KN plans to check the new attribute-based single-run calculation. */
class VttsAnalysisTest {

	private static final double TOLERANCE = 1e-6;

	@Test
	void attachesActivityAndPersonValuesWithCorrectSignsAndUnits() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		var plan = person.getSelectedPlan();
		var analysis = new VttsAnalysis(muse(scenario), parameters(2, -6), new DefaultAnalysisMainModeIdentifier());

		analysis.analyze(person, plan);

		double meanMuse = (marginal(8, 8) + marginal(16, 14)) / 2;
		assertEquals(meanMuse, getDouble(person, MUSE), TOLERANCE);
		assertEquals(meanMuse / 2, getDouble(person, VSE), TOLERANCE);
		assertEquals((meanMuse + 6) / 2, getDouble(person, VTTS), TOLERANCE);
		assertEquals(getDouble(person, VTTS), getDouble(plan, VTTS));
		assertEquals(2, person.getAttributes().getAttribute(DESTINATIONS));
		assertEquals(2, person.getAttributes().getAttribute(VTTS_DESTINATIONS));
		assertEquals("ok", person.getAttributes().getAttribute(STATUS));
		var destination = TripStructureUtils.getTrips(plan).getFirst().getDestinationActivity();
		assertEquals("car", destination.getAttributes().getAttribute(MODE));
		assertEquals(-6, getDouble(destination, TRAVEL_UTILITY), TOLERANCE);
		assertEquals((marginal(8, 8) + 6) / 2, getDouble(destination, VTTS), TOLERANCE);
	}

	@Test
	void invalidMoneyUtilityRetainsMuseButLeavesMonetaryResultsUndefined() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		new VttsAnalysis(muse(scenario), parameters(0, -6), new DefaultAnalysisMainModeIdentifier())
			.analyze(person, person.getSelectedPlan());

		assertTrue(Double.isFinite(getDouble(person, MUSE)));
		assertTrue(Double.isNaN(getDouble(person, VTTS)));
		assertTrue(Double.isNaN(getDouble(person, VSE)));
		assertEquals("invalid_money_utility", person.getAttributes().getAttribute(STATUS));
	}

	@Test
	void unsupportedTravelConversionRetainsVseAndReportsCoverage() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		new VttsAnalysis(muse(scenario), parameters(2, Double.NaN), new DefaultAnalysisMainModeIdentifier())
			.analyze(person, person.getSelectedPlan());

		assertTrue(Double.isFinite(getDouble(person, VSE)));
		assertTrue(Double.isNaN(getDouble(person, VTTS)));
		assertEquals(0, person.getAttributes().getAttribute(VTTS_DESTINATIONS));
		assertEquals("partial_vtts", person.getAttributes().getAttribute(STATUS));
	}

	@Test
	void usesTheInjectedAnalysisModeRatherThanTheRequestedRoutingMode() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		((Leg) person.getSelectedPlan().getPlanElements().get(1)).setRoutingMode("pt");
		new VttsAnalysis(muse(scenario), parameters(2, -6), elements -> "car")
			.analyze(person, person.getSelectedPlan());

		var destination = TripStructureUtils.getTrips(person.getSelectedPlan()).getFirst().getDestinationActivity();
		assertEquals("car", destination.getAttributes().getAttribute(MODE));
	}

	@Test
	void missingAndIncompletePlansDoNotLeaveStalePersonValues() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		var analysis = new VttsAnalysis(muse(scenario), parameters(2, -6), new DefaultAnalysisMainModeIdentifier());
		analysis.analyze(person, person.getSelectedPlan());
		analysis.analyze(person, null);
		assertTrue(Double.isNaN(getDouble(person, VTTS)));
		assertEquals("missing_plan", person.getAttributes().getAttribute(STATUS));

		((Activity) person.getSelectedPlan().getPlanElements().get(2)).setStartTimeUndefined();
		analysis.analyze(person, person.getSelectedPlan());
		assertTrue(Double.isNaN(getDouble(person, MUSE)));
		assertEquals("incomplete_plan", person.getAttributes().getAttribute(STATUS));
	}

	@Test
	void defaultConversionUsesThePersonsSubpopulationParameters() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		var scoring = scenario.getConfig().scoring().getOrCreateScoringParameters("custom");
		scoring.setMarginalUtilityOfMoney(4);
		scoring.getOrCreateModeParams("car").setMarginalUtilityOfTraveling(-12);
		// Subpopulation sets do not inherit default mode entries. The core scoring builder
		// also needs PT parameters to derive waiting utility, even for this car-only fixture.
		scoring.getOrCreateModeParams("pt");
		PopulationUtils.putSubpopulation(person, "custom");
		var parameters = new PersonScoringVttsParameters(new SubpopulationScoringParameters(scenario));

		assertEquals(4, parameters.marginalUtilityOfMoney(person));
		assertEquals(-12, parameters.marginalUtilityOfTraveling(person, "car"));
		assertTrue(Double.isNaN(parameters.marginalUtilityOfTraveling(person, "missing")));
	}

	static VttsParameters parameters(double moneyUtility, double travelUtilityPerHour) {
		return new VttsParameters() {
			@Override public double marginalUtilityOfMoney(Person person) { return moneyUtility; }
			@Override public double marginalUtilityOfTraveling(Person person, String mode) { return travelUtilityPerHour; }
		};
	}
}
