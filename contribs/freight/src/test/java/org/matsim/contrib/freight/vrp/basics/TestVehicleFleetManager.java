package org.matsim.contrib.freight.vrp.basics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl;

public class TestVehicleFleetManager extends TestCase{
	
	VehicleFleetManager fleetManager;
	
	Vehicle v1;
	
	Vehicle v2;
	
	public void setUp(){
		List<Vehicle> vehicles = new ArrayList<Vehicle>();
		v1 = VehicleImpl.getFactory().createtStandardVehicle();
		v2 = VehicleImpl.getFactory().createVehicle("foo", "foo", VehicleImpl.getFactory().createType("foo", 30, VehicleImpl.getFactory().createVehicleCostParams(0,10,10)));
		vehicles.add(v1);
		vehicles.add(v2);
		fleetManager = new VehicleFleetManagerImpl(vehicles);	
	}
	
	public void testGetTypes(){
		Collection<String> types = fleetManager.getAvailableVehicleTypes();
		assertEquals(2, types.size());
	}
	
	public void testGetVehicle(){
		Vehicle v = fleetManager.getEmptyVehicle("standard");
		assertEquals(v.getId(), v1.getId());
	}
	
	public void testLock(){
		fleetManager.lock(v1);
		Collection<String> types = fleetManager.getAvailableVehicleTypes();
		assertEquals(1, types.size());
	}
	
	public void testLockTwice(){
		fleetManager.lock(v1);
		Collection<String> types = fleetManager.getAvailableVehicleTypes();
		assertEquals(1, types.size());
		try{
			fleetManager.lock(v1);
			Collection<String> types_ = fleetManager.getAvailableVehicleTypes();
			assertFalse(true);
		}
		catch(IllegalStateException e){
			assertTrue(true);
		}
	}
	
	public void testGetTypesWithout(){
		Collection<String> types = fleetManager.getAvailableVehicleTypes("standard");
		assertEquals("foo", types.iterator().next());
		assertEquals(1, types.size());
	}
	
	public void testUnlock(){
		fleetManager.lock(v1);
		Collection<String> types = fleetManager.getAvailableVehicleTypes();
		assertEquals(1, types.size());
		fleetManager.unlock(v1);
		Collection<String> types_ = fleetManager.getAvailableVehicleTypes();
		assertEquals(2, types_.size());
	}

}
