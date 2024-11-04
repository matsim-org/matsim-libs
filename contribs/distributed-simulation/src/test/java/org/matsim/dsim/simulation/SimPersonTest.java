package org.matsim.dsim.simulation;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.dsim.messages.PersonMsg;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimPersonTest {

	@Test
	void initFromPerson() {
		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var simPerson = new SimPerson(person);

		assertEquals(person.getId(), simPerson.getId());
		assertEquals(person.getSelectedPlan().getPlanElements().getFirst(), simPerson.getCurrentActivity());
		assertThrows(RuntimeException.class, simPerson::getCurrentLeg);
		assertEquals(SimPerson.State.ACTIVITY, simPerson.getCurrentState());
	}

	@Test
	void initFromMessage() {
		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var msg = PersonMsg.builder()
			.setId(person.getId())
			.setPlan(person.getSelectedPlan().getPlanElements())
			.setCurrentPlanElement(1)
			.build();
		var simPerson = new SimPerson(msg);

		assertEquals(person.getId(), simPerson.getId());
		assertEquals(person.getSelectedPlan().getPlanElements().get(1), simPerson.getCurrentLeg());
		assertThrows(RuntimeException.class, simPerson::getCurrentActivity);
		assertEquals(SimPerson.State.LEG, simPerson.getCurrentState());
	}

	@Test
	void advancePlan() {
		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var simPerson = new SimPerson(person);

		var planIterator = person.getSelectedPlan().getPlanElements().iterator();
		assertEquals(planIterator.next(), simPerson.getCurrentActivity());
		assertEquals(SimPerson.State.ACTIVITY, simPerson.getCurrentState());
		simPerson.advancePlan();
		assertEquals(planIterator.next(), simPerson.getCurrentLeg());
		assertEquals(SimPerson.State.LEG, simPerson.getCurrentState());
		simPerson.advancePlan();
		assertEquals(planIterator.next(), simPerson.getCurrentActivity());
		assertEquals(SimPerson.State.ACTIVITY, simPerson.getCurrentState());
		assertThrows(RuntimeException.class, simPerson::advancePlan);
	}

	@Test
	void advanceTeleportedRoute() {
		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
		var route = new GenericRouteImpl(Id.createLinkId("start"), Id.createLinkId("end"));
		leg.setRoute(route);
		var simPerson = new SimPerson(person);
		simPerson.advancePlan();

		assertEquals(leg, simPerson.getCurrentLeg());
		assertEquals(SimPerson.State.LEG, simPerson.getCurrentState());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getCurrentRouteElement());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Curent));
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Last));
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Next));

		simPerson.advanceRoute(SimPerson.Advance.One);
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getCurrentRouteElement());
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Curent));
		assertNull(simPerson.getRouteElement(SimPerson.RouteAccess.Next));
	}

	@Test
	void advanceTeleportedRouteSameLink() {
		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
		var route = new GenericRouteImpl(Id.createLinkId("start"), Id.createLinkId("start"));
		leg.setRoute(route);
		var simPerson = new SimPerson(person);
		simPerson.advancePlan();

		assertEquals(leg, simPerson.getCurrentLeg());
		assertEquals(SimPerson.State.LEG, simPerson.getCurrentState());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getCurrentRouteElement());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Curent));
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Last));
		assertNull(simPerson.getRouteElement(SimPerson.RouteAccess.Next));

		simPerson.advanceRoute(SimPerson.Advance.One);
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getCurrentRouteElement());
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Curent));
		assertNull(simPerson.getRouteElement(SimPerson.RouteAccess.Next));
	}

	@Test
	void advanceNetworkRoute() {

		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
		var route = RouteUtils.createNetworkRoute(List.of(
			Id.createLinkId("1"),
			Id.createLinkId("2"),
			Id.createLinkId("3"),
			Id.createLinkId("4")));
		leg.setRoute(route);
		var simPerson = new SimPerson(person);
		simPerson.advancePlan();

		assertEquals(leg, simPerson.getCurrentLeg());
		assertEquals(SimPerson.State.LEG, simPerson.getCurrentState());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getCurrentRouteElement());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Curent));
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Last));

		var routeIterator = ((NetworkRoute) leg.getRoute()).getLinkIds().iterator();
		simPerson.advanceRoute(SimPerson.Advance.One);
		assertEquals(routeIterator.next(), simPerson.getCurrentRouteElement());
		simPerson.advanceRoute(SimPerson.Advance.One);
		assertEquals(routeIterator.next(), simPerson.getCurrentRouteElement());
		simPerson.advanceRoute(SimPerson.Advance.One);
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getCurrentRouteElement());
		assertNull(simPerson.getRouteElement(SimPerson.RouteAccess.Next));
	}

	@Test
	void advanceNetworkRouteSameLink() {

		var pop = createSinglePersonPopulation();
		var person = pop.getPersons().values().iterator().next();
		var leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
		var route = RouteUtils.createNetworkRoute(List.of(Id.createLinkId("start-end")));
		leg.setRoute(route);
		var simPerson = new SimPerson(person);
		simPerson.advancePlan();

		assertEquals(leg, simPerson.getCurrentLeg());
		assertEquals(SimPerson.State.LEG, simPerson.getCurrentState());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getCurrentRouteElement());
		assertEquals(leg.getRoute().getStartLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Curent));
		assertEquals(leg.getRoute().getEndLinkId(), simPerson.getRouteElement(SimPerson.RouteAccess.Last));
		assertNull(simPerson.getRouteElement(SimPerson.RouteAccess.Next));
		// though the network route does only have one element, we can advance, because the implementation
		// operates on the start and end link. This is handy for teleportation.
		simPerson.advanceRoute(SimPerson.Advance.One);
		assertEquals(route.getEndLinkId(), simPerson.getCurrentRouteElement());
		// advance once more, so that we are finally out of bounds
		simPerson.advanceRoute(SimPerson.Advance.One);
		assertNull(simPerson.getCurrentRouteElement());
	}

	private Population createSinglePersonPopulation() {

		var pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		var person = pop.getFactory().createPerson(Id.createPersonId("test"));
		pop.addPerson(person);

		var plan = pop.getFactory().createPlan();
		person.addPlan(plan);

		var startAct = pop.getFactory().createActivityFromLinkId("start", Id.createLinkId("start-link"));
		plan.addActivity(startAct);

		var leg = pop.getFactory().createLeg("some-mode");
		plan.addLeg(leg);

		var endAct = pop.getFactory().createActivityFromLinkId("end", Id.createLinkId("end-link"));
		plan.addActivity(endAct);

		return pop;
	}
}
