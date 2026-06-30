package org.matsim.vehicles;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleUtilsTest {

	@Test
	void copyFromToCopiesVehicleTypeDimensionsAndCapacity() {
		VehicleType source = VehicleUtils.createVehicleType(Id.create("source", VehicleType.class));
		source.setLength(7.5);
		source.setWidth(1.2);
		source.setMaximumVelocity(22.2);
		source.setPcuEquivalents(2.0);
		source.setNetworkMode(TransportMode.car);
		source.getCapacity()
				.setSeats(1)
				.setStandingRoom(3)
				.setOther(4600.)
				.setVolumeInCubicMeters(12.)
				.setWeightInTons(4.6);

		VehicleType target = VehicleUtils.createVehicleType(Id.create("target", VehicleType.class));

		VehicleUtils.copyFromTo(source, target);

		assertEquals(7.5, target.getLength());
		assertEquals(1.2, target.getWidth());
		assertEquals(22.2, target.getMaximumVelocity());
		assertEquals(2.0, target.getPcuEquivalents());
		assertEquals(TransportMode.car, target.getNetworkMode());
		assertEquals(1, target.getCapacity().getSeats());
		assertEquals(3, target.getCapacity().getStandingRoom());
		assertEquals(4600., target.getCapacity().getOther());
		assertEquals(12., target.getCapacity().getVolumeInCubicMeters());
		assertEquals(4.6, target.getCapacity().getWeightInTons());
	}
}
