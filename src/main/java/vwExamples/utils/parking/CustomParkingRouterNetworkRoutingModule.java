/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package vwExamples.utils.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import parking.ParkingZone;
import parking.ZonalLinkParkingInfo;

import java.util.*;
import java.util.Map.Entry;


/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 *
 * @author thibautd, nagel
 */
public final class CustomParkingRouterNetworkRoutingModule implements RoutingModule {
    private static final Logger log = Logger.getLogger(CustomParkingRouterNetworkRoutingModule.class);

    public static final class ParkingAccessEgressStageActivityTypes implements StageActivityTypes {
        @Override
        public boolean isStageActivity(String activityType) {
            if (CustomParkingRouterNetworkRoutingModule.stageActivityType.equals(activityType) ||
                    CustomParkingRouterNetworkRoutingModule.parkingStageActivityType.equals(activityType) || activityType.endsWith("interaction")
                    ) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ParkingAccessEgressStageActivityTypes)) {
                return false;
            }
            ParkingAccessEgressStageActivityTypes other = (ParkingAccessEgressStageActivityTypes) obj;
            return other.isStageActivity(CustomParkingRouterNetworkRoutingModule.stageActivityType);
        }

        @Override
        public int hashCode() {
            return CustomParkingRouterNetworkRoutingModule.stageActivityType.hashCode();// TODO update this too
        }
    }


    private final String mode;
    private final PopulationFactory populationFactory;

    private final Network network;
    private final LeastCostPathCalculator routeAlgo;
    private final static String stageActivityType = TransportMode.car + " interaction";
    public final static String parkingStageActivityType = TransportMode.car + " parkingSearch";

    private ZonalLinkParkingInfo zoneToLinks;
    private final Random random = MatsimRandom.getLocalInstance();
    private final Map<Id<Person>, Link> personToParkLink = new HashMap<>();

    public CustomParkingRouterNetworkRoutingModule(
            final String mode,
            final PopulationFactory populationFactory,
            final Network network,
            final LeastCostPathCalculator routeAlgo,
            PlansCalcRouteConfigGroup calcRouteConfig,
            ZonalLinkParkingInfo zoneToLinks) {
        Gbl.assertNotNull(network);
        Gbl.assertIf(network.getLinks().size() > 0); // otherwise network for mode probably not defined
        this.network = network;
        this.routeAlgo = routeAlgo;
        this.mode = mode;
        this.populationFactory = populationFactory;
        if (!calcRouteConfig.isInsertingAccessEgressWalk()) {
            throw new RuntimeException("trying to use access/egress but not switched on in config.  "
                    + "currently not supported; there are too many other problems");
        }
        this.zoneToLinks = zoneToLinks;
    }

    @Override
    public synchronized List<? extends PlanElement> calcRoute(
            final Facility fromFacility,
            final Facility toFacility,
            final double departureTime,
            final Person person) {
        // I need this "synchronized" since I want mobsim agents to be able to call this during the mobsim.  So when the
        // mobsim is multi-threaded, multiple agents might call this here at the same time.  kai, nov'17

        Gbl.assertNotNull(fromFacility);
        Gbl.assertNotNull(toFacility);

        Link accessActLink = decideOnLink(fromFacility);
        // so that agent walk back to where car is parked.
        if (personToParkLink.containsKey(person.getId())) {
            accessActLink = personToParkLink.remove(person.getId());
//            log.warn("Person " + person.getId() + " is walking to " + accessActLink.getId() + " to get the car.");
            // increase the parking capacity counter
            this.zoneToLinks.getParkingZone(accessActLink)
                            .updateLinkParkingCapacity(accessActLink, +1);// can be dependent on PCU of the vehicle
        }

        Link egressActLink = decideOnLink(toFacility);

        double now = departureTime;

        List<PlanElement> result = new ArrayList<>();

        // === access:
        {
            if (fromFacility.getCoord() != null) { // otherwise the trip starts directly on the link; no need to bushwhack

                Coord accessActCoord = accessActLink.getToNode().getCoord();
                // yyyy think about better solution: this may generate long walks along the link.
                // (e.g. orthogonal projection)
                Gbl.assertNotNull(accessActCoord);

                Leg accessLeg = this.populationFactory.createLeg(TransportMode.access_walk);
                accessLeg.setDepartureTime(now);
                now += routeBushwhackingLeg(person,
                        accessLeg,
                        fromFacility.getCoord(),
                        accessActCoord,
                        now,
                        accessActLink.getId(),
                        accessActLink.getId(),
                        this.populationFactory);
                // yyyy might be possible to set the link ids to null. kai & dominik, may'16

                result.add(accessLeg);
//				log.warn( accessLeg );

                final Activity interactionActivity = createInteractionActivity(accessActCoord, accessActLink.getId());
                result.add(interactionActivity);
//				log.warn( interactionActivity );
            }
        }

        // === compute the network leg:
        {
            Leg newLeg = this.populationFactory.createLeg(this.mode);
            newLeg.setDepartureTime(now);
            now += routeLeg(person, newLeg, accessActLink, egressActLink, now);

            result.add(newLeg);
//			log.warn( newLeg );
        }
        // === compute another network leg (from destination of above link to another nearest link
        {
            Leg newLeg = this.populationFactory.createLeg(this.mode);
            newLeg.setDepartureTime(now);
            // get egressActLink from zone and avg park time from Zone
            Link parkEndLink = getNClosesedParkLink(egressActLink, person);
            if (parkEndLink == null ) {
                // vehicle is outside the study area or has garage
            } else {
//                log.warn("Person " + person.getId() + " started looking for parking on link " + egressActLink.getId() + " and found parking on link " + parkEndLink
//                        .getId());
                now += routeLeg(person, newLeg, egressActLink, parkEndLink, now);

                // an activity will help in identifying the start of the parking search
                final Activity interactionActivity = PopulationUtils.createActivityFromCoordAndLinkId(this.parkingStageActivityType,
                        egressActLink.getToNode().getCoord(),
                        egressActLink.getId());
                interactionActivity.setMaximumDuration(0.0);
                result.add(interactionActivity);
                result.add(newLeg);

                this.personToParkLink.put(person.getId(), parkEndLink);
                egressActLink = parkEndLink; // <- this is new egressActLink. Amit Feb'18
//                log.warn( newLeg );
                // decrease the parking capacity counter
                this.zoneToLinks.getParkingZone(parkEndLink)
                                .updateLinkParkingCapacity(parkEndLink,
                                        -1);// can be dependent on PCU of the vehicle type
            }
        }

        // === egress:
        {
            if (toFacility.getCoord() != null) { // otherwise the trip ends directly on the link; no need to bushwhack

                Coord egressActCoord = egressActLink.getToNode().getCoord();
                Gbl.assertNotNull(egressActCoord);

                final Activity interactionActivity = createInteractionActivity(egressActCoord, egressActLink.getId());
                result.add(interactionActivity);
//				log.warn( interactionActivity );

                Leg egressLeg = this.populationFactory.createLeg(TransportMode.egress_walk);
                egressLeg.setDepartureTime(now);
                now += routeBushwhackingLeg(person,
                        egressLeg,
                        egressActCoord,
                        toFacility.getCoord(),
                        now,
                        egressActLink.getId(),
                        decideOnLink(toFacility).getId(),
                        this.populationFactory);
                result.add(egressLeg);
//				log.warn( egressLeg );
            }
//			log.warn( "===" );
        }

        return result;
    }

    private Link getParkLink(Link egressActLink, Person person) {
        ParkingZone parkingZone = this.zoneToLinks.getParkingZone(egressActLink);
        if (parkingZone==null) return null;
        double personSpecificZoneRandom = getPersonSpecificGarageRandom(person, parkingZone);

        if (personSpecificZoneRandom < parkingZone.getGarageProbability()) {
        	return null;
        }
        
        final WeightedRandomSelection<Id<Link>> wrs = new WeightedRandomSelection<>();                                                                         
        parkingZone.getLinkParkingProbabilities().forEach((k,v)->wrs.add(k, v));
        return (network.getLinks().get(wrs.select()));
        }
    

    private Link getNClosesedParkLink(Link egressActLink, Person person) {
        int n = 5;

        Map<Id<Link>, Double> possibleParkingLinks = new HashMap<>();

        for (int j = 0; j <= n; j = j + 1) {

        	ParkingZone parkingZone = this.zoneToLinks.getParkingZone(egressActLink);
            if (parkingZone==null) return null;
            double personSpecificZoneRandom = getPersonSpecificGarageRandom(person, parkingZone);

            if (personSpecificZoneRandom < parkingZone.getGarageProbability()) {
            	return null;
            }

            final WeightedRandomSelection<Id<Link>> wrs = new WeightedRandomSelection<>();
            parkingZone.getLinkParkingProbabilities().forEach((k, v) -> wrs.add(k, v));

            Link parkLink = network.getLinks().get(wrs.select());
            double distance = DistanceUtils.calculateDistance(parkLink.getCoord(), egressActLink.getCoord());

            //System.out.println("Found possible parking with distance: " +distance);

            possibleParkingLinks.put(parkLink.getId(), distance);

        }

        Entry<Id<Link>, Double> min = null;
        for (Entry<Id<Link>, Double> entry : possibleParkingLinks.entrySet()) {
            if (min == null || min.getValue() > entry.getValue()) {
                min = entry;
            }
        }

        //System.out.println("Used parking with distance: " +min.getValue());
        return network.getLinks().get(min.getKey());

    }


    private double getPersonSpecificGarageRandom(Person person, ParkingZone parkingZone) {
        Double zoneRandom = (Double) person.getAttributes().getAttribute(parkingZone.getId().toString());
        if (zoneRandom == null) {
            zoneRandom = random.nextDouble();
            person.getAttributes().putAttribute(parkingZone.getId().toString(), zoneRandom);
        }
        return zoneRandom;
    }

    private Link decideOnLink(final Facility fromFacility) {
        Link accessActLink = null;
        if (fromFacility.getLinkId() != null) {
            accessActLink = this.network.getLinks().get(fromFacility.getLinkId());
            // i.e. if street address is in mode-specific subnetwork, I just use that, and do not search for another (possibly closer)
            // other link.

        }

        if (accessActLink == null) {
            // this is the case where the postal address link is NOT in the subnetwork, i.e. does NOT serve the desired mode,
            // OR the facility does not have a street address link in the first place.

            if (fromFacility.getCoord() == null) {
                throw new RuntimeException(
                        "access/egress bushwhacking leg not possible when neither facility link id nor facility coordinate given");
            }

            accessActLink = NetworkUtils.getNearestLink(this.network, fromFacility.getCoord());
            if (accessActLink == null) {
                int ii = 0;
                for (Link link : this.network.getLinks().values()) {
                    if (ii == 10) {
                        break;
                    }
                    ii++;
                    log.warn(link);
                }
            }
            Gbl.assertNotNull(accessActLink);
        }
        return accessActLink;
    }

    private Activity createInteractionActivity(final Coord interactionCoord, final Id<Link> interactionLink) {
        Activity act = PopulationUtils.createActivityFromCoordAndLinkId(this.stageActivityType,
                interactionCoord,
                interactionLink);
        act.setMaximumDuration(0.0);
        return act;
    }


    private static double routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
                                               Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf) {
        // I don't think that it makes sense to use a RoutingModule for this, since that again makes assumptions about how to
        // map facilities, and if you follow through to the teleportation routers one even finds activity wrappers, which is yet another
        // complication which I certainly don't want here.  kai, dec'15

        // dpLinkId, arLinkId need to be in Route for lots of code to function.   So I am essentially putting in the "street address"
        // for completeness. Note that if we are walking to a parked car, this can be different from the car link id!!  kai, dec'15

        // make simple assumption about distance and walking speed
        double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);

        // create an empty route, but with realistic travel time
        Route route = pf.getRouteFactories().createRoute(Route.class, dpLinkId, arLinkId);

        double beelineDistanceFactor = 1.3;
        double networkTravelSpeed = 2.0;
        // yyyyyy take this from config!

        double estimatedNetworkDistance = dist * beelineDistanceFactor;
        int travTime = (int) (estimatedNetworkDistance / networkTravelSpeed);
        route.setTravelTime(travTime);
        route.setDistance(estimatedNetworkDistance);
        leg.setRoute(route);
        leg.setDepartureTime(depTime);
        leg.setTravelTime(travTime);
        return travTime;
    }


    @Override
    public StageActivityTypes getStageActivityTypes() {
        return new ParkingAccessEgressStageActivityTypes();
    }

    @Override
    public String toString() {
        return "[NetworkRoutingModule: mode=" + this.mode + "]";
    }


    /*package (Tests)*/ double routeLeg(Person person, Leg leg, Link fromLink, Link toLink, double depTime) {
        double travTime = 0;

        Node startNode = fromLink.getToNode();    // start at the end of the "current" link
        Node endNode = toLink.getFromNode(); // the target is the start of the link

        if (toLink != fromLink) { // (a "true" route)

            Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
            if (path == null)
                throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

            NetworkRoute route = this.populationFactory.getRouteFactories()
                                                       .createRoute(NetworkRoute.class,
                                                               fromLink.getId(),
                                                               toLink.getId());
            route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
            route.setTravelTime((int) path.travelTime);
            route.setTravelCost(path.travelCost);
            route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, this.network));
            leg.setRoute(route);
            travTime = (int) path.travelTime;

        } else {
            // create an empty route == staying on place if toLink == endLink
            // note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
            NetworkRoute route = this.populationFactory.getRouteFactories()
                                                       .createRoute(NetworkRoute.class,
                                                               fromLink.getId(),
                                                               toLink.getId());
            route.setTravelTime(0);
            route.setDistance(0.0);
            leg.setRoute(route);
            travTime = 0;
        }

        leg.setDepartureTime(depTime);
        leg.setTravelTime(travTime);

        return travTime;
    }

}
