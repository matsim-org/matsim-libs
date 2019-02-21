package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public final class TravelDisutilities {
	
	public static TravelDisutility createBaseDisutility(final CarrierVehicleTypes vehicleTypes, final TravelTime travelTime){

		return new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
				CarrierVehicleType type = vehicleTypes.getVehicleTypes().get(vehicle.getType().getId());
				if(type == null) throw new IllegalStateException("vehicle "+vehicle.getId()+" has no type");
				double tt = travelTime.getLinkTravelTime(link, time, person, vehicle);
				return type.getVehicleCostInformation().getPerDistanceUnit()*link.getLength() + type.getVehicleCostInformation().getPerTimeUnit()*tt;
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				double minDisutility = Double.MAX_VALUE;
				double free_tt = link.getLength()/link.getFreespeed();
				for(CarrierVehicleType type : vehicleTypes.getVehicleTypes().values()){
					double disu = type.getVehicleCostInformation().getPerDistanceUnit()*link.getLength() + type.getVehicleCostInformation().getPerTimeUnit()*free_tt;
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
                Id<VehicleType> typeId = vehicle.getType().getId();
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
