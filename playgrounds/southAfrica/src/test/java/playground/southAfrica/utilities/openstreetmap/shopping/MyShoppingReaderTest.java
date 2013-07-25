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
package playground.southAfrica.utilities.openstreetmap.shopping;


import java.io.FileNotFoundException;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.openstreetmap.shopping.MyShoppingReader;

/**
 * @author johanwjoubert
 *
 */
public class MyShoppingReaderTest extends MatsimTestCase {
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");

	public void testConstructor() {
		String oneMall = getPackageInputDirectory() + "oneMall.osm";
		MyShoppingReader msr = new MyShoppingReader(oneMall, ct);
		assertEquals("Wrong number of links.", 231, msr.getQuadTree().size());		
	}
	
	public void testParseShopping() {
		String oneMall = getPackageInputDirectory() + "oneMall.osm";
		MyShoppingReader msr = new MyShoppingReader(oneMall, ct);
		
		try {
			msr.parseShopping(oneMall);
		} catch (FileNotFoundException e) {
			fail("Test file should exist!");
		}
		
		/* Test that the right facility was created. */
		assertEquals("Wrong number of facilities.", 1, msr.getShops().getFacilities().size());
		assertNotNull("Could find the right facility Id.", msr.getShops().getFacilities().get(new IdImpl("1713491")));
		ActivityFacilityImpl afi = (ActivityFacilityImpl) msr.getShops().getFacilities().get(new IdImpl("1713491"));
		assertEquals("Wrong facility name/description.", "Eco Boulevard", afi.getDesc());
		assertEquals("Wrong number of activity options.", 4, afi.getActivityOptions().size());
				
	}
	
	public void testWriteReadFacility(){
		String oneMall = getPackageInputDirectory() + "oneMall.osm";
		MyShoppingReader msr = new MyShoppingReader(oneMall, ct);
		
		/* Parse, and write facilities and its attributes. */
		try {
			msr.parseShopping(oneMall);
			msr.writeFacilities(getOutputDirectory() + "facilities.xml");
			msr.writeFacilityAttributes(getOutputDirectory() + "facilityAttributes.xml");
		} catch (FileNotFoundException e) {
			/* Already tested. */
		}
		
		/* Read facilities and its attributes. */
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(getOutputDirectory() + "facilities.xml");
		ActivityFacilities afi = sc.getActivityFacilities();
		
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.parse(getOutputDirectory() + "facilityAttributes.xml");
		
		assertNotNull("Should find the facility.", afi.getFacilities().get(new IdImpl("1713491")));
		assertEquals("Wrong number of facilties found.", 1, afi.getFacilities().size());
		
		ActivityFacilityImpl facility = (ActivityFacilityImpl) afi.getFacilities().get(new IdImpl("1713491"));
		assertEquals("Wrong facility name.", "Eco Boulevard", facility.getDesc());
		assertEquals("Wrong number of activity options.", 4, facility.getActivityOptions().size());
		assertNotNull("Should find lettable area attribute.", oa.getAttribute(facility.getId().toString(), "gla"));
		assertEquals("Wrong GLA found.", "35000", oa.getAttribute(facility.getId().toString(), "gla"));


		
	}

}

