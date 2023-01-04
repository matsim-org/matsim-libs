package org.matsim.contrib.freight.carrier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CarrierVehicleTypeLoaderTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private CarrierVehicleTypes types;
	private Carriers carriers;

	@Before
	public void setUp() throws Exception{
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(utils.getClassInputDirectory() + "vehicleTypes.xml");
		carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, types ).readFile(utils.getClassInputDirectory() + "carrierPlansEquils.xml" );
	}

	@Test
	public void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarrierUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("lightVehicle"));

		VehicleType vehicleTypeLoaded = v.getType();
		Assert.assertNotNull(vehicleTypeLoaded);

		Assert.assertEquals("light", vehicleTypeLoaded.getId().toString());
		Assert.assertEquals(15, vehicleTypeLoaded.getCapacity().getOther(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(20, vehicleTypeLoaded.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.35, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("gasoline", vehicleTypeLoaded.getEngineInformation().getFuelType().toString());
		Assert.assertEquals(0.02, VehicleUtils.getFuelConsumption(vehicleTypeLoaded), MatsimTestUtils.EPSILON);
	}

	@Test
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarrierUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("mediumVehicle"));

		VehicleType vehicleTypeLoaded = v.getType();
		Assert.assertNotNull(vehicleTypeLoaded);

		Assert.assertEquals("medium", vehicleTypeLoaded.getId().toString());
		Assert.assertEquals(30, vehicleTypeLoaded.getCapacity().getOther(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(50, vehicleTypeLoaded.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.4, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("gasoline", vehicleTypeLoaded.getEngineInformation().getFuelType().toString());
		Assert.assertEquals(0.02, VehicleUtils.getFuelConsumption(vehicleTypeLoaded), MatsimTestUtils.EPSILON);

	}

}
