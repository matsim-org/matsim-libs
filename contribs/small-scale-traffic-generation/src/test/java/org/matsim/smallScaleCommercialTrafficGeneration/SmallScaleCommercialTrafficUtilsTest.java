/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.smallScaleCommercialTrafficGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ricardo Ewert
 *
 */
public class SmallScaleCommercialTrafficUtilsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void findZoneOfLinksTest() throws IOException, URISyntaxException {

		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone = new HashMap<>();
		String shapeFileZoneNameColumn = "name";

		Map<String, Map<Id<Link>, Link>> regionLinksMap = GenerateSmallScaleCommercialTrafficDemand.filterLinksForZones(scenario,
			SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem(), shapeFileZoneNameColumn),
			facilitiesPerZone, shapeFileZoneNameColumn);

		Assertions.assertEquals(3, regionLinksMap.size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(60, regionLinksMap.get("area1").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(41, regionLinksMap.get("area2").size(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(28, regionLinksMap.get("area3").size(), MatsimTestUtils.EPSILON);

		Assertions.assertNull(SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(5,4)"), regionLinksMap));
		Assertions.assertEquals("area1", SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(6,5)R"), regionLinksMap));
		Assertions.assertEquals("area2", SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(2,7)R"), regionLinksMap));
		Assertions.assertEquals("area3", SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(2,2)R"), regionLinksMap));
	}
}
