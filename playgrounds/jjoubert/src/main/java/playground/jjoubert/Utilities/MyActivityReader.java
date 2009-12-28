/* *********************************************************************** *
 * project: org.matsim.*
 * MyActivityReader.java
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

package playground.jjoubert.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class MyActivityReader {
	private final Logger log;
	
	public MyActivityReader(){
		log = Logger.getLogger(MyActivityReader.class);
	}
	
	public QuadTree<Point> readActivityPointsToQuadTree(String filename){
		log.info("Reading activities from " + filename);
		log.info("Reading all points: no study area provided.");
		GeometryFactory gf = new GeometryFactory();
		List<Point> points = new ArrayList<Point>();
		
		double x;
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double y;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( filename))));
			int lineCounter = 0;
			int lineMultiplier = 1;
			
			// Read header
			String[] line;
			input.nextLine();
			
			while(input.hasNextLine()){
				line = input.nextLine().split(",");
				if(line.length == 5){
					x = Double.parseDouble(line[1]);
					y = Double.parseDouble(line[2]);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					
					Point p = gf.createPoint(new Coordinate(x, y));
					points.add(p);

					lineCounter++;
					// Report progress
					if(lineCounter == lineMultiplier){
						log.info("   Lines processed: " + lineCounter);
						lineMultiplier *= 2;
					}
				}
			}
			log.info("   Lines processed: " + lineCounter + " (Done)");
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		log.info("Building QuadTree from activity points.");
		QuadTree<Point> qt = new QuadTree<Point>(minX, minY, maxX, maxY);
		for (Point p : points) {
			qt.put(p.getX(), p.getY(), p);
		}
		log.info("QuadTree completed.");
		
		return qt;
	}
	
	public QuadTree<Point> readActivityPointsToQuadTree(String filename, MultiPolygon studyArea){
		log.info("Reading activities from " + filename);
		log.info("Reading only points in the given study area provided.");
		List<Point> points = readActivityPointsToList(filename, studyArea);
		
		// Calculate the extent of the QuadTree.
		double minX = Double.MAX_VALUE;
		double maxX = Double.MAX_VALUE;
		double minY = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for (Point point : points) {
			minX = Math.min(minX, point.getX());
			minY = Math.min(minY, point.getY());
			maxX = Math.max(maxX, point.getX());
			maxY = Math.max(maxY, point.getY());
		}

		log.info("Building QuadTree from activity points.");
		QuadTree<Point> qt = new QuadTree<Point>(minX, minY, maxX, maxY);
		for (Point p : points) {
			qt.put(p.getX(), p.getY(), p);
		}
		log.info("QuadTree completed.");
		
		return qt;
	}
	
	
	public void filterActivity(String inputFilename, String outputFilename, MultiPolygon studyArea){
		log.info("Filtering the input file.");
		int lineCounter = 0;
		int lineMultiplier = 1;
		GeometryFactory gf = new GeometryFactory();

		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(inputFilename))));
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFilename)));
			
			try{
				String header = input.nextLine();
				output.write(header);
				output.newLine();

				while(input.hasNextLine()){
					String line = input.nextLine();
					String [] lineSplit = line.split(",");
					if(lineSplit.length == 5){
						double x = Double.parseDouble(lineSplit[1]);
						double y = Double.parseDouble(lineSplit[2]);

						Point p = gf.createPoint(new Coordinate(x, y));
						if(studyArea.contains(p)){
							output.write(line);
							output.newLine();
						}
					}
					
					lineCounter++;
					// Report progress
					if(lineCounter == lineMultiplier){
						log.info("   Lines processed: " + lineCounter);
						lineMultiplier *= 2;
					}
				}
				log.info("   Lines processed: " + lineCounter + " (Done)");
			} finally{
				output.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	public List<Point> readActivityPointsToList(String filename, MultiPolygon studyArea){
		log.info("Reading activities from " + filename);
		log.info("Reading only points in the given study area provided.");
		
		GeometryFactory gf = new GeometryFactory();
		List<Point> points = new ArrayList<Point>();
		
		Geometry envelope = studyArea.getEnvelope();
		
		double x;
		double y;
		int activityCounter = 0;
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( filename))));
			int lineCounter = 0;
			int lineMultiplier = 1;
			
			// Read header
			String[] line;
			input.nextLine();
			
			while(input.hasNextLine()){
				line = input.nextLine().split(",");
				if(line.length == 5){
					x = Double.parseDouble(line[1]);
					y = Double.parseDouble(line[2]);
					
					Point p = gf.createPoint(new Coordinate(x, y));
					if(envelope.contains(p)){
						points.add(p);
						activityCounter++;
					}

					lineCounter++;
					// Report progress
					if(lineCounter == lineMultiplier){
						log.info("   Lines processed: " + lineCounter);
						lineMultiplier *= 2;
					}
				}
			}
			log.info("   Lines processed: " + lineCounter + " (Done)");
			log.info("Total number of activities in study area: " + activityCounter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return points;
	}
	
}
