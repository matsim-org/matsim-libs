/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.ownMobsimAgentUsingRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.List;

/**
 * @author nagel
 */
class MyGuidance {

    private final TripRouter router;
    private final Scenario scenario;

    MyGuidance(TripRouter router, Scenario scenario) {
        this.router = router;
        this.scenario = scenario;
    }

    public Id<Link> getBestOutgoingLink(Id<Link> linkId, Id<Link> destinationLinkId, double now) {
        Person person = null; // does this work?
        double departureTime = now;
        String mainMode = TransportMode.car;
        Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(this.scenario.getNetwork().getLinks().get(linkId));
        Facility<ActivityFacility> toFacility = new LinkWrapperFacility(this.scenario.getNetwork().getLinks().get(destinationLinkId));
        List<? extends PlanElement> trip = router.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

        Leg leg = (Leg) trip.get(0);  // test: either plan element 0 or 1 will be a car leg

        NetworkRoute route = (NetworkRoute) leg.getRoute();

        return route.getLinkIds().get(0); // entry number 0 should be link connected to next intersection (?)
    }

}
