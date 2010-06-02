/* *********************************************************************** *
 * project: org.matsim.*
 * MyRasterTest.java
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

package playground.jjoubert.Utilities.KernelDensityEstimation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.matsim.testcases.MatsimTestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * The problem test case.
 * 
 * 		5........................
 * 		:   :   :   :   :   :  	:
 * 		:   :   :   : j :   :  	:
 * 		4...:...:_______:...:...:   
 * 		:   :h /:   :   :\  :   :     
 * 		:   : / :   :   : \ :   :     
 * 		3...:/.g:...:...:..\:...:     
 * 		:   |   :e f:   :   |   :     
 * 		:   |   : d :   :   | i :     
 * 		2...|...:...:...:...|...:     
 * 		:   :\  :   :   :c /:   :     
 * 		:   :b\ :   :   : / :   :     
 * 		1...:..\________:/..:...:     
 * 		:   :   :   :   :   :   :     
 * 		: a :   :   :   :   :   :     
 *  	0...1...2...3...4...5...6    
 * 
 * @author jwjoubert
 */
public class MyRasterTest extends MatsimTestCase{
	private final Polygon polygon = buildTestPolygon();
	private final List<Point> points = buildTestPoints();
	
	
	 /**
	  *  
	 */
	public void testRastorConstructor(){
		// Test a valid raster with stride 1.
		MyRaster mr1 = new MyRaster(polygon, 1, null, 0, Color.BLACK);
		assertEquals("X-extent (width) is incorrect.", 4, mr1.getBufferedImage().getWidth());
		assertEquals("Y-extent (height) is incorrect.", 3, mr1.getBufferedImage().getHeight());
		
		boolean b = mr1.writeMyRasterToFile(getOutputDirectory() + "Test1.jpg", "jpg");
		assertEquals("Could not write JPG file.", true, b);
		b = mr1.writeMyRasterToFile(getOutputDirectory() + "Test2.png", "png");
		assertEquals("Could not write PNG file.", true, b);
		
		// Test a valid raster with stride 2.
		MyRaster mr2 = new MyRaster(polygon, 2, null, 0, Color.BLACK);
		assertEquals("X-extent (width) is incorrect.", 2, mr2.getBufferedImage().getWidth());
		assertEquals("Y-extent (height) is incorrect.", 2, mr2.getBufferedImage().getHeight());
		
		// Test an invalid raster with too large a stride.
		MyRaster mr3 = new MyRaster(polygon, 5, null, 0, Color.BLACK);
		assertEquals("Raster should not be created. Stride too long.", null, mr3.getBufferedImage());
	}
	
	public void testProcessPoint(){
		/*
		 * Perform tests with no radius, i.e. null, and a KDE type that only 
		 * looks at the individual pixels (case `0').
		 */
		MyRaster mr = new MyRaster(polygon, 1, null, 0, Color.BLACK);
		
		assertEquals("Should not add point `a'.", false, mr.processPoint(points.get(0)));

		mr.processPoint(points.get(1));
		assertEquals("Point `b' allocated to the wrong pixel.", 1.0, mr.getImageMatrixValue(0, 2));
		assertEquals("Max pixel value is wrong.", 1.0, mr.getMaxImageMatrixValue());
		
		assertEquals("Point `c' should be added.", true, mr.processPoint(points.get(2)));
		
		mr.processPoint(points.get(3));
		assertEquals("Point `d' allocated to the wrong pixel.", 1.0, mr.getImageMatrixValue(1, 1));
		mr.processPoint(points.get(4));
		assertEquals("Point `e' allocated to the wrong pixel.", 2.0, mr.getImageMatrixValue(1, 1));
		assertEquals("Maximum image matrix value not correct.", 2.0, mr.getMaxImageMatrixValue());
		mr.processPoint(points.get(5));
		assertEquals("Point `f' allocated to the wrong pixel.", 3.0, mr.getImageMatrixValue(1, 1));
		assertEquals("Maximum image matrix value not correct.", 3.0, mr.getMaxImageMatrixValue());
		
		
	}
	
