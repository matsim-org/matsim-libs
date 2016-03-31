/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import java.util.*;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.*;
import org.matsim.facilities.Facility;


public class DynRoutingModule
    implements RoutingModule
{
    private final String mode;


    public DynRoutingModule(String mode)
    {
        this.mode = mode;
    }


    @Override
    public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility,
            double departureTime, Person person)
    {
        Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
        route.setDistance(Double.NaN);
        route.setTravelTime(Double.NaN);

        Leg leg = new LegImpl(mode);
        leg.setDepartureTime(departureTime);
        leg.setTravelTime(Double.NaN);
        leg.setRoute(route);

        return Arrays.asList(leg);
    }


    @Override
    public StageActivityTypes getStageActivityTypes()
    {
        return EmptyStageActivityTypes.INSTANCE;
    }
}
