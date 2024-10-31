package org.matsim.dsim.simulation;

import com.google.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.TestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;
import static org.mockito.Mockito.mock;

public class SimProcessTest {

    private static Person createPerson(PopulationFactory factory) {

        var plan = factory.createPlan();

        var startAct = factory.createActivityFromCoord("start", new Coord(0, 100));
        startAct.setEndTime(10);
        startAct.setStartTimeUndefined();
        startAct.setLinkId(Id.createLinkId("l1"));
        plan.addActivity(startAct);

        var access = factory.createLeg("walk");
        access.setRoutingMode("default");
        access.setRoute(RouteUtils.createGenericRouteImpl(Id.createLinkId("l1"), Id.createLinkId("l1")));
        access.setDepartureTime(startAct.getEndTime().seconds());
        access.setTravelTime(10);
        plan.addLeg(access);

        var interaction1 = factory.createActivityFromLinkId("default interaction", Id.createLinkId("l1"));
        interaction1.setMaximumDuration(0);
        interaction1.setCoord(new Coord(0, 0));
        plan.addActivity(interaction1);

        var leg = factory.createLeg("default");
        leg.setRoutingMode("default");
        leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(
                Id.createLinkId("l1"),
                List.of(Id.createLinkId("l2")),
                Id.createLinkId("l3")));
        leg.setDepartureTimeUndefined();
        leg.setTravelTimeUndefined();
        plan.addLeg(leg);

        var interaction2 = factory.createActivityFromLinkId("default interaction", Id.createLinkId("l3"));
        interaction2.setMaximumDuration(0);
        interaction2.setCoord(new Coord(1200, 0));
        plan.addActivity(interaction2);

        var egress = factory.createLeg("walk");
        egress.setRoutingMode("default");
        egress.setRoute(RouteUtils.createGenericRouteImpl(Id.createLinkId("l3"), Id.createLinkId("l3")));
        //egress.setDepartureTime(startAct.getEndTime().seconds());
        egress.setTravelTime(10);
        plan.addLeg(egress);

        var endAct = factory.createActivityFromCoord("end", new Coord(0, 1200));
        endAct.setEndTimeUndefined();
        endAct.setLinkId(Id.createLinkId("l3"));
        plan.addActivity(endAct);

        var person = factory.createPerson(Id.createPersonId("test-person"));
        person.addPlan(plan);
        return person;
    }

    @Test
    public void localThreeLinkScenario() {

        var config = ConfigUtils.createConfig();
        config.qsim().setMainModes(Set.of("default"));
        var scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setNetwork(TestUtils.createLocalThreeLinkNetwork());
        scenario.getPopulation().addPerson(createPerson(scenario.getPopulation().getFactory()));
        var defaultType = VehicleUtils.createVehicleType(Id.create("default", VehicleType.class));
        scenario.getVehicles().addVehicleType(defaultType);
        scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId("test-person_default"), defaultType));

        var broker = mock(MessageBroker.class);
        ArrayList<Event> expectedEvents = new ArrayList<>(List.of(
                new ActivityEndEvent(10, Id.createPersonId("test-person"), Id.createLinkId("l1"), null, "start", null),
                new PersonDepartureEvent(10, Id.createPersonId("test-person"), Id.createLinkId("l1"), "walk", "default"),
                new TeleportationArrivalEvent(20, Id.createPersonId("test-person"), 0, "walk"),
                new PersonArrivalEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), "walk"),
                new ActivityStartEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), null, "default interaction", null),
                new ActivityEndEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), null, "default interaction", null),
                new PersonDepartureEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), "default", "default"),
                new PersonEntersVehicleEvent(20, Id.createPersonId("test-person"), Id.createVehicleId("test-person_default")),
                new VehicleEntersTrafficEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), Id.createVehicleId("test-person_default"), "default", 0),
                new LinkLeaveEvent(21, Id.createVehicleId("test-person_default"), Id.createLinkId("l1")),
                new LinkEnterEvent(21, Id.createVehicleId("test-person_default"), Id.createLinkId("l2")),
                new LinkLeaveEvent(58, Id.createVehicleId("test-person_default"), Id.createLinkId("l2")),
                new LinkEnterEvent(58, Id.createVehicleId("test-person_default"), Id.createLinkId("l3")),
                new VehicleLeavesTrafficEvent(62, Id.createPersonId("test-person"), Id.createLinkId("l3"), Id.createVehicleId("test-person_default"), "default", 0),
                new PersonLeavesVehicleEvent(62, Id.createPersonId("test-person"), Id.createVehicleId("test-person_default")),
                new PersonArrivalEvent(62, Id.createPersonId("test-person"), Id.createLinkId("l3"), "walk"),
                new ActivityStartEvent(62, Id.createPersonId("test-person"), Id.createLinkId("l3"), null, "default interaction", null),
                new ActivityEndEvent(62, Id.createPersonId("test-person"), Id.createLinkId("l3"), null, "default interaction", null),
                new PersonDepartureEvent(62, Id.createPersonId("test-person"), Id.createLinkId("l3"), "walk", "default"),
                new TeleportationArrivalEvent(72, Id.createPersonId("test-person"), 0, "walk"),
                new PersonArrivalEvent(72, Id.createPersonId("test-person"), Id.createLinkId("l1"), "walk"),
                new ActivityStartEvent(72, Id.createPersonId("test-person"), Id.createLinkId("l3"), null, "end", null)
        ));
        var eventsManager = TestUtils.mockExpectingEventsManager(expectedEvents);

        var injector = Injector.createInjector(config, new AbstractModule() {
            @Override
            public void install() {

                bind(Scenario.class).toInstance(scenario);
                bind(Network.class).toInstance(scenario.getNetwork());
                bind(TimeInterpretation.class).in(Singleton.class);
                bind(MessageBroker.class).toInstance(broker);
                bind(EventsManager.class).toInstance(eventsManager);
                bind(SimProvider.class);
            }
        });
        var simProvider = injector.getInstance(SimProvider.class);
        var simProcess = (Steppable) simProvider.create(0);

        assertNotNull(simProcess);

        for (var i = 0; i < 100; i++) {
            simProcess.doSimStep(i);
        }
    }

    @Test
    public void distributedThreeLinkScenario() {
        var config = ConfigUtils.createConfig();
        config.qsim().setMainModes(Set.of("default"));
        var scenario = ScenarioUtils.createMutableScenario(config);
        var network = TestUtils.createLocalThreeLinkNetwork();
        network.getLinks().get(Id.createLinkId("l2")).getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
        scenario.setNetwork(TestUtils.createLocalThreeLinkNetwork());
        scenario.getPopulation().addPerson(createPerson(scenario.getPopulation().getFactory()));
        var defaultType = VehicleUtils.createVehicleType(Id.create("default", VehicleType.class));
        scenario.getVehicles().addVehicleType(defaultType);
        scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId("test-person_default"), defaultType));

        var broker = mock(MessageBroker.class);
        var em = mock(EventsManager.class);

        var injector = Injector.createInjector(config, new AbstractModule() {
            @Override
            public void install() {

                bind(Scenario.class).toInstance(scenario);
                bind(Network.class).toInstance(scenario.getNetwork());
                bind(TimeInterpretation.class).in(Singleton.class);
                bind(MessageBroker.class).toInstance(broker);
                bind(EventsManager.class).toInstance(em);
                bind(SimProvider.class);
            }
        });
        var simProvider = injector.getInstance(SimProvider.class);
        var simProcess = (Steppable) simProvider.create(0);

        assertNotNull(simProcess);

        for (var i = 0; i < 100; i++) {
            simProcess.doSimStep(i);
        }
    }
}
