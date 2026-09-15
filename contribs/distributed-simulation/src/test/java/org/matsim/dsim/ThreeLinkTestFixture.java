package org.matsim.dsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

public class ThreeLinkTestFixture {

    public static Network createNetwork(int numParts) {

        var network = NetworkUtils.createNetwork();
        var n1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
        n1.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
        var n2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(0, 100));
        n2.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
        var n3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(0, 1100));
        n3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
        var n4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(0, 1200));
        n4.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);

        var l1 = network.getFactory().createLink(Id.createLinkId("l1"), n1, n2);
        l1.setFreespeed(27.78);
        l1.setCapacity(3600);
        l1.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
        var l2 = network.getFactory().createLink(Id.createLinkId("l2"), n2, n3);
        l2.setFreespeed(27.78);
        l2.setCapacity(3600);
        l2.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
        var l3 = network.getFactory().createLink(Id.createLinkId("l3"), n3, n4);
        l3.setFreespeed(27.78);
        l3.setCapacity(3600);
        l3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);

        if (numParts == 2) {
            n3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
            n4.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
            l2.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
            l3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
        } else if (numParts == 3) {
            n3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
            n4.getAttributes().putAttribute(PARTITION_ATTR_KEY, 2);
            l2.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
            l3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 2);
        } else {
            throw new IllegalArgumentException("Can't divide three links into more than three parts.");
        }

        network.addNode(n1);
        network.addNode(n2);
        network.addNode(n3);
        network.addNode(n4);
        network.addLink(l1);
        network.addLink(l2);
        network.addLink(l3);

        return network;
    }

    public static Person createPerson(PopulationFactory factory) {

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

    public static List<Event> expectedEvents() {
        List<Event> result = new ArrayList<>();

        result.add(new ActivityEndEvent(10, Id.createPersonId("test-person"), Id.createLinkId("l1"), null, "start", null));
        result.add(new PersonDepartureEvent(10, Id.createPersonId("test-person"), Id.createLinkId("l1"), "walk", "default"));
        // here should be a travelled event
        result.add(new PersonArrivalEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), "walk"));
        result.add(new ActivityStartEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), null, "default interaction", null));
        result.add(new ActivityEndEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), null, "default interaction", null));
        result.add(new PersonDepartureEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), "default", "default"));
        result.add(new PersonEntersVehicleEvent(20, Id.createPersonId("test-person"), Id.createVehicleId("test-person_default")));
        result.add(new VehicleEntersTrafficEvent(20, Id.createPersonId("test-person"), Id.createLinkId("l1"), Id.createVehicleId("test-person_default"), "default", 0));
        result.add(new LinkLeaveEvent(21, Id.createVehicleId("test-person_default"), Id.createLinkId("l1")));
        result.add(new LinkEnterEvent(21, Id.createVehicleId("test-person_default"), Id.createLinkId("l2")));
        result.add(new LinkLeaveEvent(57, Id.createVehicleId("test-person_default"), Id.createLinkId("l2")));
        result.add(new LinkEnterEvent(57, Id.createVehicleId("test-person_default"), Id.createLinkId("l3")));
        result.add(new VehicleLeavesTrafficEvent(61, Id.createPersonId("test-person"), Id.createLinkId("l3"), Id.createVehicleId("test-person_default"), "default", 0));
        result.add(new PersonLeavesVehicleEvent(61, Id.createPersonId("test-person"), Id.createVehicleId("test-person_default")));
        result.add(new PersonArrivalEvent(61, Id.createPersonId("test-person"), Id.createLinkId("l3"), "walk"));
        result.add(new ActivityStartEvent(61, Id.createPersonId("test-person"), Id.createLinkId("l3"), null, "default interaction", null));
        result.add(new ActivityEndEvent(61, Id.createPersonId("test-person"), Id.createLinkId("l3"), null, "default interaction", null));
        result.add(new PersonDepartureEvent(61, Id.createPersonId("test-person"), Id.createLinkId("l3"), "walk", "default"));
        // here should be a travelled event
        result.add(new PersonArrivalEvent(71, Id.createPersonId("test-person"), Id.createLinkId("l1"), "walk"));
        result.add(new ActivityStartEvent(71, Id.createPersonId("test-person"), Id.createLinkId("l3"), null, "end", null));

        return result;
    }
}
