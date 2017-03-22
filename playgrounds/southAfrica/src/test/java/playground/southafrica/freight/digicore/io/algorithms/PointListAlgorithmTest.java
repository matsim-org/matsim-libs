/* *********************************************************************** *
 * project: org.matsim.*
 * PointListAlgorithmTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.io.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.containers.MyZone;

public class PointListAlgorithmTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@SuppressWarnings("unused")
	public void testConstructorToPass() {
		try{
			PointListAlgorithm pla = new PointListAlgorithm(utils.getClassInputDirectory() + "test.shp", 1);
		} catch (IOException e){
			fail("Should find test shapefile.");
		} catch (Exception e){
			fail("Should not catch any exceptions.");
		}
	}

	@Test
	@SuppressWarnings("unused")
	public void testConstructorToFail() {
		try{
			PointListAlgorithm pla = new PointListAlgorithm("dummy.shp", 1);
			fail("Should catch IOException.");
		} catch (IOException e){
			/* Correctly caught exception. */
		} catch (Exception e){
			fail("Should not catch any other exceptions.");
		}
	}
	
	@Test
	public void testBuildVehicle(){
		DigicoreVehicle vehicle = buildVehicle("test");
		assertEquals("Wrong number of chains.", 1, vehicle.getChains().size());
		assertTrue("Chain should be complete.", vehicle.getChains().get(0).isComplete());
	}
	
	@Test
	public void testSetRoot(){
		PointListAlgorithm pla = null;
		try{
			pla = new PointListAlgorithm(utils.getClassInputDirectory() + "test.shp", 1);
		} catch (IOException e){
			fail("Should find test shapefile.");
		} catch (Exception e){
			fail("Should not catch any exceptions.");
		}
		Assert.assertFalse("Serial folder should not exist.", new File(utils.getOutputDirectory() + "serial/").exists());
		pla.setRoot(utils.getOutputDirectory());
		Assert.assertTrue("Serial folder should exist.", new File(utils.getOutputDirectory() + "serial/").exists());
	}
	
	@Test
	public void testApply(){
		PointListAlgorithm pla = null;
		try {
			pla = new PointListAlgorithm(utils.getClassInputDirectory() + "test.shp", 1);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should not throw exceptions.");
		}
		DigicoreVehicle vehicle = buildVehicle("test");
		try{
			pla.apply(vehicle);
			fail("Should not execute method without root.");
		} catch (Exception e){
			/* Correctly caught exception. */
		}

		try {
			pla = new PointListAlgorithm(utils.getClassInputDirectory() + "test.shp", 1);
			pla.setRoot(utils.getOutputDirectory());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should not throw exceptions.");
		}
		pla.apply(vehicle);
		
		/* Check the serialised file(s). */
		List<File> serialFiles = FileUtils.sampleFiles(new File(utils.getOutputDirectory() + "serial/"), Integer.MAX_VALUE, FileUtils.getFileFilter(".data"));
		assertEquals("Wrong number of serialised files.", 1, serialFiles.size());

		/* Check the specific file. */
		assertTrue("Serialised output file should exist.", new File(utils.getOutputDirectory() + "serial/test.data").exists());
		assertTrue("Serialised output file should be a file.", new File(utils.getOutputDirectory() + "serial/test.data").isFile());
		assertTrue("Serialised output file should be readable.", new File(utils.getOutputDirectory() + "serial/test.data").canRead());
		
		/* Load the file and check. */
		Object o = null;
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(serialFiles.get(0));
			ois = new ObjectInputStream(fis);
			o = ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			fail("Should load the serialised file.");
		} finally{
			try {
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
				fail("Should load the serialised file.");
			}
		}
		assertTrue("Object should be Map.", o instanceof Map<?, ?>);
		Map<?,?> map = (Map<?,?>)o;
		assertEquals("Map should have entries.", 3, map.size());
		
		
		Object ido = map.keySet().iterator().next();
		assertTrue("Key should be instance of Id", ido instanceof Id<?>);
	}
	
	@Test
	public void testGetAllSerializedIdsExceptLast(){
		/* Before any serialization. */
		PointListAlgorithm pla = null;
		try{
			pla = new PointListAlgorithm(utils.getClassInputDirectory() + "test.shp", 1);
		} catch (IOException e){
			fail("Should find test shapefile.");
		} catch (Exception e){
			fail("Should not catch any exceptions.");
		}
		pla.setRoot(utils.getOutputDirectory());
		List<Id<MyZone>> ids = pla.getAllSerializedIdsExceptLast(utils.getOutputDirectory() + "serial/");
		assertEquals("Wrong number of files.", 0, ids.size());
		
		/* After one serialization. */
		DigicoreVehicle v1 = buildVehicle("1");
		pla.apply(v1);
		ids = pla.getAllSerializedIdsExceptLast(utils.getOutputDirectory() + "serial/");
		assertEquals("Wrong number of files.", 0, ids.size());
		
		/* After two serialization. */
		DigicoreVehicle v2 = buildVehicle("2");
		pla.apply(v2);
		/* Wait 2 seconds so the time can be different. */
		long now = System.currentTimeMillis();
		while(System.currentTimeMillis() - now < 2000){
		}
		DigicoreVehicle v3 = buildVehicle("3");
		pla.apply(v3);
		ids = pla.getAllSerializedIdsExceptLast(utils.getOutputDirectory() + "serial/");
		assertEquals("Wrong number of files.", 1, ids.size());
	}

	/**
	 * Build a vehicle (v2) with activities in the following zones.
	 * ._____________________.
	 * |3         |         4|
	 * |          |          |
	 * | a        |          |  e
	 * |__________|__________|  
	 * |1         |         2|
	 * |          | c        |
	 * | b d      |          |
	 * |__________|__________|
	 * 
	 * @return
	 */
	private DigicoreVehicle buildVehicle(String id){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.createVehicleId(id));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity a = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a.setCoord(CoordUtils.createCoord(0.3, 2.3));
		chain.add(a);
		DigicoreTrace trace = new DigicoreTrace(TransformationFactory.ATLANTIS);
		chain.add(trace);
		DigicoreActivity b = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		b.setCoord(CoordUtils.createCoord(0.3, 0.3));
		chain.add(b);
		chain.add(trace);
		DigicoreActivity c = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		c.setCoord(CoordUtils.createCoord(2.3, 1.3));
		chain.add(c);
		chain.add(trace);
		DigicoreActivity d = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		d.setCoord(CoordUtils.createCoord(0.6, 0.3));
		chain.add(d);
		chain.add(trace);
		DigicoreActivity e = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		e.setCoord(CoordUtils.createCoord(4.3, 2.3));
		chain.add(e);
		vehicle.getChains().add(chain);
		
		return vehicle;
	}
	
}
