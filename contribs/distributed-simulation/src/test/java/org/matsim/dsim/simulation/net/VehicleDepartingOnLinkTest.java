package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleDepartingOnLinkTest {

	@Test
	void vehicleIntoBuffer() {

		var em = mock(EventsManager.class);
		var wait2link = new DefaultWait2Link(em);
		var link = TestUtils.createSingleLink(0, 0);
		link.setCapacity(3600);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var simLink = TestUtils.createLink(link, config, 10);

		var simPerson1 = createPerson("p-1", List.of(simLink.getId().toString(), "l2", "l3"), mock(EventsManager.class));
		var simVehicle1 = TestUtils.createVehicle("veh-1", 2, 1);
		simVehicle1.setDriver(simPerson1);
		simPerson1.setVehicle(simVehicle1);
		var simPerson2 = createPerson("p-2", List.of(simLink.getId().toString(), "l2", "l3"), mock(EventsManager.class));
		var simVehicle2 = TestUtils.createVehicle("veh-2", 2, 1);
		simVehicle2.setDriver(simPerson2);
		simPerson2.setVehicle(simVehicle2);
		var blockingPerson = createPerson("blocking", List.of(simLink.getId().toString(), "l2", "l3"), mock(EventsManager.class));
		var blockingVehicle = TestUtils.createVehicle("blocking-vehicle", 2, 1);
		blockingVehicle.setDriver(blockingPerson);
		blockingPerson.setVehicle(blockingVehicle);

		simLink.pushVehicle(blockingVehicle, SimLink.LinkPosition.Buffer, 0);

		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 0));
		assertTrue(simLink.isOffering());

		wait2link.accept(simVehicle1, simLink, 0);
		// add a second vehicle to assert the correct order.
		wait2link.accept(simVehicle2, simLink, 0);
		// the vehicle can't go onto the link, as the buffer is blocked by another vehicle
		wait2link.moveWaiting(0);
		verify(em, times(0)).processEvent(any());

		// free the link
		assertEquals(blockingVehicle.getId(), simLink.popVehicle().getId());
		simLink.doSimStep(null, 2);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.Buffer, 2));
		assertFalse(simLink.isOffering());

		// try again
		wait2link.moveWaiting(2);
		assertFalse(simLink.isAccepting(SimLink.LinkPosition.Buffer, 2));
		assertTrue(simLink.isOffering());
		assertEquals(simVehicle1.getId(), simLink.popVehicle().getId());
		verify(em, times(1)).processEvent(any());

		simLink.doSimStep(null, 4);
		wait2link.moveWaiting(4);
		assertEquals(simVehicle2.getId(), simLink.popVehicle().getId());
		verify(em, times(2)).processEvent(any());
	}

	@Test
	void vehicleIntoQueue() {

		var em = mock(EventsManager.class);
		var wait2link = new DefaultWait2Link(em);
		var link = TestUtils.createSingleLink(0, 0);
		link.setCapacity(3600);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		config.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		var simLink = TestUtils.createLink(link, config, 1);

		var simPerson1 = createPerson("p-1", List.of(simLink.getId().toString()), mock(EventsManager.class));
		var simVehicle1 = TestUtils.createVehicle("veh-1", 2, 1);
		simVehicle1.setDriver(simPerson1);
		simPerson1.setVehicle(simVehicle1);
		var simPerson2 = createPerson("p-2", List.of(simLink.getId().toString()), mock(EventsManager.class));
		var simVehicle2 = TestUtils.createVehicle("veh-2", 2, 1);
		simVehicle2.setDriver(simPerson2);
		simPerson2.setVehicle(simVehicle2);
		var blockingPerson = createPerson("blocking", List.of(simLink.getId().toString(), "l2", "l3"), mock(EventsManager.class));
		var blockingVehicle = TestUtils.createVehicle("blocking-vehicle", 100, 1);
		blockingVehicle.setDriver(blockingPerson);
		blockingPerson.setVehicle(blockingVehicle);

		simLink.pushVehicle(blockingVehicle, SimLink.LinkPosition.QEnd, 0);

		assertFalse(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));

		wait2link.accept(simVehicle1, simLink, 0);
		// add a second vehicle to assert the correct order.
		wait2link.accept(simVehicle2, simLink, 0);
		// the vehicle can't go onto the link, as the queue is blocked by another vehicle
		wait2link.moveWaiting(0);
		verify(em, times(0)).processEvent(any());

		// free the link
		simLink.doSimStep(null, 0);
		assertTrue(simLink.isAccepting(SimLink.LinkPosition.QEnd, 0));
		assertTrue(simLink.isOffering());
		// this should put both vehicles onto the link
		wait2link.moveWaiting(0);
		verify(em, times(2)).processEvent(any());

		var expectedIt = List.of(Id.createVehicleId("veh-2"), Id.createVehicleId("veh-1")).iterator();
		simLink.addLeaveHandler((v, _, _) -> {
			var expectedId = expectedIt.next();
			assertEquals(expectedId, v.getId());
			return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
		});
		assertFalse(simLink.doSimStep(null, 0));
	}

	@Test
	void vehiclesOntoMultipleLinks() {
		var em = mock(EventsManager.class);
		var wait2link = new DefaultWait2Link(em);
		var link1 = TestUtils.createSingleLink(0, 0);
		link1.setCapacity(3600);
		var link2 = TestUtils.createSingleLink(0, 0);
		link2.setCapacity(3600);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		var simLink1 = TestUtils.createLink(link1, config, 1);
		var simLink2 = TestUtils.createLink(link2, config, 1);

		var simPerson1 = createPerson("p-1", List.of(simLink1.getId().toString(), "l2", "l3"), em);
		var simVehicle1 = TestUtils.createVehicle("veh-1", 2, 1);
		simVehicle1.setDriver(simPerson1);
		simPerson1.setVehicle(simVehicle1);
		var simPerson2 = createPerson("p-2", List.of(simLink2.getId().toString(), "l2", "l3"), em);
		var simVehicle2 = TestUtils.createVehicle("veh-2", 2, 1);
		simVehicle2.setDriver(simPerson2);
		simPerson2.setVehicle(simVehicle2);

		assertFalse(simLink1.isOffering());
		assertFalse(simLink2.isOffering());

		wait2link.accept(simVehicle1, simLink1, 0);
		wait2link.accept(simVehicle2, simLink2, 0);
		wait2link.moveWaiting(0);

		assertEquals(simVehicle1.getId(), simLink1.popVehicle().getId());
		assertEquals(simVehicle2.getId(), simLink2.popVehicle().getId());
	}

	private static PersonDriverAgentImpl createPerson(String id, List<String> linkIds, EventsManager em) {

		var config = ConfigUtils.createConfig();
		var scenario = ScenarioUtils.createScenario(config);
		var timer = new MobsimTimer();
		var timeInterpretation = TimeInterpretation.create(config);
		var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
		scenario.getPopulation().addPerson(person);
		var plan = PopulationUtils.createPlan(person);
		var act = PopulationUtils.createActivityFromLinkId("start", Id.createLinkId(linkIds.getFirst()));
		plan.addActivity(act);
		var leg = PopulationUtils.createLeg("car");
		leg.setRoutingMode("car");
		leg.setDepartureTime(10);
		leg.setTravelTime(42);
		var route = RouteUtils.createNetworkRoute(linkIds.stream().map(Id::createLinkId).toList());
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.setPerson(person);

		var agent = new PersonDriverAgentImpl(plan, scenario, em, timer, timeInterpretation);
		agent.endActivityAndComputeNextState(0);
		return agent;
	}
}
