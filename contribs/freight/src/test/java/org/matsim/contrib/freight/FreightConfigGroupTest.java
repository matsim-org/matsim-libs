package org.matsim.contrib.freight;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.freight.FreightConfigGroup.UseDistanceConstraintForTourPlanning;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author mrieser / Simunto
 */
public class FreightConfigGroupTest {

	@Test
	public void test_allParametersAreWrittenToXml() {
		FreightConfigGroup freight = new FreightConfigGroup();
		Map<String, String> params = freight.getParams();

		Assert.assertTrue(params.containsKey(FreightConfigGroup.CARRIERS_FILE));
		Assert.assertTrue(params.containsKey(FreightConfigGroup.CARRIERS_VEHICLE_TYPE));
		Assert.assertTrue(params.containsKey(FreightConfigGroup.VEHICLE_ROUTING_ALGORITHM));
		Assert.assertTrue(params.containsKey(FreightConfigGroup.TRAVEL_TIME_SLICE_WIDTH));
		Assert.assertTrue(params.containsKey(FreightConfigGroup.USE_DISTANCE_CONSTRAINT));
	}

	@Test
	public void test_configXmlCanBeParsed() {
		FreightConfigGroup freight = new FreightConfigGroup();
		Config config = ConfigUtils.createConfig(freight);

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/config_v2.dtd\">\n" +
				"<config>\n" +
				"  <module name=\"freight\" >\t\n" +
				"    <param name=\"carriersFile\" value=\"/path/to/carriers.xml\" />\n" +
				"    <param name=\"carriersVehicleTypeFile\" value=\"/path/to/carriersVehicleTypes.xml\" />\n" +
				"    <param name=\"vehicleRoutingAlgorithmFile\" value=\"/path/to/carriersRoutingAlgorithm.xml\" />\n" +
				"    <param name=\"travelTimeSliceWidth\" value=\"3600\" />\n" +
				"    <param name=\"useDistanceConstraintForTourPlanning\" value=\"basedOnEnergyConsumption\" />\n" +
				"  </module>\n" +
				"</config>";

		InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		new ConfigReader(config).parse(is);

		Assert.assertEquals("/path/to/carriers.xml", freight.getCarriersFile());
		Assert.assertEquals("/path/to/carriersVehicleTypes.xml", freight.getCarriersVehicleTypesFile());
		Assert.assertEquals("/path/to/carriersRoutingAlgorithm.xml", freight.getVehicleRoutingAlgorithmFile());
		Assert.assertEquals(3600.0, freight.getTravelTimeSliceWidth(), 1e-8);
		Assert.assertEquals(UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption, freight.getUseDistanceConstraintForTourPlanning());
	}

}