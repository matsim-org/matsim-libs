package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NetworkTrafficEngineTest {

    @Test
    public void singleVehicleOnLocalNetwork() {

        var scenario = createScenario();
        var expectedEvents = createExpectedEvents();
        var eventsManager = TestUtils.mockExpectingEventsManager(expectedEvents);
        var engine = new NetworkTrafficEngine(scenario, mock(SimStepMessaging.class), eventsManager, 0);

        var simPerson = new SimPerson(scenario.getPopulation().getPersons().get(Id.createPersonId("person")));
        simPerson.advancePlan();

        engine.setNextStateHandler((person, now) -> {
            assertEquals(112, now);
            assertEquals(simPerson.getId(), person.getId());
        });

        engine.accept(simPerson, 0);

        // now, do the simulation part

        for (int i = 0; i <= 120; i++) {
            engine.doSimStep(i);
        }
    }

    private static List<Event> createExpectedEvents() {
        var personId = Id.createPersonId("person");
        var vehicleId = Id.createVehicleId("person_car");

        return new ArrayList<>(List.of(
                new PersonEntersVehicleEvent(0, personId, vehicleId),
                new VehicleEntersTrafficEvent(0, personId, Id.createLinkId("l1"), vehicleId, "car", 1.0),
                new LinkLeaveEvent(1, vehicleId, Id.createLinkId("l1")),
                new LinkEnterEvent(1, vehicleId, Id.createLinkId("l2")),
                new LinkLeaveEvent(102, vehicleId, Id.createLinkId("l2")),
                new LinkEnterEvent(102, vehicleId, Id.createLinkId("l3")),
                new VehicleLeavesTrafficEvent(112, personId, Id.createLinkId("l3"), vehicleId, "car", 1.0),
                new PersonLeavesVehicleEvent(112, personId, vehicleId),
                new PersonArrivalEvent(112, personId, Id.createLinkId("l3"), "car")
        ));
    }

    private static Scenario createScenario() {

        var scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        scenario.setNetwork(TestUtils.createLocalThreeLinkNetwork());
        addPerson("person", scenario);
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

    private static void addPerson(String id, Scenario scenario) {
        var factory = scenario.getPopulation().getFactory();
        var person = factory.createPerson(Id.createPersonId(id));
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