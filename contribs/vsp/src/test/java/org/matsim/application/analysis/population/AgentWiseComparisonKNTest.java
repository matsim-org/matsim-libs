package org.matsim.application.analysis.population;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.*;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.application.analysis.population.KNTestFixture.*;

/**
 * Characterizes the existing KN base-case helpers and person-table calculation.
 *
 * <p>The fixture supplies experienced plans directly, and the table tests use the same injector
 * construction as the production base case. They cover population cleanup, income utilities,
 * aggregation, and selected score-accounting columns. They do not run the CLI, load complete run
 * directories, simulate traffic, or execute policy comparisons and rule-of-half calculations.
 */
class AgentWiseComparisonKNTest {

	private static final double TOLERANCE = 1e-6;
	// Synthetic events and any injector output stay in JUnit-managed temporary storage.
	@TempDir Path directory;
	// The existing implementation stores its population average in a mutable static field.
	// Save and restore it so these tests do not change the initial state of later tests.
	private final double previousPopulationMarginal = MUTTS_AV;

	@AfterEach
	void restorePopulationMarginal() {
		MUTTS_AV = previousPopulationMarginal;
	}

	/** Exercises the real cleanup/event-reading path with one example of each exclusion rule. */
	@Test
	void cleansBasePopulationAndCollectsMoneyEvents() {
		Scenario scenario = scenario();
		var retained = person(scenario, "retained", 2000, false);
		var stuck = person(scenario, "stuck", 2000, false);
		var freight = person(scenario, "freight", 2000, false);
		org.matsim.core.population.PopulationUtils.putSubpopulation(freight, "freight");
		// Remove everything after the initial home activity to create a person without trips.
		var stationary = person(scenario, "stationary", 2000, false);
		var stationaryElements = stationary.getSelectedPlan().getPlanElements();
		stationaryElements.subList(1, stationaryElements.size()).clear();
		// Write actual MATSim events and let the production reader process them. This checks
		// accumulation across multiple payments as well as removal triggered by a stuck event.
		EventWriterXML events = new EventWriterXML(directory.resolve("events.xml.gz").toString());
		try {
			events.handleEvent(new PersonMoneyEvent(HOUR, retained.getId(), -0.5, "test", null, null));
			events.handleEvent(new PersonMoneyEvent(2 * HOUR, retained.getId(), -0.25, "test", null, null));
			events.handleEvent(new PersonStuckEvent(3 * HOUR, stuck.getId(), LINK, "car"));
		} finally {
			events.closeFile();
		}

		cleanPopulation(directory, List.of("events.xml.gz"), scenario.getPopulation());

		// Only the mobile, non-stuck human remains; both payments must be attached to that person.
		assertEquals(1, scenario.getPopulation().getPersons().size());
		assertSame(retained, scenario.getPopulation().getPersons().get(retained.getId()));
		assertEquals(-0.75, (Double) retained.getAttributes().getAttribute(AgentWiseComparisonKN.KN_MONEY), TOLERANCE);
	}

	/** Identical schedules must have identical MUSE, while income changes monetary utility. */
	@Test
	void baseCaseProducesActivityMeansAndIncomeDependentMoneyUtilities() {
		Scenario scenario = scenario();
		var lowIncome = person(scenario, "low", 1000, false);
		var highIncome = person(scenario, "high", 3000, false);
		lowIncome.getAttributes().putAttribute(AgentWiseComparisonKN.KN_MONEY, -3.);

		Table result = baseCase(scenario);

		assertEquals(2, result.rowCount());
		Table low = row(result, "low");
		Table high = row(result, "high");
		// Both plans have the same work and overnight home durations, hence the same mean MUSE.
		double meanMuse = (marginal(8, 8) + marginal(16, 14)) / 2;
		assertEquals(meanMuse, low.doubleColumn(MUSE_h).getDouble(0), TOLERANCE);
		assertEquals(meanMuse, high.doubleColumn(MUSE_h).getDouble(0), TOLERANCE);
		// Average income is 2000. KN uses base utility * average income / personal income,
		// giving 2 for income 1000 and 2/3 for income 3000. Check both the table and attributes.
		assertEquals(2., low.doubleColumn(UTL_OF_MONEY).getDouble(0), TOLERANCE);
		assertEquals(2. / 3, high.doubleColumn(UTL_OF_MONEY).getDouble(0), TOLERANCE);
		assertEquals(2., PersonUtils.getMarginalUtilityOfMoney(lowIncome), TOLERANCE);
		assertEquals(2. / 3, PersonUtils.getMarginalUtilityOfMoney(highIncome), TOLERANCE);
		// Two one-hour legs give 2 hours and 2 * (-6) = -12 score of direct travel utility.
		assertEquals(2., low.doubleColumn(TTIME).getDouble(0), TOLERANCE);
		assertEquals(-12., low.doubleColumn(U_TRAV_DIRECT).getDouble(0), TOLERANCE);
		// Each route costs 1000 * 0.001 = 1 money unit. Two routes plus a once-per-day car
		// charge of 2 give -4; the low-income person's extra payment of 3 gives -7.
		// Monetary score multiplies those amounts by the person's utility of money.
		assertEquals(-7., low.doubleColumn(MONEY).getDouble(0), TOLERANCE);
		assertEquals(-14., low.doubleColumn(MONEY_SCORE).getDouble(0), TOLERANCE);
		assertEquals(-4., high.doubleColumn(MONEY).getDouble(0), TOLERANCE);
		assertEquals(-8. / 3, high.doubleColumn(MONEY_SCORE).getDouble(0), TOLERANCE);
		// These strings confirm that the expected trip/activity sequence reached the base table.
		assertEquals("car--car", low.stringColumn(MODE_SEQ).getString(0));
		assertEquals("home|work|home", low.stringColumn(ACT_SEQ).getString(0));
	}

