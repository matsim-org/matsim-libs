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
package org.matsim.vsp.ev.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.facilities.Facility;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.ElectricVehicle;

import java.util.List;

/**
 * This network Routing module adds stages for re-charging into the Route.
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 *
 * @author jfbischoff
 */

public final class EVNetworkRoutingModule implements RoutingModule {

    private final String mode;

    private final Network network;
    private final NetworkRoutingModule delegate;
    private final ElectricFleet electricFleet;

    public EVNetworkRoutingModule(
            final String mode,
            final Network network,
            NetworkRoutingModule delegate,
            ElectricFleet evs
    ) {
        Gbl.assertNotNull(network);
        this.delegate = delegate;
        this.network = network;
        this.mode = mode;
        this.electricFleet = evs;
    }

    @Override
    public List<? extends PlanElement> calcRoute(final Facility fromFacility, final Facility toFacility, final double departureTime,
                                                 final Person person) {
        List<? extends PlanElement> basicRoute = delegate.calcRoute(fromFacility, toFacility, departureTime, person);
        Id<ElectricVehicle> evId = Id.create(person.getId(), ElectricVehicle.class);
        if (!electricFleet.getElectricVehicles().containsKey(evId)) {
            return basicRoute;
        } else {
            Leg basicLeg = (Leg) basicRoute.get(0);
            basicLeg.getRoute().getDistance();
            return null;
        }
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return EmptyStageActivityTypes.INSTANCE;
    }

    @Override
    public String toString() {
        return "[NetworkRoutingModule: mode=" + this.mode + "]";
    }

}
