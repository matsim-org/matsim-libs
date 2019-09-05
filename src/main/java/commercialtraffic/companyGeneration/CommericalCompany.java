package commercialtraffic.companyGeneration;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;

public class CommericalCompany {

	Carrier carrier;
	String companyId;
	String carrierId;
	Double openingTime;
	Double closingTime;
	Double serviceDuration;
	String serviceType;
	int totalFleetSize;
	int fleetIterator = 0;
	Id<Link> companyLinkId;

	CommericalCompany(String companyId, Double openingTime, Double closingTime, double serviceDuration,
			String serviceType, Id<Link> companyLinkId) {
		// A company is also the carrier
		this.companyId = companyId;
		this.carrierId = serviceType+"_"+companyId;
		this.carrier = CarrierImpl.newInstance(Id.create(carrierId, Carrier.class));
		this.openingTime = openingTime;
		this.closingTime = closingTime;
		this.serviceType = serviceType;
		this.companyLinkId = companyLinkId;
		this.carrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE);

	}

	void addVehicle(Id<Link> linkId, int vehicleType, Double openingTime, Double closingTime) {

		this.carrier.getCarrierCapabilities().getCarrierVehicles().add(getVehicle(Id.createVehicleId(carrierId + "_" + fleetIterator + "_vehTyp_" + vehicleType), linkId, companyId, vehicleType,openingTime,closingTime));
		this.carrier.getCarrierCapabilities().getVehicleTypes().add(createType(vehicleType));
		fleetIterator++;
	}
	
	public void addService(String serviceId, Id<Link> linkId,double startTime, double endTime, double serviceDuration)
	{
		//FAIL! double trueEndTime = endTime-serviceDuration;
		double trueEndTime = endTime;
		CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create(serviceId, CarrierService.class ), linkId);
		//TODO: Please change to non fixed number
		serviceBuilder.setCapacityDemand( 1 );
		serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(startTime, trueEndTime) );
		serviceBuilder.setServiceDuration( serviceDuration);
		CarrierService service = serviceBuilder.build();
		carrier.getServices().add(service);
	}

	public static CarrierVehicle getVehicle(Id<?> id, Id<Link> homeId, String depot, int vehicleType, double openingTime, double closingTime) {
				
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder
				.newInstance(Id.create((id.toString()), Vehicle.class), homeId);
		vBuilder.setEarliestStart(openingTime);
		vBuilder.setLatestEnd(closingTime);
		vBuilder.setType(createType(vehicleType));
		//TODO: We could add vehicle length for each vehicleType according to KID at this stage
		return vBuilder.build();
	}

	public static CarrierVehicleType createType(int vehicleType) {

		CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder
				.newInstance(Id.create(vehicleType, VehicleType.class));
		
		switch (vehicleType) {
		case 1:
			
			typeBuilder.setCapacity(1);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(200.0 / 3.6);
			return typeBuilder.build();
		case 2:

			typeBuilder.setCapacity(25);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(160.0 / 3.6);
			return typeBuilder.build();
		case 3:

			typeBuilder.setCapacity(150);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(160.0 / 3.6);
			return typeBuilder.build();
		case 4:

			typeBuilder.setCapacity(400);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(93.0 / 3.6);
			return typeBuilder.build();
		case 5:

			typeBuilder.setCapacity(400);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(93.0 / 3.6);
			return typeBuilder.build();
		case 6:

			typeBuilder.setCapacity(80);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(110.0 / 3.6);
			return typeBuilder.build();
		case 7:

			typeBuilder.setCapacity(80);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(80.0 / 3.6);
			return typeBuilder.build();
		case 8:

			typeBuilder.setCapacity(80);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(93.0 / 3.6);
			return typeBuilder.build();
		case 9:

			typeBuilder.setCapacity(50);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(40.0 / 3.6);
			return typeBuilder.build();
		case 10:

			typeBuilder.setCapacity(100);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(160.0 / 3.6);
			return typeBuilder.build();
		case 11:

			typeBuilder.setCapacity(100);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(160.0 / 3.6);
			return typeBuilder.build();
		default:

			typeBuilder.setCapacity(100);
			typeBuilder.setFixCost(80.0);
			typeBuilder.setCostPerDistanceUnit(0.00047);
			typeBuilder.setCostPerTimeUnit(0.008);
			typeBuilder.setMaxVelocity(160.0 / 3.6);
			return typeBuilder.build();
		}

	}

	// public static CarrierVehicleType createType(int vehicleType) {
	// CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder
	// .newInstance(Id.create(vehicleType, VehicleType.class));
	// // TODO: We use right now only one type for all vehicles
	// if (true) {
	//
	// typeBuilder.setCapacity(100);
	// typeBuilder.setFixCost(80.0);
	// typeBuilder.setCostPerDistanceUnit(0.00047);
	// typeBuilder.setCostPerTimeUnit(0.008);
	//
	// }
	// return typeBuilder.build();
	//
	// }

	//
	// carrier2.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
	// carrier2.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1),
	// Id.createLinkId(62912), "dpdlehrte"));
	// carrier2.getCarrierCapabilities().getVehicleTypes().add(createLightType());
	//
	//
	// carrier3.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
	// carrier3.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1),
	// Id.createLinkId(340834), "dhlhannover2"));
	// carrier3.getCarrierCapabilities().getVehicleTypes().add(createLightType());
	//
	//
	// carrier4.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
	// carrier4.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1),
	// Id.createLinkId(62912), "dpdlehrte2"));
	// carrier4.getCarrierCapabilities().getVehicleTypes().add(createLightType());

}
