/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialChainAnalyserTest.java
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

package playground.jjoubert.CommercialTraffic.ChainAnalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.CommercialVehicle;
import playground.jjoubert.CommercialTraffic.GPSPoint;
import playground.jjoubert.Utilities.MyXmlConverter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class MyCommercialChainAnalyserTest extends MatsimTestCase{
	
	public void testMyCommercialChainAnalyserConstructor(){
		createTestVehicles("Temp");
		MyCommercialChainAnalyser mcca = new MyCommercialChainAnalyser("Temp", 0.6, 
				getOutputDirectory() + "Temp_VehicleStats.txt");
		assertEquals("Wrong number of 'through' vehicles", 1, mcca.getThroughList().size());
		assertEquals("Wrong 'through' vehicle", 1, mcca.getThroughList().get(0).intValue());
		assertEquals("Wrong number of 'within' vehicles", 1, mcca.getWithinList().size());
		assertEquals("Wrong 'within' vehicle", 2, mcca.getWithinList().get(0).intValue());
	}
	
	public void testAnalyse(){
		createTestVehicles("Temp");	
		MyCommercialChainAnalyser mcca = new MyCommercialChainAnalyser("Temp", 0.6, 
				getOutputDirectory() + "Temp_VehicleStats.txt");
		mcca.analyse(getOutputDirectory(), getOutputDirectory(), 20, 48);
		assertEquals("Wrong withinMatrix entry.", 2, (int)mcca.getWithinMatrix().getQuick(1, 2, 2));
		assertEquals("Wrong throughMatrix entry.", 1, (int)mcca.getThroughMatrix().get(1, 4, 4));
	}
	
	/**
	 * The <code>SparseDoubleMatrix3D</code> object is usually much too big to be converted
	 * and written to file using the <code>playground.jjoubert.MyXmlConverter</code>, so
	 * here I test if the sparse file is written correctly.
	 */
	public void testWriteMatrixFiles(){
		createTestVehicles("Temp");
		MyCommercialChainAnalyser mcca = new MyCommercialChainAnalyser("Temp", 0.6, 
				getOutputDirectory() + "Temp_VehicleStats.txt");
		mcca.analyse(getOutputDirectory(), getOutputDirectory(), 20, 48);
		mcca.writeMatrixFiles(getOutputDirectory(), "Temp");
		
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(getOutputDirectory() + "Temp_WithinChainMatrix.txt"))));
			String l = input.nextLine();
			String s = "SparseDoubleMatrix3D dimensions:";
			assertEquals("Within: Line 1 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "StartHour,NumberOfActivities,ChainDuration";
			assertEquals("Within: Line 2 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "24,21,49";
			assertEquals("Within: Line 3 written incorrectly.", true, s.equalsIgnoreCase(l));			
			l = input.nextLine();
			s = "SparseDoubleMatrix3D data:";
			assertEquals("Within: Line 4 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "Slice,Row,Column,Value";
			assertEquals("Within: Line 5 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "1,2,2,2";
			assertEquals("Within: Line 6 written incorrectly.", true, s.equalsIgnoreCase(l));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(getOutputDirectory() + "Temp_ThroughChainMatrix.txt"))));
			String l = input.nextLine();
			String s = "SparseDoubleMatrix3D dimensions:";
			assertEquals("Through: Line 1 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "StartHour,NumberOfActivities,ChainDuration";
			assertEquals("Through: Line 2 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "24,21,49";
			assertEquals("Through: Line 3 written incorrectly.", true, s.equalsIgnoreCase(l));			
			l = input.nextLine();
			s = "SparseDoubleMatrix3D data:";
			assertEquals("Through: Line 4 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "Slice,Row,Column,Value";
			assertEquals("Through: Line 5 written incorrectly.", true, s.equalsIgnoreCase(l));
			l = input.nextLine();
			s = "1,4,4,1";
			assertEquals("Through: Line 6 written incorrectly.", true, s.equalsIgnoreCase(l));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void testReadMatrixFiles(){
		createTestVehicles("Temp");
		MyCommercialChainAnalyser m1 = new MyCommercialChainAnalyser("Temp", 0.6, 
				getOutputDirectory() + "Temp_VehicleStats.txt");
		m1.analyse(getOutputDirectory(), getOutputDirectory(), 20, 48);
		m1.writeMatrixFiles(getOutputDirectory(), "Temp");
		
		MyCommercialChainAnalyser m2 = new MyCommercialChainAnalyser("Temp", 0.6, 
				getOutputDirectory() + "Temp_VehicleStats.txt");
		m2.readMatrixFiles(getOutputDirectory(), "Temp");
		
		assertEquals("Within: Wrong number of slices read", 24, m2.getWithinMatrix().slices());
		assertEquals("Within: Wrong number of rows read", 21, m2.getWithinMatrix().rows());
		assertEquals("Within: Wrong number of columns read", 49, m2.getWithinMatrix().columns());
		for(int a = 0; a < m2.getWithinMatrix().slices(); a++){
			for(int b = 0; b < m2.getWithinMatrix().rows(); b++){
				for(int c = 0; c < m2.getWithinMatrix().columns(); c++){
					assertEquals("Within: Wrong entry at (" + a + "," + b + "," + c + ")",
							m1.getWithinMatrix().getQuick(a, b, c),
							m2.getWithinMatrix().getQuick(a, b, c));
				}
			}
		}
		assertEquals("Through: Wrong number of slices read", 24, m2.getThroughMatrix().slices());
		assertEquals("Through: Wrong number of rows read", 21, m2.getThroughMatrix().rows());
		assertEquals("Through: Wrong number of columns read", 49, m2.getThroughMatrix().columns());
		for(int a = 0; a < m2.getThroughMatrix().slices(); a++){
			for(int b = 0; b < m2.getThroughMatrix().rows(); b++){
				for(int c = 0; c < m2.getThroughMatrix().columns(); c++){
					assertEquals("Through: Wrong entry at (" + a + "," + b + "," + c + ")",
							m1.getThroughMatrix().getQuick(a, b, c),
							m2.getThroughMatrix().getQuick(a, b, c));
				}
			}
		}
	}
	
	private MultiPolygon createTestArea(){
		Coordinate c1 = new Coordinate(0,0);
		Coordinate c2 = new Coordinate(1000,0);
		Coordinate c3 = new Coordinate(1000, 1000);
		Coordinate c4 = new Coordinate(0, 1000);
		Coordinate c[] = {c1,c2,c3,c4,c1};
		
		GeometryFactory gf = new GeometryFactory();
		Polygon p = gf.createPolygon(gf.createLinearRing(c), null);
		Polygon pa[] = new Polygon [1];
		pa[0] = p;
		MultiPolygon mp = gf.createMultiPolygon(pa);
		return mp;
	}
	

	private void createTestVehicles(String studyAreaName){

		/*
		 * Build a 'through' vehicle, with a single 'through' chain.
		 */
		CommercialVehicle v1 = new CommercialVehicle(1);

		// Create the test points (facility locations)
		GPSPoint p1 = new GPSPoint(v1.getVehID(), 0, 0, new Coordinate(-2000, 200));
		GPSPoint p2 = new GPSPoint(v1.getVehID(), 0, 0, new Coordinate(-1000, 200));
		GPSPoint p3 = new GPSPoint(v1.getVehID(), 0, 0, new Coordinate(200, 200));
		GPSPoint p4 = new GPSPoint(v1.getVehID(), 0, 0, new Coordinate(800, 800));
		GPSPoint p5 = new GPSPoint(v1.getVehID(), 0, 0, new Coordinate(2000, 800));
		GPSPoint p6 = new GPSPoint(v1.getVehID(), 0, 0, new Coordinate(3000, 800));		
		// Build chain.
		Chain c1 = new Chain();
		Activity a1 = new Activity(new GregorianCalendar(1, 1, 1, 1, 0),
				new GregorianCalendar(1, 1, 1, 1, 30), p1);
		c1.getActivities().add(a1);
		Activity a2 = new Activity(new GregorianCalendar(1, 1, 1, 2, 0),
				new GregorianCalendar(1, 1, 1, 2, 30), p2);
		c1.getActivities().add(a2);
		Activity a3 = new Activity(new GregorianCalendar(1, 1, 1, 3, 0),
				new GregorianCalendar(1, 1, 1, 3, 30), p3);
		c1.getActivities().add(a3);
		Activity a4 = new Activity(new GregorianCalendar(1, 1, 1, 4, 0),
				new GregorianCalendar(1, 1, 1, 4, 30), p4);
		c1.getActivities().add(a4);
		Activity a5 = new Activity(new GregorianCalendar(1, 1, 1, 5, 0),
				new GregorianCalendar(1, 1, 1, 5, 30), p5);
		c1.getActivities().add(a5);
		Activity a6 = new Activity(new GregorianCalendar(1, 1, 1, 6, 0),
				new GregorianCalendar(1, 1, 1, 6, 30), p6);
		c1.getActivities().add(a6);
		v1.getChains().add(c1);
		// Write chain to file.
		MyXmlConverter mxc = new MyXmlConverter(true);
		try {
			mxc.writeObjectToFile(v1, getOutputDirectory() + "1.xml");
		} catch (IOException e2) {
			throw new RuntimeException(e2);
		}

		/*
		 * Build a 'within' vehicle.
		 */
		CommercialVehicle v2 = new CommercialVehicle(2);
		// Create test points (facility locations)
		GPSPoint p7 = new GPSPoint(v2.getVehID(), 0, 0, new Coordinate(200, 200));
		GPSPoint p8 = new GPSPoint(v2.getVehID(), 0, 0, new Coordinate(800, 200));
		GPSPoint p9 = new GPSPoint(v2.getVehID(), 0, 0, new Coordinate(800, 800));
		GPSPoint p10 = new GPSPoint(v2.getVehID(), 0, 0, new Coordinate(200, 800));
		// Build chain
		Chain c2 = new Chain();
		Activity a7 = new Activity(new GregorianCalendar(1, 1, 1, 1, 0),
				new GregorianCalendar(1, 1, 1, 1, 30), p7);
		c2.getActivities().add(a7);
		Activity a8 = new Activity(new GregorianCalendar(1, 1, 1, 2, 0),
				new GregorianCalendar(1, 1, 1, 2, 30), p8);
		c2.getActivities().add(a8);
		Activity a9 = new Activity(new GregorianCalendar(1, 1, 1, 3, 0),
				new GregorianCalendar(1, 1, 1, 3, 30), p9);
		c2.getActivities().add(a9);
		Activity a10 = new Activity(new GregorianCalendar(1, 1, 1, 4, 0),
				new GregorianCalendar(1, 1, 1, 4, 30), p10);
		c2.getActivities().add(a10);
		v2.getChains().add(c2);
		// Build a second chain, just in reverse sequence, but the same times.
		Chain c3 = new Chain();
		Activity a11 = new Activity(new GregorianCalendar(1, 1, 1, 1, 0),
				new GregorianCalendar(1, 1, 1, 1, 30), p10);
		c3.getActivities().add(a11);
		Activity a12 = new Activity(new GregorianCalendar(1, 1, 1, 2, 0),
				new GregorianCalendar(1, 1, 1, 2, 30), p9);
		c3.getActivities().add(a12);
		Activity a13 = new Activity(new GregorianCalendar(1, 1, 1, 3, 0),
				new GregorianCalendar(1, 1, 1, 3, 30), p8);
		c3.getActivities().add(a13);
		Activity a14 = new Activity(new GregorianCalendar(1, 1, 1, 4, 0),
				new GregorianCalendar(1, 1, 1, 4, 30), p7);
		c3.getActivities().add(a14);
		v2.getChains().add(c3);
		
		// Write chain to file.
		try {
			mxc.writeObjectToFile(v2, getOutputDirectory() + "2.xml");
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		
		/*
		 * Create a 'vehicleStats.txt' file.
		 */
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(getOutputDirectory() + studyAreaName + "_VehicleStats.txt")));
			try{
				bw.write("Id,ActivitiesPerChain,AvgDist,AvgDur,Minor,Major,KmPerAreaActivity");
				bw.newLine();
				bw.write("1,4,0,0,0.3,0.3,0");
				bw.newLine();
				bw.write("2,2,0,0,0.8,0.8,0");
			} finally{
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
