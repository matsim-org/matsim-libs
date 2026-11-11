package org.matsim.application.analysis.population.kn;

import org.matsim.application.analysis.population.MuseComputation;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * Shared setup for tests of the existing KN base-case analysis.
 *
 * <p>The plans already contain experienced start/end times, leg travel times, and route distances.
 * They are constructed directly: no controller, routing, or traffic simulation is run. This keeps
 * the expected MUSE values independent of congestion and lets us calculate them analytically.
 * The event-reading test writes its own small event file separately.
 */
public final class KNTestFixture {

	public static final double HOUR = 3600;
	public static final Id<Link> LINK = Id.createLinkId("link");

	private KNTestFixture() {
	}

	public static Scenario scenario() {
		Config config = ConfigUtils.createConfig();
		// All duration-based expectations use beta_performing = 6 score/hour. The base money
		// utility is 1 score/money unit before the KN income-dependent adjustment is applied.
		config.scoring().setDefaultPerforming_utils_hr(6);
		config.scoring().setDefaultMarginalUtilityOfMoney(1);
		// The typical duration controls the slope of logarithmic activity utility. Actual
		// durations are supplied by person(...), and may differ from these typical durations.
		config.scoring().addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(16 * HOUR));
		config.scoring().addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * HOUR));
		config.scoring().addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("shop").setTypicalDuration(HOUR));
		// Same parameters as home, but a different type: this isolates the first/last activity
		// type-matching rule in the test that disables overnight wrapping.
		config.scoring().addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("other_home").setTypicalDuration(16 * HOUR));
		// Deliberately nonzero travel utility and charges make it possible to distinguish
		// activity-only MUSE from the separate travel and money columns in the KN person table.
		config.scoring().getOrCreateDefaultModeParams("car")
			.setMarginalUtilityOfTraveling(-6)
			.setMonetaryDistanceRate(-0.001)
			.setDailyMonetaryConstant(-2)
			.setConstant(0);
		Scenario scenario = ScenarioUtils.createScenario(config);
		// The link supplies valid references for activities, routes, and synthetic events.
		// Its geometry does not determine travel time here: the fixture sets that explicitly.
		var network = scenario.getNetwork();
		var from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
		var to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(1000, 0));
		network.addNode(from);
		network.addNode(to);
		Link link = network.getFactory().createLink(LINK, from, to);
		link.setLength(1000);
		network.addLink(link);
		return scenario;
	}

	/**
	 * Adds a person with a home-work-home or home-work-shop-home experienced plan.
	 *
	 * <p>Home ends at 08:00; work runs from 09:00 to 17:00. Without shopping, home starts
	 * again at 18:00. With shopping, shop runs from 18:00 to 19:00 and home starts at 20:00.
	 * Every trip takes one hour. The first start and final end are undefined, as expected
	 * for the morning/evening activity pair. Combined home duration is therefore 14 or 12 hours.
	 *
	 * @param income used by tests of the KN population-average/personal-income normalization
	 */
	public static Person person(Scenario scenario, String id, double income, boolean shopping) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId(id));
		PersonUtils.setIncome(person, income);
		// The current KN cleanup keeps only subpopulation "person". The shape flag supplies
		// the person-table field directly; these tests do not load a geographical boundary.
		PopulationUtils.putSubpopulation(person, "person");
		AgentWiseComparisonKNUtils.setIsInShp(person, "true");
		Plan plan = factory.createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		// A sentinel for the stored simulation score. MUSE may add result attributes, but
		// must not overwrite this score or replace the selected plan while perturbing times.
		plan.setScore(0.);
		scenario.getPopulation().addPerson(person);
		activity(plan, "home", Double.NaN, 8 * HOUR);
		leg(plan);
		activity(plan, "work", 9 * HOUR, 17 * HOUR);
		leg(plan);
		if (shopping) {
			activity(plan, "shop", 18 * HOUR, 19 * HOUR);
			leg(plan);
		}
		activity(plan, "home", (shopping ? 20 : 18) * HOUR, Double.NaN);
		return person;
	}

	public static Activity activity(Plan plan, String type, double start, double end) {
		Activity activity = PopulationUtils.createAndAddActivityFromLinkId(plan, type, LINK);
		// NaN is only a fixture argument meaning "leave undefined"; it is not assigned as
		// an activity time. This preserves MATSim's special first/last activity semantics.
		if (!Double.isNaN(start)) activity.setStartTime(start);
		if (!Double.isNaN(end)) activity.setEndTime(end);
		return activity;
	}

	static void leg(Plan plan) {
		// Explicitly supply the fields read by generatePersonTableFromPopulation(...).
		// Each leg contributes 1 hour of travel and 1000 m of distance-based charges.
		Leg leg = PopulationUtils.createAndAddLeg(plan, "car");
		leg.setDepartureTime(((Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 2)).getEndTime().seconds());
		leg.setTravelTime(HOUR);
		var route = RouteUtils.createGenericRouteImpl(LINK, LINK);
		route.setDistance(1000);
		route.setTravelTime(HOUR);
		leg.setRoute(route);
	}

	public static MuseComputation muse(Scenario scenario) {
		// Use real standard activity scoring for the numerical MUSE tests. The KN base-case
		// tests separately exercise the full injector with its income-dependent provider.
		return muse(scenario, new CharyparNagelScoringFunctionFactory(scenario));
	}

	public static MuseComputation muse(Scenario scenario, ScoringFunctionFactory factory) {
		// Bind both original MUSE dependencies. Injection tests can substitute a person-specific
		// scoring factory; activity copies use the scenario's population factory.
		return Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(ScoringFunctionFactory.class).toInstance(factory);
				bind(PopulationFactory.class).toInstance(scenario.getPopulation().getFactory());
			}
		}).getInstance(MuseComputation.class);
	}

	/**
	 * Independent expected MUSE for the logarithmic branch of activity scoring used in this fixture.
	 *
	 * <p>Starting one second earlier, with the end fixed, changes duration from d to d+1.
	 * In score/hour, the exact finite difference is beta_performing * t_typ * ln(1 + 1/d),
	 * with t_typ and d in seconds and beta_performing = 6 score/hour. Constant terms in
	 * activity utility cancel. log1p retains precision for the small one-second change.
	 * This is the finite difference evaluated by KN, rather than its infinitesimal derivative.
	 */
	public static double marginal(double typicalHours, double actualHours) {
		return 6 * typicalHours * HOUR * Math.log1p(1 / (actualHours * HOUR));
	}
}
