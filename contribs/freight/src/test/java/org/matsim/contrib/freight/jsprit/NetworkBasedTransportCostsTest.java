package org.matsim.contrib.freight.jsprit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


public class NetworkBasedTransportCostsTest {


	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test_whenAddingTwoDifferentVehicleTypes_itMustAccountForThem(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = utils.getClassInputDirectory() + "network.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILENAME);

		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(network);
		builder.addVehicleTypeSpecificCosts("type1", 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts("type2", 20.0, 0.0, 4.0);
		NetworkBasedTransportCosts c = builder.build();

		Vehicle vehicle1 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type1 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn("type1");
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("type2");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		Assert.assertEquals(20000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assert.assertEquals(40000.0, c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assert.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);
		Assert.assertEquals(20000.0, c.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}

	@Test
	public void test_whenVehicleTypeNotKnow_throwException(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = utils.getClassInputDirectory() + "network.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILENAME);

		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder = NetworkBasedTransportCosts.Builder.newInstance(network);
		builder.addVehicleTypeSpecificCosts("type1", 10.0, 0.0, 2.0);
		builder.addVehicleTypeSpecificCosts("type2", 20.0, 0.0, 4.0);
		NetworkBasedTransportCosts c = builder.build();

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("typeNotKnown");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		try{
			c.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2);
		}
		catch(IllegalStateException e){ Assert.assertTrue(true); }

	}

	@Test
	public void test_whenAddingTwoVehicleTypesViaConstructor_itMustAccountForThat(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		String NETWORK_FILENAME = utils.getClassInputDirectory() + "network.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILENAME);

		VehicleType vehType1 = VehicleUtils.getFactory().createVehicleType(Id.create( "type1", VehicleType.class ));

		CostInformation costInformation1 = vehType1.getCostInformation() ;
		costInformation1.setFixedCost( 0.0 );
		costInformation1.setCostsPerMeter( 2.0 );
		costInformation1.setCostsPerSecond( 0.0 );

		VehicleType vehType2 = VehicleUtils.getFactory().createVehicleType(Id.create( "type2", VehicleType.class ));

		CostInformation costInformation = vehType2.getCostInformation() ;
		costInformation.setFixedCost( 0.0 );
		costInformation.setCostsPerMeter( 4.0 );
		costInformation.setCostsPerSecond( 0.0 );

		Network network = scenario.getNetwork();
		NetworkBasedTransportCosts.Builder builder =
				NetworkBasedTransportCosts.Builder.newInstance(network,Arrays.asList(vehType1,vehType2));
		NetworkBasedTransportCosts networkBasedTransportCosts = builder.build();

		Vehicle vehicle1 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type1 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type1.getMaxVelocity()).thenReturn(5.0);
		when(type1.getTypeId()).thenReturn("type1");
		when(vehicle1.getType()).thenReturn(type1);
		when(vehicle1.getId()).thenReturn("vehicle1");

		Vehicle vehicle2 = mock(Vehicle.class);
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType type2 = mock( com.graphhopper.jsprit.core.problem.vehicle.VehicleType.class );
		when(type2.getMaxVelocity()).thenReturn(5.0);
		when(type2.getTypeId()).thenReturn("type2");
		when(vehicle2.getType()).thenReturn(type2);
		when(vehicle2.getId()).thenReturn("vehicle2");

		Assert.assertEquals(20000.0, networkBasedTransportCosts.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle1), 0.01);
		Assert.assertEquals(40000.0, networkBasedTransportCosts.getTransportCost(Location.newInstance("20"), Location.newInstance("21"), 0.0, mock(Driver.class), vehicle2), 0.01);
		Assert.assertEquals(20000.0, networkBasedTransportCosts.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle1), 0.01);
		Assert.assertEquals(20000.0, networkBasedTransportCosts.getDistance(Location.newInstance("6"), Location.newInstance("21"), 0.0, vehicle2), 0.01);
	}

}
