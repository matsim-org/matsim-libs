package org.matsim.dsim.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.NewScoreAssigner;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.dsim.simulation.IterationInformation;
import org.matsim.facilities.ActivityFacility;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndOfDayScoringTest {

	@Test
	void simpleTrip() {

		var personId = Id.createPersonId("p1");
		var link1 = Id.createLinkId("l1");
		var link2 = Id.createLinkId("l2");

		var population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		var person = population.getFactory().createPerson(personId);
		population.addPerson(person);

		var network = NetworkUtils.createNetwork();
		var node1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var node2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(1001, 0));
		var node3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(2002, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addLink(network.getFactory().createLink(Id.createLinkId(link1), node1, node2));
		network.addLink(network.getFactory().createLink(Id.createLinkId(link2), node2, node3));

		var startFacilityId = Id.create("f1", ActivityFacility.class);
		var backpack = new BackPack(personId);
		backpack.backpackPlan().handleEvent(new ActivityEndEvent(10, personId, link1, startFacilityId, "start", new Coord(0, 0)));
		backpack.backpackPlan().handleEvent(new PersonDepartureEvent(10., personId, link1, "walk", "walk"));
		backpack.backpackPlan().handleEvent(new TeleportationArrivalEvent(25, personId, 339, "walk"));
		backpack.backpackPlan().handleEvent(new PersonArrivalEvent(25, personId, link2, "walk"));
		backpack.backpackPlan().finishLeg(network, null);
		backpack.backpackPlan().handleEvent(new ActivityStartEvent(25, personId, link2, null, "last", new Coord(1001, 0)));
		backpack.addSpecialScoringEvent(new PersonMoneyEvent(10, personId, -10, "pay", "partner", "ref"));
		backpack.addSpecialScoringEvent(new PersonScoreEvent(10, personId, -100, "special kind"));
		backpack.addSpecialScoringEvent(new PersonStuckEvent(10, personId, link2, "stuckMode", "some reason"));
		backpack.backpackPlan().finish(network, null);

		// we have collected all the data but not yet scored the plan.
		assertNull(backpack.backpackPlan().experiencedPlan().getScore());

		NewScoreAssigner newScoreAssigner = (_, _, p) -> assertEquals(personId, p.getId());
		var scoringFunction = new TestScoringFunction();
		ScoringFunctionFactory sff = _ -> scoringFunction;
		var eods = new EndOfDayScoring(population, sff, newScoreAssigner, new IterationInformation());
		eods.score(backpack);

		assertEquals(2, scoringFunction.activities.size());
		assertEquals(1, scoringFunction.legs.size());
		assertEquals(1, scoringFunction.trips.size());
		assertEquals("start", scoringFunction.activities.getFirst().getType());
		assertEquals("last", scoringFunction.activities.getLast().getType());
		assertEquals("walk", scoringFunction.legs.getFirst().getMode());
		// now compare the trip to the individual plan elements
		assertEquals(scoringFunction.activities.getFirst(), scoringFunction.trips.getFirst().getOriginActivity());
		assertEquals(scoringFunction.activities.getLast(), scoringFunction.trips.getFirst().getDestinationActivity());
		assertEquals(1, scoringFunction.trips.getFirst().getTripElements().size());
		assertEquals(scoringFunction.legs.getFirst(), scoringFunction.trips.getFirst().getTripElements().getFirst());
		assertTrue(scoringFunction.isFinished);
		assertTrue(scoringFunction.isStuck);
		assertEquals(-100, scoringFunction.score);
		assertEquals(-10, scoringFunction.money);
	}

	private static class TestScoringFunction implements ScoringFunction {

		final List<Activity> activities = new ArrayList<>();
		final List<Leg> legs = new ArrayList<>();
		final List<TripStructureUtils.Trip> trips = new ArrayList<>();
		double money = 0;
		double score = 0;
		boolean isStuck = false;
		boolean isFinished = false;

		@Override
		public void handleActivity(Activity activity) {
			activities.add(activity);
		}

		@Override
		public void handleLeg(Leg leg) {
			legs.add(leg);
		}

		@Override
		public void agentStuck(double time) {
			isStuck = true;
		}

		@Override
		public void addMoney(double amount) {
			this.money += amount;
		}

		@Override
		public void addScore(double amount) {
			this.score += amount;
		}

		@Override
		public void finish() {
			isFinished = true;
		}

		@Override
		public double getScore() {
			return 42;
		}

		@Override
		public void handleEvent(Event event) {

		}

		@Override
		public void handleTrip(TripStructureUtils.Trip trip) {
			trips.add(trip);
		}
	}

}
