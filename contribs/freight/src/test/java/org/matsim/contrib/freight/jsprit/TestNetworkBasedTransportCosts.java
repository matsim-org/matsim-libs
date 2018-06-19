package org.matsim.contrib.freight.jsprit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleType.VehicleCostInformation;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;



public class TestNetworkBasedTransportCosts extends MatsimTestCase{


	@Test
	public void test_whenAddingTwoDifferentVehicleTypes_itMustAccountForThem(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = getClassInputDirectory() + "network.xml";
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(NETWORK_FILENAME);
		
		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(network);
		builder.addVehicleTypeSpecificCosts("type1", 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts("type2", 20.0, 0.0, 4.0);	
		NetworkBasedTransportCosts c = builder.build();
		
		Vehicle vehicle1 = mock(Vehicle.class);
		VehicleType type1 = mock(VehicleType.class);
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn("type1");
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");
		
		Vehicle vehicle2 = mock(Vehicle.class);
		VehicleType type2 = mock(VehicleType.class);
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("type2");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");
		
		assertEquals(20000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		assertEquals(40000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);

	}
	
	@Test
	public void test_whenVehicleTypeNotKnow_throwException(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = getClassInputDirectory() + "network.xml";
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(NETWORK_FILENAME);
		
		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(network);
		builder.addVehicleTypeSpecificCosts("type1", 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts("type2", 20.0, 0.0, 4.0);	
		NetworkBasedTransportCosts c = builder.build();
		
		Vehicle vehicle2 = mock(Vehicle.class);
		VehicleType type2 = mock(VehicleType.class);
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("typeNotKnown");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");
		
		try{
			c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2);
		}
		catch(IllegalStateException e){ assertTrue(true); }
		
	}
	
	@Test
	public void test_whenAddingTwoVehicleTypesViaConstructor_itMustAccountForThat(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = getClassInputDirectory() + "network.xml";
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(NETWORK_FILENAME);
		
		CarrierVehicleType vtype1 = mock(CarrierVehicleType.class);
		VehicleCostInformation param1 = new VehicleCostInformation(0.0, 2.0, 0.0);
		when(vtype1.getVehicleCostInformation()).thenReturn(param1);
		when(vtype1.getId()).thenReturn(Id.create("type1", org.matsim.vehicles.VehicleType.class));
		
		CarrierVehicleType vtype2 = mock(CarrierVehicleType.class);
		VehicleCostInformation param2 = new VehicleCostInformation(0.0, 4.0, 0.0);
		when(vtype2.getVehicleCostInformation()).thenReturn(param2);
		when(vtype2.getId()).thenReturn(Id.create("type2", org.matsim.vehicles.VehicleType.class));
		
		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = 
				NetworkBasedTransportCosts.Builder.newInstance(network,Arrays.asList(vtype1,vtype2));
		NetworkBasedTransportCosts c = builder.build();
		
		Vehicle vehicle1 = mock(Vehicle.class);
		VehicleType type1 = mock(VehicleType.class);
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn("type1");
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");
		
		Vehicle vehicle2 = mock(Vehicle.class);
		VehicleType type2 = mock(VehicleType.class);
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("type2");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");
		
		assertEquals(20000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		assertEquals(40000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);

	}

}