	/** Makes destination weighting distinguishable from averaging the two person means. */
	@Test
	void populationMarginalIsDestinationWeightedRatherThanPersonWeighted() {
		Scenario scenario = scenario();
		person(scenario, "two_trips", 2000, false);
		person(scenario, "three_trips", 2000, true);

		Table result = baseCase(scenario);

		// The first person has work + home destinations. Shopping adds a third destination
		// for the second person and postpones evening home to 20:00, leaving 12 home hours.
		double twoTripSum = marginal(8, 8) + marginal(16, 14);
		double threeTripSum = marginal(8, 8) + marginal(1, 1) + marginal(16, 12);
		assertEquals(twoTripSum / 2, row(result, "two_trips").doubleColumn(MUSE_h).getDouble(0), TOLERANCE);
		assertEquals(threeTripSum / 3, row(result, "three_trips").doubleColumn(MUSE_h).getDouble(0), TOLERANCE);
		// Despite its historical MUTTS_AV name, this field contains the population MUSE mean.
		// All five destination values receive equal weight; the two persons do not.
		assertEquals((twoTripSum + threeTripSum) / 5, MUTTS_AV, TOLERANCE);
		assertNotEquals((twoTripSum / 2 + threeTripSum / 3) / 2, MUTTS_AV, TOLERANCE);
	}

	/** With ten distinct incomes, each decile should contain exactly one person. */
	@Test
	void assignsIncomeDecilesInIncreasingIncomeOrder() {
		Scenario scenario = scenario();
		// Insert in reverse income order so the expected deciles require sorting by income.
		for (int i = 9; i >= 0; i--) person(scenario, "person_" + i, (i + 1) * 1000, false);

		computeAndSetIncomeDeciles(scenario.getPopulation());

		for (int i = 0; i < 10; i++) {
			var person = scenario.getPopulation().getPersons().get(org.matsim.api.core.v01.Id.createPersonId("person_" + i));
			assertEquals(i, getIncomeDecileBetween0And9(person));
		}
	}

	private Table baseCase(Scenario scenario) {
		scenario.getConfig().controller().setOutputDirectory(directory.resolve("run").toString());
		// Reproduce the relevant preparation steps from AgentWiseComparisonKN.call(). All
		// persons in these table tests have positive income, so both utility calculations apply.
		computeAndSetMarginalUtilitiesOfMoney(scenario);
		computeAndSetIncomeDeciles(scenario.getPopulation());
		var injector = createInjector(scenario);
		// The KN helper stores money utilities on persons, while the injected scoring provider
		// also derives them. Check agreement before building the table, which uses both paths.
		var parameters = injector.getInstance(ScoringParametersForPerson.class);
		for (var person : scenario.getPopulation().getPersons().values())
			assertEquals(PersonUtils.getMarginalUtilityOfMoney(person), parameters.getScoringParameters(person).marginalUtilityOfMoney, TOLERANCE);
		AgentWiseComparisonKN analysis = new AgentWiseComparisonKN();
		analysis.baseScenario = scenario;
		// The current method reads this field for MUSE, in addition to receiving an injector argument.
		analysis.baseCaseInjector = injector;
		// A null reference population selects the base-case branch; no policy case is evaluated.
		return analysis.generatePersonTableFromPopulation(injector, null);
	}

	private static Table row(Table table, String personId) {
		// Select by ID so assertions do not depend on population iteration or table row order.
		Table result = table.where(table.stringColumn(PERSON_ID).isEqualTo(personId));
		assertEquals(1, result.rowCount());
		return result;
	}
}
