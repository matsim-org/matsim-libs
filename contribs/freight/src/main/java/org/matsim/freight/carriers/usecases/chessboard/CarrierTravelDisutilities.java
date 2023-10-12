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

package org.matsim.freight.carriers.usecases.chessboard;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public final class CarrierTravelDisutilities{

	public static TravelDisutility createBaseDisutility(final CarrierVehicleTypes vehicleTypes, final TravelTime travelTime){

		return new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
				VehicleType type = vehicleTypes.getVehicleTypes().get(vehicle.getType().getId() );
				if(type == null) throw new IllegalStateException("vehicle "+vehicle.getId()+" has no type");
				double tt = travelTime.getLinkTravelTime(link, time, person, vehicle);
				return type.getCostInformation().getCostsPerMeter()*link.getLength() + type.getCostInformation().getCostsPerSecond()*tt;
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				double minDisutility = Double.MAX_VALUE;
				double free_tt = link.getLength()/link.getFreespeed();
				for( VehicleType type : vehicleTypes.getVehicleTypes().values()){
					double disu = type.getCostInformation().getCostsPerMeter()*link.getLength() + type.getCostInformation().getCostsPerSecond()*free_tt;
					if(disu < minDisutility) minDisutility=disu;
				}
				return minDisutility;
			}
		};
	}


    public static TravelDisutility withToll(final TravelDisutility base, final VehicleTypeDependentRoadPricingCalculator roadPricing){

        return new TravelDisutility() {

            @Override
            public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                double costs = base.getLinkTravelDisutility(link, time, person, vehicle);
                Id<org.matsim.vehicles.VehicleType> typeId = vehicle.getType().getId();
                double toll = roadPricing.getTollAmount(typeId, link, time);
                return costs + toll;
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return base.getLinkMinimumTravelDisutility(link);
            }

        };

    }



}
