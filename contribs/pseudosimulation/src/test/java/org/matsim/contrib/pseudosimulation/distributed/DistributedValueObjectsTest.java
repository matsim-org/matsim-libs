package org.matsim.contrib.pseudosimulation.distributed;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.plans.PlanGenome;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DistributedValueObjectsTest {

	@Test
	void replaceableTravelTimeFailsBeforeInitializationAndDelegatesAfterReplacement() {
		ReplaceableTravelTime replaceable = new ReplaceableTravelTime();
		assertThrows(NullPointerException.class,
				() -> replaceable.getLinkTravelTime(null, 1, null, null));

		replaceable.setTravelTime((link, time, person, vehicle) -> time + 7);
		assertEquals(12.0, replaceable.getLinkTravelTime(null, 5, null, null));
		replaceable.setTravelTime((link, time, person, vehicle) -> 99);
		assertEquals(99.0, replaceable.getLinkTravelTime(null, 5, null, null));
	}

	@Test
	void serializableTravelTimesSampleEachLinkAndTimeBin() {
		Network network = NetworkUtils.createNetwork();
		Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
		Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(1, 0));
		network.addNode(from);
		network.addNode(to);
		Link first = network.getFactory().createLink(Id.createLinkId("first"), from, to);
		Link second = network.getFactory().createLink(Id.createLinkId("second"), from, to);
		network.addLink(first);
		network.addLink(second);

		SerializableLinkTravelTimes times = new SerializableLinkTravelTimes(
				(link, time, person, vehicle) -> (link == first ? 100 : 200) + time / 900,
				900, 3600, List.of(first, second));

		assertEquals(100.0, times.getLinkTravelTime(first, 0, null, null));
		assertEquals(102.0, times.getLinkTravelTime(first, 1800, null, null));
		assertEquals(203.0, times.getLinkTravelTime(second, 3500, null, null));
		assertEquals(100.0, times.getLinkTravelTime(first, 86400, null, null));
	}

	@Test
	void personSerializationRoundTripsSelectedPlanAndBasicElements() {
		Person original = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
		Plan first = PopulationUtils.createPlan(original);
		first.setScore(1.5);
		first.setType("first");
		Activity home = PopulationUtils.createActivityFromCoord("home", new Coord(1, 2));
		home.setStartTime(3);
		home.setEndTime(4);
		home.setMaximumDuration(5);
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
		TripStructureUtils.setRoutingMode(leg, TransportMode.walk);
		leg.setDepartureTime(4);
		leg.setTravelTime(6);
		Activity work = PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("work-link"));
		work.setCoord(new Coord(7, 8));
		work.setStartTime(10);
		work.setEndTime(11);
		work.setMaximumDuration(1);
		first.addActivity(home);
		first.addLeg(leg);
		first.addActivity(work);
		Plan selected = PopulationUtils.createPlan(original);
		selected.setScore(9.0);
		selected.setType("selected");
		original.addPlan(first);
		original.addPlan(selected);
		original.setSelectedPlan(selected);

		Person copy = new PersonSerializable(original).getPerson();

		assertEquals(original.getId(), copy.getId());
		assertEquals(2, copy.getPlans().size());
		assertEquals(9.0, copy.getSelectedPlan().getScore());
		assertEquals("selected", copy.getSelectedPlan().getType());
		Plan copiedFirst = copy.getPlans().get(0);
		assertInstanceOf(PlanGenome.class, copiedFirst);
		assertEquals(3, copiedFirst.getPlanElements().size());
		Activity copiedHome = (Activity) copiedFirst.getPlanElements().get(0);
		assertEquals(new Coord(1, 2), copiedHome.getCoord());
		assertEquals(3, copiedHome.getStartTime().seconds());
		assertEquals(4, copiedHome.getEndTime().seconds());
		assertEquals(5, copiedHome.getMaximumDuration().seconds());
		Leg copiedLeg = (Leg) copiedFirst.getPlanElements().get(1);
		assertEquals(TransportMode.walk, copiedLeg.getMode());
		assertEquals(TransportMode.walk, TripStructureUtils.getRoutingMode(copiedLeg));
		assertEquals(4, copiedLeg.getDepartureTime().seconds());
		assertEquals(6, copiedLeg.getTravelTime().seconds());
		assertNull(copiedLeg.getRoute());
		assertSame(copy, copiedFirst.getPerson());
	}

	@Test
	void planSerializationRejectsActivitiesWithUndefinedTimes() {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("undefined"));
		Plan plan = PopulationUtils.createPlan(person);
		plan.addActivity(PopulationUtils.createActivityFromCoord("home", new Coord(0, 0)));
		person.addPlan(plan);

		assertThrows(java.util.NoSuchElementException.class, () -> new PlanSerializable(plan));
	}
}
