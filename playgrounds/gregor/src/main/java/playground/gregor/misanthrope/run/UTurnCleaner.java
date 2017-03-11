package playground.gregor.misanthrope.run;/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

public class UTurnCleaner implements BeforeMobsimListener {
    private static void handlePlan(Plan pl, Scenario sc) {
        Activity a0 = (Activity) pl.getPlanElements().get(0);

        Leg leg = (Leg) pl.getPlanElements().get(1);


        Id<Link> frst = a0.getLinkId();
        Link frstL = sc.getNetwork().getLinks().get(frst);

        LinkNetworkRouteImpl route = ((LinkNetworkRouteImpl) leg.getRoute());

        if (route.getLinkIds().size() == 0) {
            return;
        }
        Id<Link> scnd = route.getLinkIds().get(0);
        Link scndL = sc.getNetwork().getLinks().get(scnd);

//        route.set

        if (frstL.getFromNode() == scndL.getToNode()) {//&& frstL.getToNode().getOutLinks().size() == 1) { //U-turn in dead end street
            a0.setLinkId(scnd);
            LinkNetworkRouteImpl newRoute = new LinkNetworkRouteImpl(route.getLinkIds().get(0), route.getLinkIds().subList(1, route.getLinkIds().size()), route.getEndLinkId());
//            newRoute.setRouteDescription(route.getRouteDescription());
            newRoute.setTravelCost(route.getTravelCost());
            newRoute.setVehicleId(route.getVehicleId());
            newRoute.setDistance(route.getDistance());
            newRoute.setTravelTime(route.getTravelTime());
            leg.setRoute(newRoute);
        }


    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        Population pop = event.getServices().getScenario().getPopulation();
        pop.getPersons().values().parallelStream().forEach(p -> {
            Plan pl = p.getSelectedPlan();
            UTurnCleaner.handlePlan(pl, event.getServices().getScenario());
        });
    }
}
