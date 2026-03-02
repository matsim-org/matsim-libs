package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.mockito.Mockito;

import java.util.Collection;

public class QVehicleImplTest {

	@Test
	public void testAddPassenger() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		PassengerAgent p1 = Mockito.mock(PassengerAgent.class);
		Mockito.when(p1.getId()).thenReturn(Id.createPersonId("p1"));

		boolean added = qVehicle.addPassenger(p1);
		Assertions.assertTrue(added);
		Assertions.assertEquals(1, qVehicle.getPassengers().size());
		Assertions.assertTrue(qVehicle.getPassengers().contains(p1));
	}

	@Test
	public void testAddPassengerBeyondCapacity() {
		VehicleType type = VehicleUtils.createVehicleType(Id.create("type", VehicleType.class));
		type.getCapacity().setSeats(1);
		type.getCapacity().setStandingRoom(0);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), type);
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		PassengerAgent p1 = Mockito.mock(PassengerAgent.class);
		Mockito.when(p1.getId()).thenReturn(Id.createPersonId("p1"));
		PassengerAgent p2 = Mockito.mock(PassengerAgent.class);
		Mockito.when(p2.getId()).thenReturn(Id.createPersonId("p2"));

		Assertions.assertTrue(qVehicle.addPassenger(p1));
		Assertions.assertFalse(qVehicle.addPassenger(p2));
		Assertions.assertEquals(1, qVehicle.getPassengers().size());
	}

	@Test
	public void testRemovePassengerSameInstance() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		PassengerAgent p1 = Mockito.mock(PassengerAgent.class);
		Mockito.when(p1.getId()).thenReturn(Id.createPersonId("p1"));

		qVehicle.addPassenger(p1);
		boolean removed = qVehicle.removePassenger(p1);

		Assertions.assertTrue(removed);
		Assertions.assertEquals(0, qVehicle.getPassengers().size());
	}

	@Test
	public void testRemovePassengerDifferentInstanceSameId() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		var pId = Id.createPersonId("p1");

		PassengerAgent p1 = Mockito.mock(PassengerAgent.class);
		Mockito.when(p1.getId()).thenReturn(pId);

		PassengerAgent p1Alternative = Mockito.mock(PassengerAgent.class);
		Mockito.when(p1Alternative.getId()).thenReturn(pId);

		qVehicle.addPassenger(p1);

		// Try to remove using the alternative instance
		boolean removed = qVehicle.removePassenger(p1Alternative);

		Assertions.assertTrue(removed, "Passenger should be removed even if instance is different but ID is the same");
		Assertions.assertEquals(0, qVehicle.getPassengers().size());
	}

	@Test
	public void testRemovePassengerNotPresent() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		PassengerAgent p1 = Mockito.mock(PassengerAgent.class);
		Mockito.when(p1.getId()).thenReturn(Id.createPersonId("p1"));

		boolean removed = qVehicle.removePassenger(p1);
		Assertions.assertFalse(removed);
	}

	@Test
	public void testGetPassengersUnmodifiable() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		Collection<? extends PassengerAgent> passengers = qVehicle.getPassengers();
		PassengerAgent p1 = Mockito.mock(PassengerAgent.class);

		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			// disable inspection as we try to add to an immutable collection,
			//noinspection unchecked, rawtypes
			((Collection) passengers).add(p1);
		});
	}

	@Test
	public void testPassengerCapacityDefault() {
		VehicleType type = Mockito.mock(VehicleType.class);
		Mockito.when(type.getCapacity()).thenReturn(null);
		Mockito.when(type.getId()).thenReturn(Id.create("type", VehicleType.class));
		Vehicle vehicle = Mockito.mock(Vehicle.class);
		Mockito.when(vehicle.getType()).thenReturn(type);
		Mockito.when(vehicle.getId()).thenReturn(Id.createVehicleId("v1"));

		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);
		Assertions.assertEquals(4, qVehicle.getPassengerCapacity());
	}

	@Test
	public void testPassengerCapacityFromVehicleType() {
		VehicleType type = VehicleUtils.createVehicleType(Id.create("type", VehicleType.class));
		type.getCapacity().setSeats(10);
		type.getCapacity().setStandingRoom(5);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), type);

		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);
		Assertions.assertEquals(15, qVehicle.getPassengerCapacity());
	}

	@Test
	public void testSizeInEquivalentsWithDefaultScaling() {
		VehicleType type = VehicleUtils.createVehicleType(Id.create("type", VehicleType.class));
		type.setPcuEquivalents(2.5);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), type);

		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);
		Assertions.assertEquals(2.5, qVehicle.getSizeInEquivalents(), 0.00001);
	}

	@Test
	public void testSizeInEquivalentsWithCustomScaling() {
		VehicleType type = VehicleUtils.createVehicleType(Id.create("type", VehicleType.class));
		type.setPcuEquivalents(2.0);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), type);

		double scalingFactor = 1.5;
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle, scalingFactor);

		// 2.0 * 1.5 = 3.0
		Assertions.assertEquals(3.0, qVehicle.getSizeInEquivalents(), 0.00001);
	}

	@Test
	public void testSetAndGetDriver() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		org.matsim.core.mobsim.framework.MobsimDriverAgent driver = Mockito.mock(org.matsim.core.mobsim.framework.MobsimDriverAgent.class);
		var driverId = Id.createPersonId("d1");
		Mockito.when(driver.getId()).thenReturn(driverId);

		qVehicle.setDriver(driver);
		Assertions.assertEquals(driver, qVehicle.getDriver());
	}

	@Test
	public void testSetDifferentDriverThrowsException() {
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("v1"), VehicleUtils.createDefaultVehicleType());
		QVehicleImpl qVehicle = new QVehicleImpl(vehicle);

		org.matsim.core.mobsim.framework.MobsimDriverAgent driver1 = Mockito.mock(org.matsim.core.mobsim.framework.MobsimDriverAgent.class);
		Mockito.when(driver1.getId()).thenReturn(Id.createPersonId("d1"));

		org.matsim.core.mobsim.framework.MobsimDriverAgent driver2 = Mockito.mock(org.matsim.core.mobsim.framework.MobsimDriverAgent.class);
		Mockito.when(driver2.getId()).thenReturn(Id.createPersonId("d2"));

		qVehicle.setDriver(driver1);

		Assertions.assertThrows(RuntimeException.class, () -> qVehicle.setDriver(driver2));
	}
}
