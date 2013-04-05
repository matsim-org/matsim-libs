package vehicles;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class VehicleTransportCostsPerLink implements TravelDisutility{

	private TravelTime travelTime;
	
	private CarrierVehicleTypes vehicleTypes;
	
	public VehicleTransportCostsPerLink(TravelTime travelTime, CarrierVehicleTypes vehicleTypes) {
		super();
		this.travelTime = travelTime;
		this.vehicleTypes = vehicleTypes;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		CarrierVehicleType type = vehicleTypes.getVehicleTypes().get(vehicle.getType().getId());
		double base = link.getLength()*type.getVehicleCostInformation().perDistanceUnit + 
			travelTime.getLinkTravelTime(link, time, person, vehicle)*type.getVehicleCostInformation().perTimeUnit;
		return base;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}
	
}
