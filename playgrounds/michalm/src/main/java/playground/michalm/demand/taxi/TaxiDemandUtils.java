/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.demand.taxi;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;


public class TaxiDemandUtils
{
    public static void preprocessPlansBasedOnCoordsOnly(Scenario scenario)
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        for (Person p : scenario.getPopulation().getPersons().values()) {
            List<PlanElement> planElements = p.getSelectedPlan().getPlanElements();

            ActivityImpl fromActivity = (ActivityImpl)planElements.get(0);
            Link fromLink = network.getNearestLink(fromActivity.getCoord());
            fromActivity.setLinkId(fromLink.getId());

            ActivityImpl toActivity = (ActivityImpl)planElements.get(2);
            Link toLink = network.getNearestLink(toActivity.getCoord());
            toActivity.setLinkId(toLink.getId());

            Leg leg = (Leg)p.getSelectedPlan().getPlanElements().get(1);
            leg.setRoute(new GenericRouteImpl(fromLink.getId(), toLink.getId()));
        }
    }
}
