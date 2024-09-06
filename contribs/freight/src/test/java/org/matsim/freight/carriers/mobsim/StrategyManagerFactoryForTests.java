/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.mobsim;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.controler.CarrierReRouteVehicles;
import org.matsim.freight.carriers.controler.CarrierStrategyManager;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

@Disabled
public class StrategyManagerFactoryForTests implements Provider<CarrierStrategyManager>{

    @Inject Network network;
    @Inject Map<String, TravelTime> travelTimes;

    static class MyTravelCosts implements TravelDisutility {

        private final TravelTime travelTime;

        public MyTravelCosts(TravelTime travelTime) {
            super();
            this.travelTime = travelTime;
        }

        @Override
        public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
            return disutility(link.getLength(),  travelTime.getLinkTravelTime(link, time, null, null));
        }

        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return disutility(link.getLength(),  link.getLength() / link.getFreespeed());
        }

        private double disutility(double distance, double time) {
            double cost_per_m = 1.0 / 1000.0;
            double cost_per_s = 50.0 / (60.0 * 60.0);
            return distance * cost_per_m + time * cost_per_s;
        }
    }

    @Override
    public CarrierStrategyManager get() {

        final LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(network, new MyTravelCosts(travelTimes.get(TransportMode.car)), travelTimes.get(TransportMode.car));

        GenericPlanStrategyImpl<CarrierPlan, Carrier> planStrat_reRoutePlan = new GenericPlanStrategyImpl<>( new BestPlanSelector<>() );
        planStrat_reRoutePlan.addStrategyModule(new CarrierReRouteVehicles.Factory(router, network, travelTimes.get(TransportMode.car )).build() );

        CarrierStrategyManager stratManager = CarrierControlerUtils.createDefaultCarrierStrategyManager();

        stratManager.addStrategy(planStrat_reRoutePlan, null, 1.0);

        return stratManager;
    }

}
