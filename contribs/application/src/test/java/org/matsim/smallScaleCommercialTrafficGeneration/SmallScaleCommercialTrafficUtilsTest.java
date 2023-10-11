/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ricardo Ewert
 *
 */
public class SmallScaleCommercialTrafficUtilsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void findZoneOfLinksTest() throws IOException, URISyntaxException {

		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		String networkPath = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath);
		config.network().setInputCRS("EPSG:4326");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = GenerateSmallScaleCommercialTrafficDemand
				.filterLinksForZones(scenario, shpZones, SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, config.global().getCoordinateSystem()),
                        buildingsPerZone);

		Assert.assertEquals(3, regionLinksMap.size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(60, regionLinksMap.get("testArea1_area1").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(41, regionLinksMap.get("testArea1_area2").size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(28, regionLinksMap.get("testArea2_area3").size(), MatsimTestUtils.EPSILON);

		Assert.assertNull(SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(5,4)"), regionLinksMap));
		Assert.assertEquals("testArea1_area1", SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(6,5)R"), regionLinksMap));
		Assert.assertEquals("testArea1_area2", SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(2,7)R"), regionLinksMap));
		Assert.assertEquals("testArea2_area3", SmallScaleCommercialTrafficUtils.findZoneOfLink(Id.createLinkId("j(2,2)R"), regionLinksMap));
	}
}
