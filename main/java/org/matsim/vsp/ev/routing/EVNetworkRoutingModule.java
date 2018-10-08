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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.util.LinkProvider;
import org.matsim.contrib.util.StraightLineKnnFinder;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.facilities.Facility;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.ElectricVehicle;

import java.util.*;

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
    private final ChargingInfrastructure chargingInfrastructure;
    private final Random random = MatsimRandom.getLocalInstance();
    private static final LinkProvider<Charger> CHARGER_TO_LINK = charger -> charger.getLink();
    private static final LinkProvider<Link> LINK_TO_LINK = l -> l;


    public EVNetworkRoutingModule(
            final String mode,
            final Network network,
            NetworkRoutingModule delegate,
            ElectricFleet evs,
            ChargingInfrastructure chargingInfrastructure
    ) {
        Gbl.assertNotNull(network);
        this.delegate = delegate;
        this.network = network;
        this.mode = mode;
        this.electricFleet = evs;
        this.chargingInfrastructure = chargingInfrastructure;
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
            ElectricVehicle ev = electricFleet.getElectricVehicles().get(evId);

            Map<Link, Double> estimatedEnergyConsumption = estimateConsumption(ev, basicLeg);
            double estimatedOverallConsumption = estimatedEnergyConsumption.values().stream().mapToDouble(Number::doubleValue).sum();
            double capacity = ev.getBattery().getCapacity() * (0.8 + random.nextDouble() * 0.18);
            double numberOfStops = Math.floor(estimatedOverallConsumption / capacity);
            if (numberOfStops < 1) {
                return basicRoute;
            } else {
                List<Link> stopLocations = new ArrayList<>();
                double currentConsumption = 0;
                for (Map.Entry<Link, Double> e : estimatedEnergyConsumption.entrySet()) {
                    currentConsumption += e.getValue();
                    if (currentConsumption > capacity) {
                        stopLocations.add(e.getKey());
                        currentConsumption = 0;
                    }
                }
                List<PlanElement> stagedRoute = new ArrayList<>();
                Facility lastFrom = fromFacility;
                for (Link stopLocation : stopLocations) {

                    StraightLineKnnFinder<Link, Charger> straightLineKnnFinder = new StraightLineKnnFinder(2, l -> (Link) l, CHARGER_TO_LINK);
                    List<Charger> nearestChargers = straightLineKnnFinder.findNearest(stopLocation, chargingInfrastructure.getChargers().values().stream());
                    Charger selectedCharger = nearestChargers.get(0);
                    Facility nexttoFacility = new LinkWrapperFacility(selectedCharger.getLink());
                    stagedRoute.addAll(delegate.calcRoute(lastFrom, nexttoFacility, departureTime, person));
                    Activity chargeAct = PopulationUtils.createActivityFromCoordAndLinkId("car charging", selectedCharger.getCoord(), selectedCharger.getLink().getId());
                    chargeAct.setMaximumDuration(1800);
                    stagedRoute.add(chargeAct);
                    lastFrom = nexttoFacility;
                }
                stagedRoute.addAll(delegate.calcRoute(lastFrom, toFacility, departureTime, person));


                return stagedRoute;

            }

        }
    }

    private Map<Link, Double> estimateConsumption(ElectricVehicle ev, Leg basicLeg) {
        Map<Link, Double> consumptions = new LinkedHashMap<>();
        NetworkRoute route = (NetworkRoute) basicLeg.getRoute();
        List<Link> links = NetworkUtils.getLinks(network, route.getLinkIds());

        for (Link l : links) {
            double freeSpeedTravelTime = l.getLength() / l.getFreespeed();
            double consumption = ev.getDriveEnergyConsumption().calcEnergyConsumption(l, freeSpeedTravelTime) + ev.getAuxEnergyConsumption().calcEnergyConsumption(freeSpeedTravelTime);
            consumptions.put(l, consumption);
        }
        return consumptions;
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
