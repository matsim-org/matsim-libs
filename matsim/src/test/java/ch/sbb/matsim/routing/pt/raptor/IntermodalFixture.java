/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a simple scenario with a single transit line connecting two
 * areas (one around coordinates 10000/10000, the other around 50000/10000).
 * In each area, several transit stops with different characteristics are
 * located along the transit line:
 *
 * <pre>
 * [n]  stop facility
 * ---  link, transit route
 *
 *
 * [0]---[1]---[2]---[3]---------------------[4]---[5]---[6]---[7]
 *  B                 B                             B           B
 *        H           H                             H
 *
 *  B: bikeAccessible=true
 *  H: hub=true
 *
 * </pre>
 *
 * The stops have the following attributes:
 * <ul>
 *     <li>0: bikeAccessible=true   hub=false</li>
 *     <li>1: bikeAccessible=false  hub=true</li>
 *     <li>2: bikeAccessible=false  hub=false</li>
 *     <li>3: bikeAccessible=true   hub=true</li>
 *     <li>4: bikeAccessible=false  hub=false</li>
 *     <li>5: bikeAccessible=true   hub=true</li>
 *     <li>6: (none)</li>
 *     <li>7: bikeAccessible=true</li>
 * </ul>
 *
 * The line is running every 10 minutes between 06:00 and 08:00 from [0] to [7].
 *
 *
 * @author mrieser / SBB
 */
class IntermodalFixture {

    final SwissRailRaptorConfigGroup srrConfig;
    final Config config;
    final Scenario scenario;
    final Person dummyPerson;

    IntermodalFixture() {
        this.srrConfig = new SwissRailRaptorConfigGroup();
        this.config = ConfigUtils.createConfig(this.srrConfig);
		this.config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
        this.scenario = ScenarioUtils.createScenario(this.config);

        TransitSchedule schedule = this.scenario.getTransitSchedule();
        TransitScheduleFactory sf = schedule.getFactory();

        Id<Link>[] ptLinkIds = new Id[8];
        for (int i = 0; i < ptLinkIds.length; i++) {
            ptLinkIds[i] = Id.create("pt_" + i, Link.class);
        }

        TransitStopFacility[] stops = new TransitStopFacility[8];
        stops[0] = sf.createTransitStopFacility(Id.create(0, TransitStopFacility.class), new Coord( 9000, 10000), false);
        stops[1] = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord( 9500, 10000), false);
        stops[2] = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(10000, 10000), false);
        stops[3] = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(10500, 10000), false);
        stops[4] = sf.createTransitStopFacility(Id.create(4, TransitStopFacility.class), new Coord(49500, 10000), false);
        stops[5] = sf.createTransitStopFacility(Id.create(5, TransitStopFacility.class), new Coord(50000, 10000), false);
        stops[6] = sf.createTransitStopFacility(Id.create(6, TransitStopFacility.class), new Coord(50500, 10000), false);
        stops[7] = sf.createTransitStopFacility(Id.create(7, TransitStopFacility.class), new Coord(51000, 10000), false);

        for (int i = 0; i < stops.length; i++) {
            TransitStopFacility stop = stops[i];
            stop.setLinkId(ptLinkIds[i]);
            schedule.addStopFacility(stop);
        }

        stops[0].getAttributes().putAttribute("bikeAccessible", "true");
        stops[1].getAttributes().putAttribute("bikeAccessible", "false");
        stops[2].getAttributes().putAttribute("bikeAccessible", "false");
        stops[3].getAttributes().putAttribute("bikeAccessible", "true");
        stops[4].getAttributes().putAttribute("bikeAccessible", "false");
        stops[5].getAttributes().putAttribute("bikeAccessible", "true");
        stops[7].getAttributes().putAttribute("bikeAccessible", "true");

        stops[0].getAttributes().putAttribute("hub", false);
        stops[1].getAttributes().putAttribute("hub", true);
        stops[2].getAttributes().putAttribute("hub", false);
        stops[3].getAttributes().putAttribute("hub", true);
        stops[4].getAttributes().putAttribute("hub", false);
        stops[5].getAttributes().putAttribute("hub", true);

        for (int i = 0; i < stops.length; i++) {
            if ("true".equals(stops[i].getAttributes().getAttribute("bikeAccessible"))) {
                stops[i].getAttributes().putAttribute("accessLinkId_bike", "bike_" + i);
            }
        }

        TransitLine line = sf.createTransitLine(Id.create("oneway", TransitLine.class));

        List<TransitRouteStop> rStops = new ArrayList<>();
        for (int i = 0; i < stops.length; i++) {
            double arrivalTime = i * 120 + (i > 3 ? 1200 : 0);
            double departureTime = arrivalTime + 60;
            rStops.add(sf.createTransitRouteStop(stops[i], arrivalTime, departureTime));
        }
        NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(ptLinkIds[0], Arrays.copyOfRange(ptLinkIds, 1, 7), ptLinkIds[7]);
        TransitRoute route = sf.createTransitRoute(Id.create("goEast", TransitRoute.class), networkRoute, rStops, "train");

        for (int i = 0; i < 13; i++) {
            Departure d = sf.createDeparture(Id.create(i, Departure.class), 6*3600 + i * 600);
            d.setVehicleId(Id.create(i, Vehicle.class));
            route.addDeparture(d);
        }

        line.addRoute(route);
        schedule.addTransitLine(line);

        // add transit vehicles, required for integration test
        Vehicles transitVehicles = this.scenario.getTransitVehicles();
        VehiclesFactory vf = transitVehicles.getFactory();
        VehicleType busType = vf.createVehicleType(Id.create("bus", VehicleType.class));
        busType.getCapacity().setSeats(30);
        busType.getCapacity().setStandingRoom(20);
        transitVehicles.addVehicleType(busType);
        for (int i = 0; i < 13; i++) {
            transitVehicles.addVehicle(vf.createVehicle(Id.create(i, Vehicle.class), busType));
        }

        this.dummyPerson = this.scenario.getPopulation().getFactory().createPerson(Id.create("dummy", Person.class));

        Network network = this.scenario.getNetwork();
        NetworkFactory nf = network.getFactory();
        Node[] nodes = new Node[stops.length + 1];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = nf.createNode(Id.create(i, Node.class), new Coord(10000 + 5000 * i, 10000));
            network.addNode(nodes[i]);
        }
        for (int i = 0; i < stops.length; i++) {
            Link link = nf.createLink(Id.create("pt_" + i, Link.class), nodes[i], nodes[i+1]);
            network.addLink(link);
            link = nf.createLink(Id.create("bike_" + i, Link.class), nodes[i], nodes[i+1]);
            network.addLink(link);
        }

    }
}
