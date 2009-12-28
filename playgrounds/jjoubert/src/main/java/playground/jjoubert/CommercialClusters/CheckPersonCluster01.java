/* *********************************************************************** *
 * project: org.matsim.*
 * CheckPersonCluster01.java
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

package playground.jjoubert.CommercialClusters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.Utilities.Clustering.DJCluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class CheckPersonCluster01 {

	private final static Logger log = Logger.getLogger(CheckPersonCluster01.class);
	private static double distanceThreshold = 100;
	private static float clusterRadius = 10;
	private static int clusterMinimumPoints = 20; 
	private static String person = "3592";

	public static void main(String args[]){
		log.info("================================================================");
		log.info("   Testing DJ-Cluster on person GPS records.");
		log.info("================================================================");
		
		log.info("Reading activity statistics file");
		String root = "/Users/johanwjoubert/Desktop/Temp/Schuessler/";
//		String inputFilename = root + "ActivityStatistics.txt";
		String inputFilename = root + "Raw/ID" + person + ".RAW";
//		ArrayList<Point> pointsToCluster = readActivityStatistics(inputFilename);
		ArrayList<Point> pointsToCluster = readRawDataToArrayList(inputFilename);
//		QuadTree<Point> qt1 = readRawDataToQuadTree(inputFilename);		
		
		DJCluster djc = new DJCluster(clusterRadius, clusterMinimumPoints, pointsToCluster);
		djc.clusterInput();
		
		log.info("Visualising clusters.");

//		String pointFilename = root + "Point_" + ((int)clusterRadius) + "_" + clusterMinimumPoints + ".txt";
//		String lineFilename = root + "Line_" + ((int)clusterRadius) + "_" + clusterMinimumPoints + ".txt";
//		String clusterFilename = root + "Cluster_" + ((int)clusterRadius) + "_" + clusterMinimumPoints + ".txt";

		String pointFilename = root + "Person" + person + "Point_" + ((int)clusterRadius) + "_" + clusterMinimumPoints + ".txt";
		String lineFilename = root + "Person" + person + "Line_" + ((int)clusterRadius) + "_" + clusterMinimumPoints + ".txt";
		String clusterFilename = root + "Person" + person + "Cluster_" + ((int)clusterRadius) + "_" + clusterMinimumPoints + ".txt";
		djc.visualizeClusters(pointFilename, clusterFilename, lineFilename, null);		
		log.info("Completed");
	}
	
	private static ArrayList<Point> readActivityStatistics(String filename){
		GeometryFactory gf = new GeometryFactory();
		ArrayList<Point> al = new ArrayList<Point>();
		double xMin = Double.POSITIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(filename))));
			
			input.nextLine();
			int lineCounter = 0;
			int lineMultiplier = 1;
			while(input.hasNextLine()){
				String[] line = input.nextLine().split("\t");
				if(line.length == 15){
					Point p1 = gf.createPoint(new Coordinate(Double.parseDouble(line[5]), Double.parseDouble(line[6]), Double.parseDouble(line[7])));
					Point p2 = gf.createPoint(new Coordinate(Double.parseDouble(line[10]), Double.parseDouble(line[11]), Double.parseDouble(line[12])));
					if(p1.distance(p2) < distanceThreshold){
						Coordinate c1 = p1.getCoordinate();
						Coordinate c2 = p2.getCoordinate();
						Coordinate [] c = {c1,c2};
						LineString ls = gf.createLineString(c);
						Point p;
						if(ls.getLength() == 0){
							p = p1;
						} else{
							p = ls.getCentroid();
						}
						Double pX = Double.valueOf(p.getX());
						if(pX.isNaN()){
							log.info("A NaN found!!");
						}
						xMin = Math.min(xMin, p.getX());
						yMin = Math.min(yMin, p.getY());
						xMax = Math.max(xMax, p.getX());
						yMax = Math.max(yMax, p.getY());
						al.add(p);						
					}	
				}
				lineCounter++;
				// Report progress.
				if(lineCounter == lineMultiplier){
					log.info("   Lines read: " + lineCounter);
					lineMultiplier *= 2;
				}
			}
			log.info("   Lines read: " + lineCounter + " (Done)");
			log.info("Completed processing input file (" + al.size() + " points)");
			
			log.info("Building QuadTree from points.");
			int qtCounter = 0;
			int qtMultiplier = 1;
			QuadTree<Point> qt = new QuadTree<Point>(xMin, yMin, xMax, yMax);
			for (Point point : al) {
				qt.put(point.getX(), point.getY(), point);
				qtCounter++;
				// Report progress
				if(qtCounter == qtMultiplier){
					qtMultiplier *= 2;
					log.info("   Points added: " + qtCounter);
				}
			}
			log.info("   Points added: " + qtCounter + " (Done)");
			return al;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static ArrayList<Point> readRawDataToArrayList(String filename){
		GeometryFactory gf = new GeometryFactory();
		ArrayList<Point> al = new ArrayList<Point>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(filename))));
			
			boolean foundStart = false;
			while(input.hasNextLine() && !foundStart){
				String headerLine = input.nextLine();
				if(headerLine.equalsIgnoreCase("DATA")){
					foundStart = true;
				}
			}
			int lineCounter = 0;
			int lineMultiplier = 1;
			while(input.hasNextLine()){
				String [] line = input.nextLine().split(",");
				if(line.length == 6){
					Point p = gf.createPoint(new Coordinate(Double.parseDouble(line[1]), Double.parseDouble(line[2])));
					al.add(p);
				}
				
				lineCounter++;
				// Report progress
				if(lineCounter == lineMultiplier){
					log.info("   Lines processed: " + lineCounter);
					lineMultiplier *= 2;
				}
			}
			log.info("   Lines processed: " + lineCounter + " (Done)");
			log.info("Completed processing input file (" + al.size() + " points)");
			return al;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static QuadTree<Point> readRawDataToQuadTree(String filename){
		double xMin = Double.POSITIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;

		ArrayList<Point> al = readRawDataToArrayList(filename);
		for(Point p : al){
			xMin = Math.min(xMin, p.getX());
			yMin = Math.min(yMin, p.getY());
			xMax = Math.max(xMax, p.getX());
			yMax = Math.max(yMax, p.getY());			
		}

		log.info("Building QuadTree from points.");
		int qtCounter = 0;
		int qtMultiplier = 1;
		QuadTree<Point> qt = new QuadTree<Point>(xMin, yMin, xMax, yMax);
		for (Point point : al) {
			boolean added = qt.put(point.getX(), point.getY(), point);
			if(added){
				qtCounter++;
			}
			// Report progress
			if(qtCounter == qtMultiplier){
				qtMultiplier *= 2;
				log.info("   Points added: " + qtCounter);
			}
		}
		log.info("   Points added: " + qtCounter + " (Done)");
		return qt;
	}

}
