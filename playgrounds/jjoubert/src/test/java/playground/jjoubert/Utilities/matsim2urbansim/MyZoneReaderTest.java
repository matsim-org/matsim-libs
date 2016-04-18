/* *********************************************************************** *
 * project: org.matsim.*
 * MyZoneReaderTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;


public class MyZoneReaderTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	private Logger log = Logger.getLogger(MyZoneReaderTest.class);
	
	@Test
	public void testMyZoneReaderConstructor(){
		File folder = new File(utils.getInputDirectory());
		String shapefile = folder.getParent() + "/zones.shp";
		MyZoneReader mzr1 = new MyZoneReader(shapefile);
		Assert.assertEquals("MyZoneReader not created.", false, mzr1 == null);
		Assert.assertEquals("Wrong shapefile name stored.", true, shapefile.equalsIgnoreCase(mzr1.getShapefileName()));
		
		MyZoneReader mzr2 = null;
		try{
			mzr2 = new MyZoneReader(folder.getParent() + "/dummy.shp");
			Assert.fail("Should have thrown an exception.");
		} catch(RuntimeException e){
			log.info("Caught the expected exception.");
		}
		Assert.assertNull("Should not create instance if shapefile does not exist.", mzr2);
	}
	
	@Test
	public void testReadZones(){
		File folder = new File(utils.getInputDirectory());
		String shapefile = folder.getParent() + "/zones.shp";
		MyZoneReader mzr = new MyZoneReader(shapefile);
		mzr.readZones(1);
		Assert.assertEquals("Wrong return type.", ArrayList.class, mzr.getZoneList().getClass());
		List<MyZone> zones = (List<MyZone>) mzr.getZoneList();
		Assert.assertEquals("Wrong number of objects.", 4, zones.size());
		MyZone z = zones.get(0);
		Assert.assertEquals("Wrong Id for first zone.", "0", z.getId().toString());
		z = zones.get(1);
		Assert.assertEquals("Wrong Id for second zone.", "1", z.getId().toString());
		z = zones.get(2);
		Assert.assertEquals("Wrong Id for third zone.", "2", z.getId().toString());
		z = zones.get(3);
		Assert.assertEquals("Wrong Id for fourth zone.", "3", z.getId().toString());
	}
}

