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
package org.matsim.contrib.ev.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.contrib.util.LinkProvider;
import org.matsim.contrib.util.StraightLineKnnFinder;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

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
    private final RoutingModule delegate;
    private final ElectricFleet electricFleet;
    private final ChargingInfrastructure chargingInfrastructure;
    private final Random random = MatsimRandom.getLocalInstance();
    private static final LinkProvider<Charger> CHARGER_TO_LINK = charger -> charger.getLink();
    private static final LinkProvider<Link> LINK_TO_LINK = l -> l;
    private final TravelTime travelTime;
    private final String stageActivityType;
    private final String vehicleSuffix;


    private final class EVCharingStageActivityType implements StageActivityTypes {
        @Override
        public boolean isStageActivity(String activityType) {
            if (EVNetworkRoutingModule.this.stageActivityType.equals(activityType)) {
                return true;
            } else if (activityType.endsWith("interaction")) {
                return true;
            }
            {
                return false;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EVCharingStageActivityType)) {
                return false;
            }
            EVCharingStageActivityType other = (EVCharingStageActivityType) obj;
            return other.isStageActivity(EVNetworkRoutingModule.this.stageActivityType);
        }

        @Override
        public int hashCode() {
            return EVNetworkRoutingModule.this.stageActivityType.hashCode();
        }
    }

    public EVNetworkRoutingModule(
            final String mode,
            final Network network,
            RoutingModule delegate,
            ElectricFleet evs,
            ChargingInfrastructure chargingInfrastructure, TravelTime travelTime) {
        this.travelTime = travelTime;
        Gbl.assertNotNull(network);
        this.delegate = delegate;
        this.network = network;
        this.mode = mode;
        this.electricFleet = evs;
        this.chargingInfrastructure = chargingInfrastructure;
        stageActivityType = mode + VehicleChargingHandler.CHARGING_IDENTIFIER;
        this.vehicleSuffix = mode == TransportMode.car ? "" : "_" + mode;
    }

    @Override
    public List<? extends PlanElement> calcRoute(final Facility fromFacility, final Facility toFacility, final double departureTime,
                                                 final Person person) {

        List<? extends PlanElement> basicRoute = delegate.calcRoute(fromFacility, toFacility, departureTime, person);
        Id<ElectricVehicle> evId = Id.create(person.getId() + vehicleSuffix, ElectricVehicle.class);
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
                double lastArrivaltime = departureTime;
                for (Link stopLocation : stopLocations) {

                    StraightLineKnnFinder<Link, Charger> straightLineKnnFinder = new StraightLineKnnFinder(2, l -> (Link) l, CHARGER_TO_LINK);
                    List<Charger> nearestChargers = straightLineKnnFinder.findNearest(stopLocation, chargingInfrastructure.getChargers().values().stream().filter(charger -> ev.getChargingTypes().contains(charger.getChargerType())));
                    Charger selectedCharger = nearestChargers.get(random.nextInt(1));
                    Facility nexttoFacility = new LinkWrapperFacility(selectedCharger.getLink());
                    if (nexttoFacility.getLinkId().equals(lastFrom.getLinkId())) {
                        continue;
                    }
                    List<? extends PlanElement> routeSegment = delegate.calcRoute(lastFrom, nexttoFacility, lastArrivaltime, person);
                    Leg lastLeg = (Leg) routeSegment.get(0);
                    lastArrivaltime = lastLeg.getDepartureTime() + lastLeg.getTravelTime();
                    stagedRoute.add(lastLeg);
                    Activity chargeAct = PopulationUtils.createActivityFromCoordAndLinkId(stageActivityType, selectedCharger.getCoord(), selectedCharger.getLink().getId());
                    double estimatedChargingTime = (ev.getBattery().getCapacity() * .8) / selectedCharger.getPower();
                    chargeAct.setMaximumDuration(estimatedChargingTime);
                    lastArrivaltime += chargeAct.getMaximumDuration();
                    stagedRoute.add(chargeAct);
                    lastFrom = nexttoFacility;
                }
                stagedRoute.addAll(delegate.calcRoute(lastFrom, toFacility, lastArrivaltime, person));


                return stagedRoute;

            }

        }
    }

    private Map<Link, Double> estimateConsumption(ElectricVehicle ev, Leg basicLeg) {
        Map<Link, Double> consumptions = new LinkedHashMap<>();
        NetworkRoute route = (NetworkRoute) basicLeg.getRoute();
        List<Link> links = NetworkUtils.getLinks(network, route.getLinkIds());

        for (Link l : links) {
            double travelT = travelTime.getLinkTravelTime(l, basicLeg.getDepartureTime(), null, null);
            double consumption = ev.getDriveEnergyConsumption().calcEnergyConsumption(l, travelT) + ev.getAuxEnergyConsumption().calcEnergyConsumption(travelT);
            consumptions.put(l, consumption);
        }
        return consumptions;
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return new EVCharingStageActivityType();
    }

    @Override
    public String toString() {
        return "[NetworkRoutingModule: mode=" + this.mode + "]";
    }

}
