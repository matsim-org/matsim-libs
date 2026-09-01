package org.matsim.contrib.pseudosimulation.distributed.plans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.distributed.scoring.PlanScoreComponent;
import org.matsim.contrib.pseudosimulation.distributed.scoring.ScoreComponentType;
import org.matsim.core.population.PopulationUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanGenomeTest {

	private Person person;
	private PlanGenome plan;

	@BeforeEach
	void setUp() {
		person = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
		plan = new PlanGenome(person);
	}

	@Test
	void storesCorePlanAndGenomeState() {
		plan.setScore(4.5);
		plan.setType("type");
		plan.setGenome("A0001");
		plan.appendStrategyToGenome("B0002");
		plan.setpSimScore(3.25);
		plan.setPlanId(Id.create("ignored", org.matsim.api.core.v01.population.Plan.class));
		plan.setIterationCreated(9);
		plan.setPlanMutator("ignored");

		assertSame(person, plan.getPerson());
		assertEquals(4.5, plan.getScore());
		assertEquals("type", plan.getType());
		assertEquals("A0001B0002", plan.getGenome());
		assertEquals(3.25, plan.getpSimScore());
		assertNull(plan.getId());
		assertEquals(-1, plan.getIterationCreated());
		assertNull(plan.getPlanMutator());
		assertEquals("[score=4.5][nof_acts_legs=0][type=type][personId=person]", plan.toString());
	}

	@Test
	void defaultPlanReportsUndefinedStateAndProvidesMutableAttributeMap() {
		PlanGenome empty = new PlanGenome();

		assertEquals("[score=undefined][nof_acts_legs=0][type=null][personId=undefined]", empty.toString());
		empty.getCustomAttributes().put("legacy", 7);
		assertEquals(7, empty.getCustomAttributes().get("legacy"));
		empty.getAttributes().putAttribute("modern", "value");
		assertEquals("value", empty.getAttributes().getAttribute("modern"));
	}

	@Test
	void scoreComponentListsAreStoredByReferenceAndResetOnlyPrimaryList() {
		ArrayList<PlanScoreComponent> primary = new ArrayList<>();
		ArrayList<PlanScoreComponent> alternate = new ArrayList<>();
		plan.setScoreComponents(primary);
		plan.setAltScoreComponents(alternate);
		plan.addScoreComponent(ScoreComponentType.Leg, 2.0, "car");

		assertSame(primary, plan.getScoreComponents());
		assertEquals(1, primary.size());
		assertSame(alternate, plan.getAltScoreComponents());
		plan.resetScoreComponents();
		assertTrue(plan.getScoreComponents().isEmpty());
		assertFalse(plan.getScoreComponents() == primary);
		assertSame(alternate, plan.getAltScoreComponents());
	}

	@Test
	void deprecatedCreationHelpersEnforceActivityBeforeLeg() {
		IllegalStateException error = assertThrows(IllegalStateException.class,
				() -> plan.createAndAddLeg(TransportMode.car));
		assertEquals("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.",
				error.getMessage());

		Activity activity = plan.createAndAddActivity("home", new Coord(1, 2));
		Leg leg = plan.createAndAddLeg(TransportMode.car);
		Activity linkActivity = plan.createAndAddActivity("work", Id.createLinkId("link"));
		assertEquals(List.of(activity, leg, linkActivity), plan.getPlanElements());
	}

	@Test
	void navigationFindsAdjacentElementsAndExposesFirstActivityQuirk() {
		Activity first = PopulationUtils.createActivityFromCoord("first", new Coord(0, 0));
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
		Activity last = PopulationUtils.createActivityFromCoord("last", new Coord(1, 1));
		plan.addActivity(first);
		plan.addLeg(leg);
		plan.addActivity(last);

		assertSame(first, plan.getFirstActivity());
		assertSame(last, plan.getLastActivity());
		assertSame(first, plan.getPreviousActivity(leg));
		assertSame(last, plan.getNextActivity(leg));
		assertSame(leg, plan.getPreviousLeg(last));
		assertSame(leg, plan.getNextLeg(first));
		assertNull(plan.getNextLeg(last));
		assertNull(plan.getPreviousActivity(PopulationUtils.createLeg("missing")));
		assertThrows(IndexOutOfBoundsException.class, () -> plan.getPreviousLeg(first));
		assertThrows(IllegalArgumentException.class, () -> plan.getActLegIndex(new UnknownElement()));
	}

	@Test
	void insertLegActAcceptsEndAndRejectsActivityPositionOrGap() {
		Activity first = PopulationUtils.createActivityFromCoord("first", new Coord(0, 0));
		plan.addActivity(first);
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		Activity last = PopulationUtils.createActivityFromCoord("last", new Coord(1, 1));
		plan.insertLegAct(1, leg, last);
		assertEquals(List.of(first, leg, last), plan.getPlanElements());

		assertThrows(IllegalArgumentException.class,
				() -> plan.insertLegAct(0, PopulationUtils.createLeg("x"), last));
		assertThrows(IllegalArgumentException.class,
				() -> plan.insertLegAct(9, PopulationUtils.createLeg("x"), last));
	}

	@Test
	void removingFirstAndLastActivitiesAlsoRemovesAdjacentLeg() {
		populateFiveElements();
		plan.removeActivity(0);
		assertEquals(3, plan.getPlanElements().size());
		plan.removeActivity(2);
		assertEquals(1, plan.getPlanElements().size());
		plan.removeActivity(0);
		assertEquals(1, plan.getPlanElements().size());
		plan.removeActivity(-1);
		assertEquals(1, plan.getPlanElements().size());
	}

	@Test
	void removingMiddleActivityClearsPreviousLegAndDropsFollowingLeg() {
		populateFiveElements();
		Leg previous = (Leg) plan.getPlanElements().get(1);
		previous.setDepartureTime(10);
		previous.setTravelTime(20);

		plan.removeActivity(2);

		assertEquals(3, plan.getPlanElements().size());
		assertFalse(previous.getDepartureTime().isDefined());
		assertFalse(previous.getTravelTime().isDefined());
		assertNull(previous.getRoute());
	}

	@Test
	void removingLegDropsFollowingActivityAndClearsNextLegUnlessItWasLast() {
		populateFiveElements();
		Leg next = (Leg) plan.getPlanElements().get(3);
		next.setDepartureTime(10);
		next.setTravelTime(20);
		plan.removeLeg(1);
		assertEquals(3, plan.getPlanElements().size());
		assertFalse(next.getDepartureTime().isDefined());
		assertFalse(next.getTravelTime().isDefined());

		plan.removeLeg(1);
		assertEquals(1, plan.getPlanElements().size());
		plan.removeLeg(0);
		assertEquals(1, plan.getPlanElements().size());
	}

	private void populateFiveElements() {
		plan.addActivity(PopulationUtils.createActivityFromCoord("a", new Coord(0, 0)));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
		plan.addActivity(PopulationUtils.createActivityFromCoord("b", new Coord(1, 1)));
		plan.addLeg(PopulationUtils.createLeg(TransportMode.walk));
		plan.addActivity(PopulationUtils.createActivityFromCoord("c", new Coord(2, 2)));
	}

	private static final class UnknownElement implements org.matsim.api.core.v01.population.PlanElement {
		private final Attributes attributes = new AttributesImpl();

		@Override
		public Attributes getAttributes() {
			return attributes;
		}
	}
}
