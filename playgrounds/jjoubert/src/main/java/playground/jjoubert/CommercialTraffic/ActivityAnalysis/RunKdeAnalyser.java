/* *********************************************************************** *
 * project: org.matsim.*
 * RunKdeAnalyser.java
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

package playground.jjoubert.CommercialTraffic.ActivityAnalysis;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.KernelDensityEstimation.MyRaster;

public class RunKdeAnalyser {
	private final static Logger log = Logger.getLogger(RunKdeAnalyser.class);

	 /*=============================================================================
	 * String value indicating where the root where job is executed. 				|
	 * 		- Mac																	|
	 * 		- IVT-Sim0																|
	 * 		- Satawal																|
	 * 		- IE-Calvin														  		|
	 *=============================================================================*/
//	private static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; 	// Mac
//	private static String root = "/home/jjoubert/";										// IVT-Sim0
//	private static String root = "/home/jjoubert/data/";								// Satawal
	private static String root = "/home/jwjoubert/MATSim/MATSimData/";					// IE-Calvin

	 /*=============================================================================
	 * String value that must be set. Allowed study areas are:						|
	 * 		- SouthAfrica															|
	 * 		- Gauteng																|
	 * 		- KZN																	|
	 * 		- WesternCape															|
	 *=============================================================================*/
	private static String studyAreaName = "Gauteng";

	/*==============================================================================
	 * The year for which the DigiCore analysis is being done. Available years are:	|
	 * 		- 2008																	|
	 *=============================================================================*/
	private static int year = 2008;

	 /*=============================================================================
	 * Version of ActivityAnalysis that should be used.								|
	 *=============================================================================*/
	private static String version = "20091202131951";

	 /*=============================================================================
	 * Integer indicating for which minor/major threshold the KDE image should be 	|
	 * calculated.																	|
	 *=============================================================================*/
	private static int threshold = 300;

	 /*=============================================================================
	 * Integer indicating for which sample the KDE image should be 	calculated.		|
	 *=============================================================================*/
	private static int sample = 1;

	private static String activityType;
	private static boolean splitHourOfDay;
	private static double resolution;
	private static double radius;
	private static int kdeType;
	
	/**
	 * 
	 * @param args contain four arguments:
	 * <ul>
	 * 	<b>activityType</b> that can be "Minor", "Major", or "Both".<br>
	 *  <b>splitHourOfDay</b> indicating if a raster should be created for each 
	 *  	hour of the day separately. Values can be either "true" or "false". <br>
	 *  <b>resolution</b> indicating the width (in meters) of each pixel in 
	 *  	the final raster.<br>
	 *  <b>radius</b> with which the kernel density function is calculated 
	 *  	(expressed in meters).<br>
	 *  <b>kdeType</b> the type of kernel density function used. Currently the 
	 *  	following options are available:
	 *  	<ul>
	 *  		"0" - the value of the pixel containing the point is increased by 1;<br>
	 *  		"1" - uniform function;<br>
	 *  		"2" - triangular function;<br>
	 *  		"3" - triweight (tricube) function;
	 *  	</ul>    
	 * </ul>
	 */
	public static void main(String[] args) {
		int lineTotal = 0;		
		if(args.length != 5){
			throw new RuntimeException("Must have 5 arguments!");
		}
		
		activityType = args[0];
		splitHourOfDay = Boolean.parseBoolean(args[1]);
		resolution = Double.parseDouble(args[2]);
		radius = Double. parseDouble(args[3]);
		kdeType = Integer.parseInt(args[4]);
		
		log.info("Determining the number of activities to process");
		if(activityType.equalsIgnoreCase("Minor") || activityType.equalsIgnoreCase("Both")){
			String s = String.format("%s%s/%d/%s/%04d/Sample%02d/Activities/%s_MinorLocations.txt", 
					root, studyAreaName, year, version, threshold, sample, studyAreaName);
			log.info("   ...checking " + s);
			try {
				Scanner sc = new Scanner(new BufferedReader(new FileReader(new File(s))));
				String line = sc.nextLine();
				while(sc.hasNextLine()){
					line = sc.nextLine();
					lineTotal++;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} 
		if(activityType.equalsIgnoreCase("Major") || activityType.equalsIgnoreCase("Both")){
			String s = String.format("%s%s/%d/%s/%04d/Sample%02d/Activities/%s_MajorLocations.txt", 
					root, studyAreaName, year, version, threshold, sample, studyAreaName);
			log.info("   ...checking " + s);
			try {
				Scanner sc = new Scanner(new BufferedReader(new FileReader(new File(s))));
				String line = sc.nextLine();
				while(sc.hasNextLine()){
					line = sc.nextLine();
					lineTotal++;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}			
		}
		
		log.info("Generating the Kernel Density Estimate. Total of " + lineTotal + " activities to process.");
		MyShapefileReader msr = new MyShapefileReader(String.format("%sShapefiles/%s/%s_UTM35S.shp", 
								root, studyAreaName, studyAreaName));
		GeometryFactory gf = new GeometryFactory();
		MultiPolygon mp = msr.readMultiPolygon();
		Polygon p = null;
		if(mp.getNumGeometries() > 1){
			throw new RuntimeException("MyRaster can not deal with multiple polygons.");
		} else {
			p = (Polygon) mp.getGeometryN(0);
		}
		MyRaster mr = new MyRaster(p, resolution, radius, kdeType, Color.BLACK);
		
		int counter = 0;
		int multiplier = 1;
		if(splitHourOfDay){
			//TODO Fix this so that it runs 24 times through the files.
		} else{
			if(activityType.equalsIgnoreCase("Minor") || activityType.equalsIgnoreCase("Both")){
				String s = String.format("%s%s/%d/%s/%04d/Sample%02d/Activities/%s_MinorLocations.txt", 
						root, studyAreaName, year, version, threshold, sample, studyAreaName);
				try {
					Scanner sc = new Scanner(new BufferedReader(new FileReader(new File(s))));
					String header = sc.nextLine();
					while(sc.hasNextLine()){
						String [] line = sc.nextLine().split(",");
						Point point = gf.createPoint(new Coordinate(Double.parseDouble(line[1]),Double.parseDouble(line[2])));
						mr.processPoint(point);
						
						if(++counter == multiplier){
							// Report progress.
							log.info("   ...lines processed: " + counter);
							multiplier *= 2;
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} 
			if(activityType.equalsIgnoreCase("Major") || activityType.equalsIgnoreCase("Both")){
				String s = String.format("%s%s/%d/%s/%04d/Sample%02d/Activities/%s_MajorLocations.txt", 
						root, studyAreaName, year, version, threshold, sample, studyAreaName);
				try {
					Scanner sc = new Scanner(new BufferedReader(new FileReader(new File(s))));
					String header = sc.nextLine();
					while(sc.hasNextLine()){
						String [] line = sc.nextLine().split(",");
						Point point = gf.createPoint(new Coordinate(Double.parseDouble(line[1]),Double.parseDouble(line[2])));
						mr.processPoint(point);
						
						if(++counter == multiplier){
							// Report progress.
							log.info("   ...lines processed: " + counter);
							multiplier *= 2;
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}			
			}	
			log.info("   ...lines processed: " + counter + " (Done)");
			mr.convertMatrixToRaster();
			String output = String.format("%s%s/%d/%s/%04d/Sample%02d/Activities/%s_KDE_%s_%s_%s_%s_%s.png", 
					root, studyAreaName, year, version, threshold, sample, studyAreaName, studyAreaName, args[0], args[1], args[2], args[3], args[4]);
			mr.writeMyRasterToFile(output, "png");
			
			log.info("=================================================");
			log.info("              PROCESS COMPLETED");
			log.info("-------------------------------------------------");
			log.info("Arguments:");
			log.info("             Activity type: " + args[0]);
			log.info("    Split hours of the day: " + args[1]);
			log.info("                Resolution: " + args[2]);
			log.info("                    Radius: " + args[3]);
			log.info("                  KDE type: " + args[4]);
			log.info("=================================================");
			
//			for(int a = 0; a < mr.)
		}
		
		
	}

}
