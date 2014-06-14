package usecases.chessboard;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class TravelDisutilities {
	
	public static TravelDisutility createBaseDisutility(final CarrierVehicleTypes vehicleTypes, final TravelTime travelTime){
		
		return new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
				CarrierVehicleType type = vehicleTypes.getVehicleTypes().get(vehicle.getType().getId());
				if(type == null) throw new IllegalStateException("vehicle "+vehicle.getId()+" has no type");
				double tt = travelTime.getLinkTravelTime(link, time, person, vehicle);
				return type.getVehicleCostInformation().perDistanceUnit*link.getLength() + type.getVehicleCostInformation().perTimeUnit*tt;
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				double minDisutility = Double.MAX_VALUE;
				double free_tt = link.getLength()/link.getFreespeed();
				for(CarrierVehicleType type : vehicleTypes.getVehicleTypes().values()){
					double disu = type.getVehicleCostInformation().perDistanceUnit*link.getLength() + type.getVehicleCostInformation().perTimeUnit*free_tt;
					if(disu < minDisutility) minDisutility=disu;
				}
				return minDisutility;
			}
		};
	}
	


}
