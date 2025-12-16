package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.dsim.simulation.SimpleVehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NetworkTrafficEngineTest {

	@Test
	public void singleVehicleOnLocalNetwork() {

		var scenario = createScenario();
		var expectedEvents = createExpectedEvents();
		var eventsManager = TestUtils.mockExpectingEventsManager(expectedEvents);
		var timeInterpretation = TimeInterpretation.create(scenario.getConfig());
		var wait2link = new DefaultWait2Link(eventsManager);
		var activeNodes = new ActiveNodes(eventsManager);
		var activeLinks = new ActiveLinks(mock(SimStepMessaging.class));
		var parkedVehicles = new MassConservingParking();
		var simNetwork = new SimNetwork(scenario.getNetwork(), scenario.getConfig(), NetworkPartition.SINGLE_INSTANCE, activeLinks, activeNodes);
		var config = new DSimConfigGroup();
		var networkDepartureHandler = new NetworkTrafficDepartureHandler(simNetwork, config, parkedVehicles, wait2link, eventsManager);

		var engine = new NetworkTrafficEngine(scenario, mock(AgentSourcesContainer.class), simNetwork,
			activeNodes, activeLinks, parkedVehicles, wait2link, eventsManager);

		var timer = mock(MobsimTimer.class);

		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("person"));
		PersonDriverAgentImpl agent = new PersonDriverAgentImpl(person.getSelectedPlan(), scenario, eventsManager, timer, timeInterpretation);

		// The planned vehicle id will be the same as the agent id
		SimpleVehicle vehicle = TestUtils.createVehicle("person", 1.0, 10);
		vehicle.setDriver(agent);
		agent.setVehicle(vehicle);
		engine.onPrepareSim();

		engine.addParkedVehicle(vehicle, agent.getCurrentLinkId());

		AtomicInteger i = new AtomicInteger(0);

		agent.endActivityAndComputeNextState(0);

		engine.setInternalInterface(new InternalInterface() {
			@Override
			public Netsim getMobsim() {
				return mock(Netsim.class);
			}

			@Override
			public void arrangeNextAgentState(MobsimAgent agent) {
				assertEquals(112, i.get());
				assertEquals(person.getId(), agent.getId());
			}

			@Override
			public void registerAdditionalAgentOnLink(MobsimAgent agent) {
			}

			@Override
			public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
				return null;
			}

			@Override
			public List<DepartureHandler> getDepartureHandlers() {
				return List.of(networkDepartureHandler);
			}
		});

		networkDepartureHandler.handleDeparture(0, agent, agent.getCurrentLinkId());
		do {
			engine.doSimStep(i.get());
		} while (i.getAndIncrement() <= 120);
	}

	private static List<Event> createExpectedEvents() {
		var personId = Id.createPersonId("person");
		var vehicleId = Id.createVehicleId("person");

		// TODO: Enter and leave events now missing

		return new ArrayList<>(List.of(
			new ActivityEndEvent(0, personId, Id.createLinkId("l1"), null, "start", new Coord(-100, 100)),
			new PersonEntersVehicleEvent(0, personId, vehicleId),
			new VehicleEntersTrafficEvent(0, personId, Id.createLinkId("l1"), vehicleId, "car", 1.0),
			new LinkLeaveEvent(1, vehicleId, Id.createLinkId("l1")),
			new LinkEnterEvent(1, vehicleId, Id.createLinkId("l2")),
			new LinkLeaveEvent(102, vehicleId, Id.createLinkId("l2")),
			new LinkEnterEvent(102, vehicleId, Id.createLinkId("l3")),
			new VehicleLeavesTrafficEvent(112, personId, Id.createLinkId("l3"), vehicleId, "car", 1.0),
			new PersonLeavesVehicleEvent(112, personId, vehicleId),
			new PersonArrivalEvent(112, personId, Id.createLinkId("l3"), "car"),
			new ActivityStartEvent(112, personId, Id.createLinkId("l3"), null, "destination", new Coord(1200, 0))
		));
	}

	private static Scenario createScenario() {

		var scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		scenario.setNetwork(TestUtils.createLocalThreeLinkNetwork());
		addPerson(scenario);
		addVehicle(scenario);
		return scenario;
	}

	private static void addVehicle(Scenario scenario) {

		var type = scenario.getVehicles().getFactory().createVehicleType(Id.create("vehicle-type", VehicleType.class));
		type.setNetworkMode("car");
		type.setMaximumVelocity(10);
		type.setPcuEquivalents(1);
		scenario.getVehicles().addVehicleType(type);

		for (var person : scenario.getPopulation().getPersons().values()) {
			for (var vehType : scenario.getVehicles().getVehicleTypes().values()) {
				var vehicleId = Id.createVehicleId(person.getId().toString() + "_" + vehType.getNetworkMode());
				var vehicle = scenario.getVehicles().getFactory().createVehicle(vehicleId, vehType);
				scenario.getVehicles().addVehicle(vehicle);
			}
		}
	}

	private static void addPerson(Scenario scenario) {
		var factory = scenario.getPopulation().getFactory();
		var person = factory.createPerson(Id.createPersonId("person"));
		var plan = factory.createPlan();

		var startAct = factory.createActivityFromCoord(
			Id.create("start", String.class).toString(), new Coord(-100, 100));
		startAct.setEndTime(10);
		plan.addActivity(startAct);
		var leg = factory.createLeg(Id.create("car", String.class).toString());
		leg.setRoutingMode("car");
		var route = RouteUtils.createLinkNetworkRouteImpl(
			Id.createLinkId("l1"),
			List.of(Id.createLinkId("l2")),
			Id.createLinkId("l3"));
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(
			factory.createActivityFromCoord(
				Id.create("destination", String.class).toString(), new Coord(1200, 0)
			)
		);

		new XY2Links(scenario).run(plan);
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
}
