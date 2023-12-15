/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author mrieser / Simunto
 */
public class FreightCarriersConfigGroupTest {

	@Test
	void test_allParametersAreWrittenToXml() {
		FreightCarriersConfigGroup freight = new FreightCarriersConfigGroup();
		Map<String, String> params = freight.getParams();

		Assertions.assertTrue(params.containsKey(FreightCarriersConfigGroup.CARRIERS_FILE));
		Assertions.assertTrue(params.containsKey(FreightCarriersConfigGroup.CARRIERS_VEHICLE_TYPE));
		Assertions.assertTrue(params.containsKey(FreightCarriersConfigGroup.VEHICLE_ROUTING_ALGORITHM));
		Assertions.assertTrue(params.containsKey(FreightCarriersConfigGroup.TRAVEL_TIME_SLICE_WIDTH));
		Assertions.assertTrue(params.containsKey(FreightCarriersConfigGroup.USE_DISTANCE_CONSTRAINT));
	}

	@Test
	void test_configXmlCanBeParsed() {
		FreightCarriersConfigGroup freight = new FreightCarriersConfigGroup();
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

		Assertions.assertEquals("/path/to/carriers.xml", freight.getCarriersFile());
		Assertions.assertEquals("/path/to/carriersVehicleTypes.xml", freight.getCarriersVehicleTypesFile());
		Assertions.assertEquals("/path/to/carriersRoutingAlgorithm.xml", freight.getVehicleRoutingAlgorithmFile());
		Assertions.assertEquals(3600.0, freight.getTravelTimeSliceWidth(), 1e-8);
		Assertions.assertEquals(UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption, freight.getUseDistanceConstraintForTourPlanning());
	}

}
