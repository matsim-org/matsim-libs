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
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

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
	public void addGroceryService(String serviceId, Id<Link> linkId,double startTime, double endTime, int Capacity)
	{
		//FAIL! double trueEndTime = endTime-serviceDuration;
		double trueEndTime = endTime;
		CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create(serviceId, CarrierService.class ), linkId);
		//TODO: Please change to non fixed number
		serviceBuilder.setCapacityDemand( Capacity );
		serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(startTime, trueEndTime) );
		serviceBuilder.setServiceDuration( 300);
		CarrierService service = serviceBuilder.build();
		carrier.getServices().add(service);
	}
	
	public double getClosingTime(String companyId) {
		
		
		return closingTime;
		
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

	public static VehicleType createType(int vehicleTypeIndex) {

		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create(vehicleTypeIndex, VehicleType.class));

		switch (vehicleTypeIndex) {
		case 1:
			vehicleType.getCapacity().setOther(1);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(200.0 / 3.6);
			return vehicleType;
		case 2:

			vehicleType.getCapacity().setOther(25);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(160.0 / 3.6);
		case 3:

			vehicleType.getCapacity().setOther(150);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(160.0 / 3.6);
		case 4:

			vehicleType.getCapacity().setOther(400);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(93.0 / 3.6);
		case 5:

			vehicleType.getCapacity().setOther(400);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(93.0 / 3.6);
		case 6:

			vehicleType.getCapacity().setOther(80);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(110.0 / 3.6);
		case 7:

			vehicleType.getCapacity().setOther(80);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(80.0 / 3.6);
		case 8:

			vehicleType.getCapacity().setOther(80);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(93.0 / 3.6);
		case 9:

			vehicleType.getCapacity().setOther(50);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(40.0 / 3.6);
		case 10:

			vehicleType.getCapacity().setOther(100);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(160.0 / 3.6);
		case 11:

			vehicleType.getCapacity().setOther(100);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(160.0 / 3.6);
		default:

			vehicleType.getCapacity().setOther(100);
			vehicleType.getCostInformation().setFixedCost(80.0);
			vehicleType.getCostInformation().setCostsPerMeter(0.00047);
			vehicleType.getCostInformation().setCostsPerSecond(0.008);
			vehicleType.setMaximumVelocity(160.0 / 3.6);
		}
		return vehicleType;
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
