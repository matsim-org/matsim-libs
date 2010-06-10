/* *********************************************************************** *
 * project: org.matsim.*
 * MyThroughTrafficAnalyser.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.CommercialVehicle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class MyThroughTrafficAnalyser {
	private Logger log = Logger.getLogger(MyThroughTrafficAnalyser.class);
	private MultiPolygon studyArea;
	private Polygon studyAreaEnvelope;
	private List<Point> gates;
	private QuadTree<Coordinate> entryQT;
	private QuadTree<Coordinate> exitQT;
	private List<List<Double>> entryLineList;
	private List<List<Double>> exitLineList;
	
	public MyThroughTrafficAnalyser(MultiPolygon studyArea, List<Point> gates) {
		this.studyArea = studyArea;
		this.studyAreaEnvelope = (Polygon) studyArea.getEnvelope();
		this.gates = gates;
		Geometry g = studyArea.getEnvelope();
		this.entryQT = new QuadTree<Coordinate>(g.getCoordinates()[0].x, 
				g.getCoordinates()[0].y, 
				g.getCoordinates()[2].x, 
				g.getCoordinates()[2].y);
		this.exitQT = new QuadTree<Coordinate>(g.getCoordinates()[0].x, 
				g.getCoordinates()[0].y, 
				g.getCoordinates()[2].x, 
				g.getCoordinates()[2].y);
		this.entryLineList = new ArrayList<List<Double>>();
		this.exitLineList = new ArrayList<List<Double>>();
	}

	public void processVehicle(CommercialVehicle v) {
		GeometryFactory gf = new GeometryFactory();
		for (Chain c : v.getChains()){
			for (int i = 0; i < c.getActivities().size()-1; i++){
				Point p1 = gf.createPoint(c.getActivities().get(i).getLocation().getCoordinate());
				Point p2 = gf.createPoint(c.getActivities().get(i+1).getLocation().getCoordinate());
				
				boolean in1 = false;
				if(studyAreaEnvelope.contains(p1)){
					if(studyArea.contains(p1)){
						in1 = true;
					}
				}
				boolean in2 = false;
				if(studyAreaEnvelope.contains(p2)){
					if(studyArea.contains(p2)){
						in2 = true;
					}
				}
				
				/*
				 * Only process the combination if ONE of the two is within 
				 * the study area. 
				 */
				if((in1 && !in2) || (!in1 && in2)){
					Coordinate[] cs = {p1.getCoordinate(), p2.getCoordinate()};
					LineString ls = gf.createLineString(cs);
					LineString lsIn = (LineString) ls.intersection(studyArea).getGeometryN(0); // Just get first point.
					Coordinate coord;
					List<Double> list = new ArrayList<Double>(6);
					if(lsIn == null){
						log.warn("Could not find the intersection.");
					}
					if(!in1 && in2){
						/*
						 * It is an entry.
						 */
						coord = lsIn.getCoordinateN(0);
						entryQT.put(coord.x, coord.y, coord);
						list.add(p1.getX());
						list.add(p1.getY());
						list.add(coord.x);
						list.add(coord.y);
						list.add(p2.getX());
						list.add(p2.getY());						
						entryLineList.add(list);						
					} else{
						/*
						 * It is an exit.
						 */
						coord = lsIn.getCoordinateN(lsIn.getNumPoints()-1);
						exitQT.put(coord.x, coord.y, coord);
						list.add(p1.getX());
						list.add(p1.getY());
						list.add(coord.x);
						list.add(coord.y);
						list.add(p2.getX());
						list.add(p2.getY());
						exitLineList.add(list);
					}
				}
			}
		}
	}
	
	public List<List<Double>> getEntryLineList() {
		return entryLineList;
	}

	public List<List<Double>> getExitLineList() {
		return exitLineList;
	}

	public void writeListsToFile(String location) {
		writeListToFile(entryLineList, location + "Entry");
		writeListToFile(exitLineList, location + "Exit");		
	}
	
	private void writeListToFile(List<List<Double>> list, String location){
		log.info("Writing list to " + location + "Line.txt");
		try {
			File f1 = new File(location + "Line.txt");
			boolean f1create = f1.createNewFile();
			if(!f1create){
				log.warn("Cannot create " + f1.getAbsolutePath());
			}
			if(!f1.canWrite()){
				log.warn("Cannot write to " + f1.getAbsolutePath());
			}
			File f2 = new File(location + "Point.txt");
			boolean f2create = f1.createNewFile();
			if(!f2create){
				log.warn("Cannot create " + f2.getAbsolutePath());
			}
			if(!f2.canWrite()){
				log.warn("Cannot write to " + f2.getAbsolutePath());
			}
			BufferedWriter o1 = new BufferedWriter(new FileWriter(f1));			
			BufferedWriter o2 = new BufferedWriter(new FileWriter(f2));			
			try{
				o1.write("ID");
				o1.newLine();
				o2.write("ID,X,Y");
				o2.newLine();
				int id = 0;
				for (List<Double> l : list){
					/*
					 * Write the line to output 1.
					 */
					o1.write(String.valueOf(id));
					o1.newLine();
					o1.write(String.valueOf(l.get(0)));
					o1.write(",");
					o1.write(String.valueOf(l.get(1)));
					o1.newLine();
					o1.write(String.valueOf(l.get(4)));
					o1.write(",");
					o1.write(String.valueOf(l.get(5)));
					o1.newLine();
					o1.write("END");
					o1.newLine();
					
					/*
					 * Write the point to output 2.
					 */
					o2.write(String.valueOf(id));
					o2.write(",");
					o2.write(String.valueOf(l.get(2)));
					o2.write(",");
					o2.write(String.valueOf(l.get(3)));
					o2.newLine();
					id++;
				}
				o1.write("END");
				o2.write("END");
			} finally{
				o1.close();
				o2.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("List written successfully. Number of entries: " + list.size());
	}


	
}
