/* *********************************************************************** *
 * project: org.matsim.*
 * MyShoppingReaderTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.utilities.openstreetmap.shopping;


import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author jwjoubert
 *
 */
public class MyShoppingReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");

	@Test
	public void testConstructor() {
		String oneMall = utils.getPackageInputDirectory() + "oneMall.osm";
		MyShoppingReader msr = new MyShoppingReader(oneMall, ct);
		Assert.assertEquals("Wrong number of links.", 231, msr.getQuadTree().size());		
	}
	
	@Test
	public void testParseShopping() {
		String oneMall = utils.getPackageInputDirectory() + "oneMall.osm";
		MyShoppingReader msr = new MyShoppingReader(oneMall, ct);
		
		try {
			msr.parseShopping(oneMall);
		} catch (FileNotFoundException e) {
			Assert.fail("Test file should exist!");
		}
		
		/* Test that the right facility was created. */
		Assert.assertEquals("Wrong number of facilities.", 1, msr.getShops().getFacilities().size());
		Assert.assertNotNull("Could find the right facility Id.", msr.getShops().getFacilities().get(Id.create("1713491", ActivityFacility.class)));
		ActivityFacilityImpl afi = (ActivityFacilityImpl) msr.getShops().getFacilities().get(Id.create("1713491", ActivityFacility.class));
		Assert.assertEquals("Wrong facility name/description.", "Eco Boulevard", afi.getDesc());
		Assert.assertEquals("Wrong number of activity options.", 4, afi.getActivityOptions().size());
				
	}
	
	@Test
	public void testWriteReadFacility(){
		String oneMall = utils.getPackageInputDirectory() + "oneMall.osm";
		MyShoppingReader msr = new MyShoppingReader(oneMall, ct);
		
		/* Parse, and write facilities and its attributes. */
		try {
			msr.parseShopping(oneMall);
			msr.writeFacilities(utils.getOutputDirectory() + "facilities.xml");
			msr.writeFacilityAttributes(utils.getOutputDirectory() + "facilityAttributes.xml");
		} catch (FileNotFoundException e) {
			/* Already tested. */
		}
		
		/* Read facilities and its attributes. */
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(utils.getOutputDirectory() + "facilities.xml");
		ActivityFacilities afi = sc.getActivityFacilities();
		
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.parse(utils.getOutputDirectory() + "facilityAttributes.xml");
		
		Assert.assertNotNull("Should find the facility.", afi.getFacilities().get(Id.create("1713491", ActivityFacility.class)));
		Assert.assertEquals("Wrong number of facilties found.", 1, afi.getFacilities().size());
		
		ActivityFacilityImpl facility = (ActivityFacilityImpl) afi.getFacilities().get(Id.create("1713491", ActivityFacility.class));
		Assert.assertEquals("Wrong facility name.", "Eco Boulevard", facility.getDesc());
		Assert.assertEquals("Wrong number of activity options.", 4, facility.getActivityOptions().size());
		Assert.assertNotNull("Should find lettable area attribute.", oa.getAttribute(facility.getId().toString(), "gla"));
		Assert.assertEquals("Wrong GLA found.", "35000", oa.getAttribute(facility.getId().toString(), "gla"));
	}

}

