package org.matsim.contrib.parking.parkingsearchparameterization;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler.VehicleArrival;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingVehicleHandlerTest {

	@Test
	void onlyCarsSearchForParkingByDefault() {
		ParkingVehicleHandler handler = new ParkingVehicleHandler();

		assertEquals(VehicleArrival.PARKING, handler.handleVehicleArrival(createVehicle(TransportMode.car), null));
		assertEquals(VehicleArrival.ALLOWED, handler.handleVehicleArrival(createVehicle(TransportMode.truck), null));
	}

	@Test
	void configuredModesSearchForParking() {
		ParkingVehicleHandler handler = new ParkingVehicleHandler(Set.of(TransportMode.truck, TransportMode.motorcycle));

		assertEquals(VehicleArrival.PARKING, handler.handleVehicleArrival(createVehicle(TransportMode.truck), null));
		assertEquals(VehicleArrival.PARKING, handler.handleVehicleArrival(createVehicle(TransportMode.motorcycle), null));
		assertEquals(VehicleArrival.ALLOWED, handler.handleVehicleArrival(createVehicle(TransportMode.car), null));
	}

	private static QVehicle createVehicle(String networkMode) {
		return new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(networkMode),
			VehicleUtils.createVehicleType(Id.create(networkMode, VehicleType.class), networkMode)));
	}
}