	/**
	 * Check that the image matrix values are correctly processed.
	 */
	public void testProcessPoints(){
		/*
		 * Create a raster with stride of 1: raster is 4x3. 
		 */
		MyRaster mr1 = new MyRaster(polygon, 1, null, 0, Color.BLACK);
		mr1.processPoints(points);
		
		assertEquals("Pixel value (0,0) of the imageMatrix `mr1' is incorrect.", 2.0, mr1.getImageMatrixValue(0, 0));
		assertEquals("Pixel value (1,0) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(1, 0));
		assertEquals("Pixel value (2,0) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(2, 0));
		assertEquals("Pixel value (3,0) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(3, 0));
		assertEquals("Pixel value (0,1) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(0, 1));
		assertEquals("Pixel value (1,1) of the imageMatrix `mr1' is incorrect.", 3.0, mr1.getImageMatrixValue(1, 1));
		assertEquals("Pixel value (2,1) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(2, 1));
		assertEquals("Pixel value (3,1) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(3, 1));
		assertEquals("Pixel value (0,2) of the imageMatrix `mr1' is incorrect.", 1.0, mr1.getImageMatrixValue(0, 2));
		assertEquals("Pixel value (1,2) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(1, 2));
		assertEquals("Pixel value (2,2) of the imageMatrix `mr1' is incorrect.", 0.0, mr1.getImageMatrixValue(2, 2));
		assertEquals("Pixel value (3,2) of the imageMatrix `mr1' is incorrect.", 1.0, mr1.getImageMatrixValue(3, 2));
		/*
		 * Create a raster with stride of 2: raster is 2x2.
		 */
		MyRaster mr2 = new MyRaster(polygon, 2, null, 0, Color.BLACK);
		mr2.processPoints(points);
		
		assertEquals("Pixel value (0,0) of the imageMatrix `mr2' is incorrect.", 5.0, mr2.getImageMatrixValue(0, 0));
		assertEquals("Pixel value (1,0) of the imageMatrix `mr2' is incorrect.", 0.0, mr2.getImageMatrixValue(1, 0));
		assertEquals("Pixel value (0,1) of the imageMatrix `mr2' is incorrect.", 1.0, mr2.getImageMatrixValue(0, 1));
		assertEquals("Pixel value (1,1) of the imageMatrix `mr2' is incorrect.", 1.0, mr2.getImageMatrixValue(1, 1));
		
		/*
		 * Create a raster with stride of 3: raster is 2x1.
		 */
		MyRaster mr3 = new MyRaster(polygon, 3, null, 0, Color.BLACK);
		mr3.processPoints(points);

		assertEquals("Pixel value (0,0) of the imageMatrix `mr3' is incorrect.", 6.0, mr3.getImageMatrixValue(0, 0));
		assertEquals("Pixel value (1,0) of the imageMatrix `mr3' is incorrect.", 1.0, mr3.getImageMatrixValue(1, 0));
		
		/*
		 * Perform tests with a radius of 1, stride of 1, and a KDE type that 
		 * looks at a uniform kernel function (case `1').
		 */
		MyRaster mr4 = new MyRaster(polygon, 1.0, 1.0, 1, Color.BLACK);
		mr4.processPoints(points);
		assertEquals("Pixel (0,0) has the wrong value.", 1.0, mr4.getImageMatrixValue(0, 0));
		assertEquals("Pixel (1,0) has the wrong value.", 2.0, mr4.getImageMatrixValue(1, 0));
		assertEquals("Pixel (2,0) has the wrong value.", 0.0, mr4.getImageMatrixValue(2, 0));
		assertEquals("Pixel (3,0) has the wrong value.", 0.0, mr4.getImageMatrixValue(3, 0));
		assertEquals("Pixel (0,1) has the wrong value.", 1.5, mr4.getImageMatrixValue(0, 1));
		assertEquals("Pixel (1,1) has the wrong value.", 2.0, mr4.getImageMatrixValue(1, 1));
		assertEquals("Pixel (2,1) has the wrong value.", 1.5, mr4.getImageMatrixValue(2, 1));
		assertEquals("Pixel (3,1) has the wrong value.", 0.5, mr4.getImageMatrixValue(3, 1));
		assertEquals("Pixel (0,2) has the wrong value.", 0.5, mr4.getImageMatrixValue(0, 2));
		assertEquals("Pixel (1,2) has the wrong value.", 0.5, mr4.getImageMatrixValue(1, 2));
		assertEquals("Pixel (2,2) has the wrong value.", 0.5, mr4.getImageMatrixValue(2, 2));
		assertEquals("Pixel (3,2) has the wrong value.", 0.5, mr4.getImageMatrixValue(3, 2));
		mr4.convertMatrixToRaster();
		boolean b = mr4.writeMyRasterToFile(getOutputDirectory() + "UniformKDE-Stride1.png", "png");
		assertEquals("Could not write uniform KDE raster to file", true, b);
		
		/*
		 * Perform tests with a radius of 1, stride of 1, and a KDE type that 
		 * looks at a uniform kernel function (case `1').
		 */
		MyRaster mr5 = new MyRaster(polygon, 2.0, 1.0, 1, Color.BLACK);
		mr5.processPoints(points);
		assertEquals("Pixel (0,0) has the wrong value.", 2.5, mr5.getImageMatrixValue(0, 0));
		assertEquals("Pixel (1,0) has the wrong value.", 0.0, mr5.getImageMatrixValue(1, 0));
		assertEquals("Pixel (0,1) has the wrong value.", 0.5, mr5.getImageMatrixValue(0, 1));
		assertEquals("Pixel (1,1) has the wrong value.", 0.5, mr5.getImageMatrixValue(1, 1));
		mr5.convertMatrixToRaster();
		b = mr5.writeMyRasterToFile(getOutputDirectory() + "UniformKDE-Stride2.png", "png");
		assertEquals("Could not write uniform KDE raster to file", true, b);
		
}
		

	/**
	 * Check that the converted image's alpha-channel levels are correctly calculated. 
	 * The output images are also written to file for visual inspection.
	 */
	public void testConvertMatrixToRaster(){
		/*
		 * Create a raster with stride of 1: raster is 4x3. 
		 */
		MyRaster mr1 = new MyRaster(polygon, 1, null, 0, Color.blue);
		mr1.processPoints(points);
		mr1.convertMatrixToRaster();
		
		assertEquals("Pixel (0,0) of the raster in incorrect.", (int) Math.floor((double) 2 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(0, 0, 3));
		assertEquals("Pixel (1,0) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(1, 0, 3));
		assertEquals("Pixel (2,0) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(2, 0, 3));
		assertEquals("Pixel (3,0) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(3, 0, 3));
		assertEquals("Pixel (0,1) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(0, 1, 3));
		assertEquals("Pixel (1,1) of the raster in incorrect.", (int) Math.floor((double) 3 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(1, 1, 3));
		assertEquals("Pixel (2,1) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(2, 1, 3));
		assertEquals("Pixel (3,1) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(3, 1, 3));
		assertEquals("Pixel (0,2) of the raster in incorrect.", (int) Math.floor((double) 1 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(0, 2, 3));
		assertEquals("Pixel (1,2) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(1, 2, 3));
		assertEquals("Pixel (2,2) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(2, 2, 3));
		assertEquals("Pixel (3,2) of the raster in incorrect.", (int) Math.floor((double) 1 / (double) 3 * 255), mr1.getBufferedImage().getData().getSample(3, 2, 3));

		mr1.writeMyRasterToFile(getOutputDirectory() + "Test1.png", "png");
		
		/*
		 * Create a raster with stride of 2: raster is 2x2.
		 */
		MyRaster mr2 = new MyRaster(polygon, 2, null, 0, Color.blue);
		mr2.processPoints(points);
		mr2.convertMatrixToRaster();
		
		assertEquals("Pixel (0,0) of the raster in incorrect.", (int) Math.floor((double) 5 / (double) 5 * 255), mr2.getBufferedImage().getData().getSample(0, 0, 3));
		assertEquals("Pixel (1,0) of the raster in incorrect.", (int) Math.floor((double) 0 / (double) 5 * 255), mr2.getBufferedImage().getData().getSample(1, 0, 3));
		assertEquals("Pixel (0,1) of the raster in incorrect.", (int) Math.floor((double) 1 / (double) 5 * 255), mr2.getBufferedImage().getData().getSample(0, 1, 3));
		assertEquals("Pixel (1,1) of the raster in incorrect.", (int) Math.floor((double) 1 / (double) 5 * 255), mr2.getBufferedImage().getData().getSample(1, 1, 3));
		
		mr2.writeMyRasterToFile(getOutputDirectory() + "Test2.png", "png");
		
		/*
		 * Create a raster with stride of 3: raster is 2x1.
		 */
		MyRaster mr3 = new MyRaster(polygon, 3, null, 0, Color.blue);
		mr3.processPoints(points);
		mr3.convertMatrixToRaster();
		
		assertEquals("Pixel (0,0) of the raster in incorrect.", (int) Math.floor((double) 6 / (double) 6 * 255), mr3.getBufferedImage().getData().getSample(0, 0, 3));
		assertEquals("Pixel (1,0) of the raster in incorrect.", (int) Math.floor((double) 1 / (double) 6 * 255), mr3.getBufferedImage().getData().getSample(1, 0, 3));
		
		mr3.writeMyRasterToFile(getOutputDirectory() + "Test3.png", "png");
	}
	
	
	/**
	  * Just check that the test case is actually created correctly ;-)
	  */
	 public void testTestCase(){
		 assertEquals( "Point p1 should not be in polygon!", false, polygon.contains(points.get(0)));		
		 assertEquals( "Point p1 should not be in polygon envelope!", false, polygon.getEnvelope().contains(points.get(0)));		
		 
		 assertEquals( "Point p2 should not be in polygon!", false, polygon.contains(points.get(1)));		
		 assertEquals( "Point p2 should be in polygon envelope!", true, polygon.getEnvelope().contains(points.get(1)));
		 
		 assertEquals( "Point p3 should be in polygon!", true, polygon.contains(points.get(2)));		
		 
		 assertEquals( "Point p4 should be in polygon!", true, polygon.contains(points.get(3)));		
		 
		 assertEquals( "Point p5 should be in polygon!", true, polygon.contains(points.get(4)));		
		 
		 assertEquals( "Point p6 should be in polygon!", true, polygon.contains(points.get(5)));		 
		 
		 assertEquals( "Point p7 should be in polygon!", true, polygon.contains(points.get(6)));		 
		 
		 assertEquals( "Point p8 should not be in polygon!", false, polygon.contains(points.get(7)));		
		 assertEquals( "Point p8 should be in polygon envelope!", true, polygon.getEnvelope().contains(points.get(7)));
		 
		 assertEquals( "Point p9 should not be in polygon!", false, polygon.contains(points.get(8)));		
		 assertEquals( "Point p9 should not be in polygon envelope!", false, polygon.getEnvelope().contains(points.get(8)));
		 
		 assertEquals( "Point p10 should not be in polygon!", false, polygon.contains(points.get(9)));		
		 assertEquals( "Point p10 should not be in polygon envelope!", false, polygon.getEnvelope().contains(points.get(9)));
	 }

	private static Polygon buildTestPolygon(){
		GeometryFactory gf = new GeometryFactory();

		Coordinate c1 = new Coordinate(2, 1);
		Coordinate c2 = new Coordinate(4, 1);
		Coordinate c3 = new Coordinate(5, 2);
		Coordinate c4 = new Coordinate(5, 3);
		Coordinate c5 = new Coordinate(4, 4);
		Coordinate c6 = new Coordinate(2, 4);
		Coordinate c7 = new Coordinate(1, 3);
		Coordinate c8 = new Coordinate(1, 2);
		Coordinate[] ca = {c1,c2,c3,c4,c5,c6,c7,c8,c1};

		LinearRing lr = gf.createLinearRing(ca);		
		Polygon p = gf.createPolygon(lr, null);	
		return p;		
	}

	private static List<Point> buildTestPoints() {
		GeometryFactory gf = new GeometryFactory();

		List<Point> l = new ArrayList<Point>(9);
		Point p1 = gf.createPoint(new Coordinate(0.5, 0.5)); // a
		l.add(p1);
		Point p2 = gf.createPoint(new Coordinate(1.2, 1.2)); // b
		l.add(p2);
		Point p3 = gf.createPoint(new Coordinate(4.2, 1.8)); // c
		l.add(p3);
		Point p4 = gf.createPoint(new Coordinate(2.5, 2.5)); // d
		l.add(p4);
		Point p5 = gf.createPoint(new Coordinate(2.3, 2.8)); // e
		l.add(p5);
		Point p6 = gf.createPoint(new Coordinate(2.7, 2.8)); // f
		l.add(p6);		
		Point p7 = gf.createPoint(new Coordinate(1.8, 3.2)); // g
		l.add(p7);
		Point p8 = gf.createPoint(new Coordinate(1.2, 3.8)); // h
		l.add(p8);
		Point p9 = gf.createPoint(new Coordinate(5.5, 2.5)); // i
		l.add(p9);
		Point p10 = gf.createPoint(new Coordinate(3.5, 4.5)); // j
		l.add(p10);
	
		return l;
	}


}
