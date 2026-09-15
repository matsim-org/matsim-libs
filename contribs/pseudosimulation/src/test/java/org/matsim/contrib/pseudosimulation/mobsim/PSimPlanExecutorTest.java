package org.matsim.contrib.pseudosimulation.mobsim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;

class PSimPlanExecutorTest {

    @Test
    void createsTeleportationArrivalAndPlanTransitionEvents() {
        Plan plan = createPlan(TransportMode.walk, 10, 7);
        Execution execution = execute(plan, 100, null, Set.of());

        assertEquals(List.of(ActivityEndEvent.class, PersonDepartureEvent.class,
                        org.matsim.core.api.experimental.events.TeleportationArrivalEvent.class,
                        PersonArrivalEvent.class, ActivityStartEvent.class),
                execution.events.stream().map(Event::getClass).toList());
        assertEquals(List.of(10.0, 10.0, 17.0, 17.0, 17.0),
                execution.events.stream().map(Event::getTime).toList());
    }

    @Test
    void retainsEventsQueuedBeforeAUnititializedRouteIsSkipped() {
        Plan plan = createPlan(TransportMode.walk, 10, null);
        Execution execution = execute(plan, 100, null, Set.of());

        assertEquals(List.of(ActivityEndEvent.class, PersonDepartureEvent.class),
                execution.events.stream().map(Event::getClass).toList());
    }

    @Test
    void replacesFirstEventBeyondEndTimeWithStuckEvent() {
        Plan plan = createPlan(TransportMode.walk, 10, 7);
        Execution execution = execute(plan, 5, null, Set.of());

        assertEquals(List.of(PersonStuckEvent.class), execution.events.stream().map(Event::getClass).toList());
        assertEquals(5.0, execution.events.getFirst().getTime());
    }

    @Test
    void usesDummyVehicleForTransitTripWithoutVehicleId() {
        Plan plan = createPlan(TransportMode.pt, 10, 7);
        TransitEmulator emulator = (leg, departure) -> new TransitEmulator.Trip(null, departure + 2, departure + 8);
        Execution execution = execute(plan, 100, emulator, Set.of(TransportMode.pt));

        assertEquals(List.of(ActivityEndEvent.class, PersonDepartureEvent.class, PersonEntersVehicleEvent.class,
                        PersonLeavesVehicleEvent.class, PersonArrivalEvent.class, ActivityStartEvent.class),
                execution.events.stream().map(Event::getClass).toList());
        PersonEntersVehicleEvent enter = (PersonEntersVehicleEvent) execution.events.get(2);
        assertEquals("dummy", enter.getVehicleId().toString());
        assertEquals(18.0, execution.events.get(4).getTime());
    }

    @Test
    void retainsLegacyCarLinkEventTiming() {
        Network network = NetworkUtils.createNetwork();
        var first = NetworkUtils.createAndAddNode(network, Id.createNodeId("first"), new Coord(0, 0));
        var second = NetworkUtils.createAndAddNode(network, Id.createNodeId("second"), new Coord(1, 0));
        var third = NetworkUtils.createAndAddNode(network, Id.createNodeId("third"), new Coord(2, 0));
        Link start = NetworkUtils.createAndAddLink(network, Id.createLinkId("start"), first, second, 1, 1, 1, 1);
        Link end = NetworkUtils.createAndAddLink(network, Id.createLinkId("end"), second, third, 1, 1, 1, 1);
        Plan plan = createPlan(TransportMode.car, 10, null);
        Leg leg = (Leg) plan.getPlanElements().get(1);
        leg.setRoute(RouteUtils.createNetworkRoute(List.of(start.getId(), end.getId()), network));

        Execution execution = execute(plan, 100, network, null, Set.of());

        assertEquals(List.of(ActivityEndEvent.class, PersonDepartureEvent.class, PersonEntersVehicleEvent.class,
                        VehicleEntersTrafficEvent.class, LinkLeaveEvent.class, LinkEnterEvent.class,
                        VehicleLeavesTrafficEvent.class, PersonLeavesVehicleEvent.class,
                        PersonArrivalEvent.class, ActivityStartEvent.class),
                execution.events.stream().map(Event::getClass).toList());
        assertEquals(List.of(10.0, 10.0, 10.0, 10.0, 11.0, 11.0, 12.0, 12.0, 12.0, 12.0),
                execution.events.stream().map(Event::getTime).toList());
    }

    private static Execution execute(Plan plan, double endTime, TransitEmulator emulator, Set<String> transitModes) {
        return execute(plan, endTime, NetworkUtils.createNetwork(), emulator, transitModes);
    }

    private static Execution execute(Plan plan, double endTime, Network network, TransitEmulator emulator,
            Set<String> transitModes) {
        List<Event> events = new ArrayList<>();
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler((BasicEventHandler) events::add);
        PSimPlanExecutor executor = new PSimPlanExecutor(endTime, (link, time, person, vehicle) -> 1,
                emulator, transitModes);
        executor.initialize(List.of(plan), network, eventsManager);
        executor.run();
        return new Execution(events);
    }

    private static Plan createPlan(String mode, double activityEnd, Integer routeTravelTime) {
        Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
        Plan plan = PopulationUtils.createPlan(person);
        person.addPlan(plan);
        Activity origin = PopulationUtils.createActivityFromLinkId("origin", Id.createLinkId("from"));
        origin.setEndTime(activityEnd);
        Leg leg = PopulationUtils.createLeg(mode);
        if (routeTravelTime != null) {
            var route = RouteUtils.createGenericRouteImpl(Id.createLinkId("from"), Id.createLinkId("to"));
            route.setTravelTime(routeTravelTime);
            route.setDistance(42);
            leg.setRoute(route);
        }
        Activity destination = PopulationUtils.createActivityFromLinkId("destination", Id.createLinkId("to"));
        plan.addActivity(origin);
        plan.addLeg(leg);
        plan.addActivity(destination);
        return plan;
    }

    private record Execution(List<Event> events) {
    }
}
