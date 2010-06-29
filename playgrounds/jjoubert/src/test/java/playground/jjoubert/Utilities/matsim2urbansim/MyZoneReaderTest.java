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
import org.matsim.testcases.MatsimTestCase;


public class MyZoneReaderTest extends MatsimTestCase{
	private Logger log = Logger.getLogger(MyZoneReaderTest.class);
	
	public void testMyZoneReaderConstructor(){
		File folder = new File(getInputDirectory());
		String shapefile = folder.getParent() + "/zones.shp";
		MyZoneReader mzr1 = new MyZoneReader(shapefile);
		assertEquals("MyZoneReader not created.", false, mzr1 == null);
		assertEquals("Wrong shapefile name stored.", true, shapefile.equalsIgnoreCase(mzr1.getShapefileName()));
		
		MyZoneReader mzr2 = null;
		try{
			mzr2 = new MyZoneReader(folder.getParent() + "/dummy.shp");
			fail("Should have thrown an exception.");
		} catch(RuntimeException e){
			log.info("Caught the expected exception.");
		}
		assertNull("Should not create instance if shapefile does not exist.", mzr2);
	}
	
	public void testReadZones(){
		File folder = new File(getInputDirectory());
		String shapefile = folder.getParent() + "/zones.shp";
		MyZoneReader mzr = new MyZoneReader(shapefile);
		mzr.readZones(1);
		assertEquals("Wrong return type.", ArrayList.class, mzr.getZones().getClass());
		List<MyZone> zones = (List<MyZone>) mzr.getZones();
		assertEquals("Wrong number of objects.", 4, zones.size());
		MyZone z = zones.get(0);
		assertEquals("Wrong Id for first zone.", "0", z.getId().toString());
		z = zones.get(1);
		assertEquals("Wrong Id for second zone.", "1", z.getId().toString());
		z = zones.get(2);
		assertEquals("Wrong Id for third zone.", "2", z.getId().toString());
		z = zones.get(3);
		assertEquals("Wrong Id for fourth zone.", "3", z.getId().toString());
	}
}

