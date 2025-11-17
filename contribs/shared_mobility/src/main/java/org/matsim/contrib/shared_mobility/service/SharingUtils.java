package org.matsim.contrib.shared_mobility.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModes;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingVehicleSource;
import org.matsim.contrib.shared_mobility.service.events.SharingReservingEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingDropoffEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingFailedDropoffEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingFailedPickupEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingPickupEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingVehicleEvent;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.common.base.Verify;

public class SharingUtils {

	public static final Logger LOG = LogManager.getLogger(SharingUtils.class);

	static public final String BOOKING_ACTIVITY = "sharing booking interaction";
	static public final String PICKUP_ACTIVITY = "sharing pickup interaction";
	static public final String DROPOFF_ACTIVITY = "sharing dropoff interaction";

	static public final String STATION_ID_ATTRIBUTE = "sharing:stationId";
	static public final String SERVICE_ID_ATTRIBUTE = "sharing:service";

	static public final String MODE_PREFIX = "sharing_";

	static public final double INTERACTION_DURATION = 60.0;

	public enum SHARING_VEHICLE_STATES {
		RESERVED, BOOKED, IDLE
	};

	static public void setStationId(Activity activity, Id<SharingStation> stationId) {
		Verify.verify(activity.getType().equals(PICKUP_ACTIVITY) || activity.getType().equals(DROPOFF_ACTIVITY));
		activity.getAttributes().putAttribute(STATION_ID_ATTRIBUTE, stationId.toString());
	}

	static public Id<SharingStation> getStationId(Activity activity) {
		Verify.verify(activity.getType().equals(PICKUP_ACTIVITY) || activity.getType().equals(DROPOFF_ACTIVITY));
		String stationId = (String) activity.getAttributes().getAttribute(STATION_ID_ATTRIBUTE);
		Verify.verifyNotNull(stationId);
		return Id.create(stationId, SharingStation.class);
	}

	static public void setServiceId(Activity activity, Id<SharingService> serviceId) {
		Verify.verify(activity.getType().equals(BOOKING_ACTIVITY) || activity.getType().equals(PICKUP_ACTIVITY)
				|| activity.getType().equals(DROPOFF_ACTIVITY));
		activity.getAttributes().putAttribute(SERVICE_ID_ATTRIBUTE, serviceId.toString());
	}

	static public Id<SharingService> getServiceId(Activity activity) {
		Verify.verify(activity.getType().equals(PICKUP_ACTIVITY) || activity.getType().equals(DROPOFF_ACTIVITY));
		String serviceId = (String) activity.getAttributes().getAttribute(SERVICE_ID_ATTRIBUTE);
		Verify.verifyNotNull(serviceId);
		return Id.create(serviceId, SharingService.class);
	}

	static public String getServiceMode(Id<SharingService> id) {
		return MODE_PREFIX + id;
	}

	static public String getServiceMode(SharingServiceConfigGroup serviceConfig) {
		return getServiceMode(serviceConfig.getId());
	}

	public static QSimComponentsConfigurator configureQSim(SharingConfigGroup sharingConfig) {
		return components -> {
			for (SharingServiceConfigGroup serviceConfig : sharingConfig.getServices()) {
				components.addComponent(SharingModes.mode(getServiceMode(serviceConfig)));
			}
		};
	}

	public static void addEventMappers(MatsimEventsReader reader) {
		reader.addCustomEventMapper(SharingVehicleEvent.TYPE, SharingVehicleEvent::convert);
		reader.addCustomEventMapper(SharingPickupEvent.TYPE, SharingPickupEvent::convert);
		reader.addCustomEventMapper(SharingFailedPickupEvent.TYPE, SharingFailedPickupEvent::convert);
		reader.addCustomEventMapper(SharingDropoffEvent.TYPE, SharingDropoffEvent::convert);
		reader.addCustomEventMapper(SharingFailedDropoffEvent.TYPE, SharingFailedDropoffEvent::convert);
		reader.addCustomEventMapper(SharingReservingEvent.TYPE, SharingReservingEvent::convert);
	}

	public static VehicleType getOrCreateAndAddVehicleType(SharingServiceConfigGroup serviceConfig,
			Vehicles vehicles) {
		Id<VehicleType> vehicleTypeId = serviceConfig.getVehicleTypeId();
		VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
		if (vehicleType == null) {
			SharingVehicleSource.LOG.warn("VehicleType with id " + vehicleTypeId
					+ " not found in vehicles file, but required for sharing service "
					+ serviceConfig.getId() + ", creating a default one for mode ", serviceConfig.getMode());
			vehicleType = VehicleUtils.createVehicleType(vehicleTypeId, serviceConfig.getMode());
			vehicles.addVehicleType(vehicleType);
		}
		return vehicleType;
	}

	public static Vehicle getOrCreateAndAddVehicle(Id<Vehicle> vehicleId, VehicleType vehicleType, Vehicles vehicles) {
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
		if (vehicle != null) {
			if (vehicle.getType() != vehicleType) {
				throw new IllegalStateException("Vehicle with id " + vehicleId
						+ " already exists but has a different vehicle type ("
						+ vehicle.getType().getId() + ") than expected ("
						+ vehicleType.getId() + ").");
			}
			LOG.debug("Vehicle with id {} already exists. Skipping creation of sharing vehicle.", vehicleId);
		} else {
			// create vehicle with type and add to vehicles
			vehicle = vehicles.getFactory().createVehicle(vehicleId, vehicleType);
			vehicles.addVehicle(vehicle);
		}
		return vehicle;
	}

	public static Vehicle getOrCreateAndAddDummyVehicle(SharingServiceConfigGroup serviceConfig, Vehicles vehicles) {
		Id<Vehicle> routingVehicleId = Id.createVehicleId(
				serviceConfig.getId().toString() + "_" + serviceConfig.getMode() + "_replanningRoutingDummyVehicle");
		Vehicle routingVehicle = vehicles.getVehicles().get(routingVehicleId);
		if (routingVehicle != null) {
			return routingVehicle;
		} else {
			VehicleType vehicleType = getOrCreateAndAddVehicleType(serviceConfig, vehicles);
			routingVehicle = vehicles.getFactory().createVehicle(routingVehicleId, vehicleType);
			vehicles.addVehicle(routingVehicle);
		}
		return routingVehicle;
	}

}
