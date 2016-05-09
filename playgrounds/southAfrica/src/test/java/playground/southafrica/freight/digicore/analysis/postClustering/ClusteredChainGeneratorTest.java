/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.postClustering;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.algorithms.djcluster.HullConverter;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class ClusteredChainGeneratorTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testSetup(){
		setupClusters();
		File vehicleFile = new File(utils.getOutputDirectory() + "xml/vehicle.xml.gz");
		Assert.assertTrue("Vehicle file does not exist.", vehicleFile.exists());
		
		File facilityFile = new File(utils.getOutputDirectory() + "facilities.xml");
		Assert.assertTrue("Facility file does not exist.", facilityFile.exists());
		
		/* Check facilities. */
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(utils.getOutputDirectory() + "facilities.xml");
		ActivityFacilities afs = sc.getActivityFacilities();
		Assert.assertTrue("Facility 1 not in map.", afs.getFacilities().containsKey(Id.create("f1", Facility.class)));
		Assert.assertTrue("Facility 2 not in map.", afs.getFacilities().containsKey(Id.create("f2", Facility.class)));
		Assert.assertTrue("Facility 3 not in map.", afs.getFacilities().containsKey(Id.create("f3", Facility.class)));
		
		/* Check facility coordinates. */
		Assert.assertEquals("Wrong centroid for f1", new Coord(0.5, 5.5), afs.getFacilities().get(Id.create("f1", Facility.class)).getCoord());
		Assert.assertEquals("Wrong centroid for f2", new Coord((3.0 + 4.0 + 5.0) / 3.0, (1.0 + 3.0 + 3.0) / 3.0), afs.getFacilities().get(Id.create("f2", Facility.class)).getCoord());
		Assert.assertEquals("Wrong centroid for f3", new Coord(5.0, 6.0), afs.getFacilities().get(Id.create("f3", Facility.class)).getCoord());
		
		File facilityAttributeFile = new File(utils.getOutputDirectory() + "facilityAttributes.xml");
		Assert.assertTrue("Facility attributes file does not exist.", facilityAttributeFile.exists());
	}
	
	
	@Test
	public void testConstructor(){
		
	}
	
	@Test
	public void testBuildFacilityQuadTree(){
		setupClusters();
		ClusteredChainGenerator ccg = new ClusteredChainGenerator();
		QuadTree<DigicoreFacility> qt = null;
		
		/* Should catch IOExceptions for non-existing files. */
		try {
			qt = ccg.buildFacilityQuadTree("dummy.xml", utils.getOutputDirectory() + "facilityAttributes.xml");
			Assert.fail("Facility file does not exist.");
		} catch (Exception e1) {
			/* Pass. */
		}
		try {
			qt = ccg.buildFacilityQuadTree(utils.getOutputDirectory() + "facilities.xml", "dummy.xml");
//			fail("Facility attributes file does not exist.");
		} catch (Exception e1) {
			/* Pass. */
		}
		
		try{
			qt = ccg.buildFacilityQuadTree(utils.getOutputDirectory() + "facilities.xml", utils.getOutputDirectory() + "facilityAttributes.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should not catch an IOException.");
		}
		
		Assert.assertEquals("Wrong number of facilities in QT.", 3, qt.getDisk(3, 3, 10).size());
		Assert.assertEquals("Wrong facility", Id.create("f1", Facility.class), qt.getClosest(0, 6).getId());
		Assert.assertEquals("Wrong facility", Id.create("f2", Facility.class), qt.getClosest(6, 0).getId());
		Assert.assertEquals("Wrong facility", Id.create("f3", Facility.class), qt.getClosest(5, 6).getId());
	}
	
	@Test
	public void testReconstructChains(){
		setupClusters();
		
		ClusteredChainGenerator ccg = new ClusteredChainGenerator();
		QuadTree<DigicoreFacility> qt = null;
		try {
			qt = ccg.buildFacilityQuadTree(utils.getOutputDirectory() + "facilities.xml", utils.getOutputDirectory() + "facilityAttributes.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/* Set up files and shapefiles. */
		String facilityAttributes = utils.getOutputDirectory() + "facilityAttributes.xml";
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.putAttributeConverter(Point.class, new HullConverter());
		oar.putAttributeConverter(Polygon.class, new HullConverter());
		oar.parse(facilityAttributes);
		
		String inputFolder = utils.getOutputDirectory() + "xml/";
		String outputFolder = utils.getOutputDirectory() + "xml2/";
		Coordinate[] ca = new Coordinate[5];
		ca[0] = new Coordinate(0.0, 0.0);
		ca[1] = new Coordinate(0.0, 7.0);
		ca[2] = new Coordinate(7.0, 7.0);
		ca[3] = new Coordinate(7.0, 0.0);
		ca[4] = ca[0];
		Polygon studyArea = new GeometryFactory().createPolygon(ca);
		
		try {
			ccg.reconstructChains(qt, oa, inputFolder, outputFolder, 1, studyArea);
		} catch (IOException e) {
			Assert.fail("Should not have any exceptions.");
		}
		
		/* Check that the correct three activities do have facility Ids. */
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(utils.getOutputDirectory() + "xml2/v1.xml.gz");
		DigicoreChain chain = dvr.getVehicle().getChains().get(0);
		Assert.assertNotNull("First activity must have an id.", chain.getAllActivities().get(0).getFacilityId());
		Assert.assertEquals("First activity has wrong id.", Id.create("f1", Facility.class), chain.getAllActivities().get(0).getFacilityId());
		Assert.assertNotNull("Second activity must have an id.", chain.getAllActivities().get(2).getFacilityId());
		Assert.assertEquals("Second activity has wrong id.", Id.create("f2", Facility.class), chain.getAllActivities().get(2).getFacilityId());
		Assert.assertNotNull("Third activity must have an id.", chain.getAllActivities().get(4).getFacilityId());
		Assert.assertEquals("Third activity has wrong id.", Id.create("f3", Facility.class), chain.getAllActivities().get(4).getFacilityId());
	}
	
	
	/**
	 * Test case:
	 * 
	 * Activity chain
	 * a1 -> a9 -> a8 -> a10 -> a11 
	 * 
	 * a1---a2               a11
	 *  |    |            facility 3
	 * a3---a4
	 * facility 1
	 *             a10
	 * 					facility 2
	 * 					a5---a6
	 * 	a9  			 | a8 |
	 *                    \  /
	 *  y                  a7
	 *  ^                   
	 *  |                   
	 *  |____> x                   
	 */
	private void setupClusters(){
		/* Create the output directories. */
		File directory = new File(utils.getOutputDirectory());
		directory.mkdirs();
		File directoryXml = new File(utils.getOutputDirectory() + "xml/");
		directoryXml.mkdirs();
		File directoryXml2 = new File(utils.getOutputDirectory() + "xml2/");
		directoryXml2.mkdirs();
		
		/* Set up basic activities. */
		DigicoreActivity a1 = new DigicoreActivity("t1", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a1.setCoord(new Coord((double) 0, (double) 6));
		DigicoreActivity a2 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a2.setCoord(new Coord((double) 1, (double) 6));
		DigicoreActivity a3 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a3.setCoord(new Coord((double) 0, (double) 5));
		DigicoreActivity a4 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a4.setCoord(new Coord((double) 1, (double) 5));
		DigicoreActivity a5 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a5.setCoord(new Coord((double) 3, (double) 3));
		DigicoreActivity a6 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a6.setCoord(new Coord((double) 5, (double) 3));
		DigicoreActivity a7 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a7.setCoord(new Coord((double) 4, (double) 1));
		DigicoreActivity a8 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a8.setCoord(new Coord((double) 4, (double) 2));
		DigicoreActivity a9 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a9.setCoord(new Coord((double) 0, (double) 2));
		DigicoreActivity a10 = new DigicoreActivity("t2", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a10.setCoord(new Coord((double) 2, (double) 4));
		DigicoreActivity a11 = new DigicoreActivity("t1", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a11.setCoord(new Coord((double) 5, (double) 6));
		
		/* Set up facilities */
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		ObjectAttributes attributes = new ObjectAttributes();
		ActivityFacilitiesFactory ff = new ActivityFacilitiesFactoryImpl();

		GeometryFactory gf = new GeometryFactory();
		Coordinate[] ca1  = new Coordinate[5];
		ca1[0] = new Coordinate(a1.getCoord().getX(), a1.getCoord().getY());
		ca1[1] = new Coordinate(a2.getCoord().getX(), a2.getCoord().getY());
		ca1[2] = new Coordinate(a3.getCoord().getX(), a3.getCoord().getY());
		ca1[3] = new Coordinate(a4.getCoord().getX(), a4.getCoord().getY());
		ca1[4] = ca1[0];
		Polygon p1 = gf.createPolygon(ca1);
		ActivityFacility f1 = ff.createActivityFacility(Id.create("f1", ActivityFacility.class), new Coord(p1.getCentroid().getX(), p1.getCentroid().getY()));
		facilities.addActivityFacility(f1);
		attributes.putAttribute(f1.getId().toString(), "concaveHull", p1);
		
		Coordinate[] ca2 = new Coordinate[4];
		ca2[0] = new Coordinate(a5.getCoord().getX(), a5.getCoord().getY());
		ca2[1] = new Coordinate(a6.getCoord().getX(), a6.getCoord().getY());
		ca2[2] = new Coordinate(a7.getCoord().getX(), a7.getCoord().getY());
		ca2[3] = ca2[0];
		Polygon p2 = gf.createPolygon(ca2);
		ActivityFacility f2 = ff.createActivityFacility(Id.create("f2", ActivityFacility.class), new Coord(p2.getCentroid().getX(), p2.getCentroid().getY()));
		facilities.addActivityFacility(f2);
		attributes.putAttribute(f2.getId().toString(), "concaveHull", p2);
		
		Coordinate[] ca3 = new Coordinate[1];
		ca3[0] = new Coordinate(a11.getCoord().getX(), a11.getCoord().getY());
		Point p3 = gf.createPoint(ca3[0]);
		ActivityFacility f3 = ff.createActivityFacility(Id.create("f3", ActivityFacility.class), new Coord(p3.getCentroid().getX(), p3.getCentroid().getY()));
		facilities.addActivityFacility(f3);
		attributes.putAttribute(f3.getId().toString(), "concaveHull", p3);
		
		/* Set up activity chain for the vehicle. */
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("v1", Vehicle.class));
		DigicoreChain chain = new DigicoreChain();
		chain.add(a1);
		chain.add(a9);
		chain.add(a8);
		chain.add(a10);
		chain.add(a11);
		vehicle.getChains().add(chain);
		
		/* Write vehicle to file. First create the folder  */
		
		DigicoreVehicleWriter vw = new DigicoreVehicleWriter();
		vw.write(utils.getOutputDirectory() + "xml/vehicle.xml.gz", vehicle);
		
		/* Write facilities. */
		FacilitiesWriter fw = new FacilitiesWriter(facilities);
		fw.write(utils.getOutputDirectory() + "facilities.xml");
		
		/* Write facility attributes. */
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(attributes);
		oaw.putAttributeConverter(Point.class, new HullConverter());
		oaw.putAttributeConverter(Polygon.class, new HullConverter());
		oaw.writeFile(utils.getOutputDirectory() + "facilityAttributes.xml");
	}
		
}
