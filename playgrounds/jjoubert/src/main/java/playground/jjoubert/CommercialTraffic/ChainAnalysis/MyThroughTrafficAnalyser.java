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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.Activity;
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
	private List<List<Integer>> gateStatsIn;
	private List<List<Integer>> gateStatsOut;
	private List<Integer> activityCounterList;
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
		this.activityCounterList = new ArrayList<Integer>();
		
		// Create the basic structure for gate statistics.
		this.gateStatsIn = new ArrayList<List<Integer>>(gates.size());
		this.gateStatsOut = new ArrayList<List<Integer>>(gates.size());
		for(Point p : gates){
			List<Integer> gateLineIn = new ArrayList<Integer>(26);
			List<Integer> gateLineOut = new ArrayList<Integer>(26);
			for(int i = 0; i < 26; i++){
				gateLineIn.add(new Integer(0));
				gateLineOut.add(new Integer(0));
			}
			// Set the first two entries as the gate's coordinates.
			gateLineIn.set(0, (int) Math.round(p.getX()));
			gateLineIn.set(1, (int) Math.round(p.getY()));
			gateStatsIn.add(gateLineIn);

			gateLineOut.set(0, (int) Math.round(p.getX()));
			gateLineOut.set(1, (int) Math.round(p.getY()));
			gateStatsOut.add(gateLineOut);
		}
	}

	public void processVehicle(CommercialVehicle v) {
		GeometryFactory gf = new GeometryFactory();
		for (Chain c : v.getChains()){
			Integer activityCounter = null;
			for (int i = 0; i < c.getActivities().size()-1; i++){
				Activity a1 = c.getActivities().get(i);
				Activity a2 = c.getActivities().get(i+1);				
				Point p1 = gf.createPoint(a1.getLocation().getCoordinate());
				Point p2 = gf.createPoint(a2.getLocation().getCoordinate());
				
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
					/*
					 * Add the following values to the list:
					 * 	- origin's x-coordinate;
					 *  - origin's y-coordinate;
					 *  - entry point's x-coordinate;
					 *  - entry point's y-coordinate;
					 *  - destination's x-coordinate;
					 *  - destination's y-coordinate;
					 *  - entry point's hour of the day;
					 */
					List<Double> list = new ArrayList<Double>(6);
					if(lsIn == null){
						log.warn("Could not find the intersection.");
					}
					/*
					 * Variables to calculate the entry point's time of day.
					 */
					double fraction;
					long tripDuration = a2.getStartTime().getTimeInMillis() - a1.getEndTime().getTimeInMillis();
					GregorianCalendar entryTime = new GregorianCalendar(a1.getStartTime().getTimeZone(), new Locale("en", "ZA"));
					int hourOfDay;
					
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
						// Time of entry (hour of the day).
						fraction = (ls.getLength() - lsIn.getLength()) / ls.getLength();
						entryTime.setTimeInMillis((long) (a1.getEndTime().getTimeInMillis() + fraction*tripDuration));
						hourOfDay = entryTime.get(Calendar.HOUR_OF_DAY);
						list.add(Double.valueOf(hourOfDay));
						
						entryLineList.add(list);	
						
						activityCounter = new Integer(1);
						
						// Find the closest entry gate and add the details.
						double minD = Double.MAX_VALUE;
						int index = Integer.MAX_VALUE;
						for(int gate = 0; gate < gates.size(); gate++){
							double d = gates.get(gate).distance(gf.createPoint(coord));
							if(d < minD){
								minD = d;
								index = gate;
							}
						}
						gateStatsIn.get(index).set(hourOfDay+2, gateStatsIn.get(index).get(hourOfDay+2) + 1);
						
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
						// Time of entry (hour of the day).
						fraction = (lsIn.getLength()) / ls.getLength();
						entryTime.setTimeInMillis((long) (a1.getEndTime().getTimeInMillis() + fraction*tripDuration));
						hourOfDay = entryTime.get(Calendar.HOUR_OF_DAY);
						list.add(Double.valueOf(hourOfDay));
						
						exitLineList.add(list);
						
						if(activityCounter != null){
							activityCounterList.add(activityCounter);
							activityCounter = null;
						}
						// Find the closest entry gate and add the details.
						double minD = Double.MAX_VALUE;
						int index = Integer.MAX_VALUE;
						for(int gate = 0; gate < gates.size(); gate++){
							double d = gates.get(gate).distance(gf.createPoint(coord));
							if(d < minD){
								minD = d;
								index = gate;
							}
						}
						gateStatsOut.get(index).set(hourOfDay+2, gateStatsOut.get(index).get(hourOfDay+2) + 1);
					}

				} else if (in1 && in2 && activityCounter != null){
					activityCounter++;
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
		/*
		 * Write the entry and exit lines and points in a format readable and
		 * processable by the ET Geowizard in ArcGIS. 
		 */
		writeListToFile(entryLineList, location + "Entry");
		writeListToFile(exitLineList, location + "Exit");
		
		/*
		 * TODO Write all values in the entry and exit lists to a flat file.  
		 */
		
		
		/*
		 * Write activityCounter details.
		 */
		String activityCounterfilename = location + "ActivityCount.txt";
		log.info("Writing activity counter values to " + activityCounterfilename);
		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File(activityCounterfilename)));
			try {
				for(Integer i : activityCounterList){
					bw1.write(String.valueOf(i));
					bw1.newLine();
				}
			} finally {
				bw1.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		/*
		 * Write the gate statistics to file.
		 */
		String gateStatisticsInFilename = location + "GateStatisticsIn.txt";
		String gateStatisticsOutFilename = location + "GateStatisticsOut.txt";
		log.info("Writing gate statistics to " + location);
		try {
			BufferedWriter bw2In = new BufferedWriter(new FileWriter(new File(gateStatisticsInFilename)));
			BufferedWriter bw2Out = new BufferedWriter(new FileWriter(new File(gateStatisticsOutFilename)));
			try{
				bw2In.write("Long,Lat,H0,H1,H2,H3,H4,H5,H6,H7,H8,H9,H10,H11,H12,H13,H14,H15,H16,H17,H18,H19,H20,H21,H22,H23");
				bw2In.newLine();
				bw2Out.write("Long,Lat,H0,H1,H2,H3,H4,H5,H6,H7,H8,H9,H10,H11,H12,H13,H14,H15,H16,H17,H18,H19,H20,H21,H22,H23");
				bw2Out.newLine();
				
				for(int i = 0; i < gateStatsIn.size(); i++){
					List<Integer> lineIn = gateStatsIn.get(i);
					List<Integer> lineOut = gateStatsOut.get(i);
					for(int j = 0; j < lineIn.size()-1; j++){
						bw2In.write(String.valueOf(lineIn.get(j)));
						bw2In.write(",");
						bw2Out.write(String.valueOf(lineOut.get(j)));
						bw2Out.write(",");
					}
					bw2In.write(String.valueOf(lineIn.get(lineIn.size()-1)));
					bw2In.newLine();
					bw2Out.write(String.valueOf(lineOut.get(lineOut.size()-1)));
					bw2Out.newLine();
				}
			} finally {
				bw2In.close();
				bw2Out.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		

	}
	
	private void writeListToFile(List<List<Double>> list, String location){
		log.info("Writing list to " + location + "Line.txt");
		try {
			File f1 = new File(location + "Line.txt");
			log.info("   File f1 exist   : " + f1.exists());
			log.info("   File f1 created : " + f1.createNewFile());
			log.info("   File f1 writable: " + f1.canWrite());

			File f2 = new File(location + "Point.txt");
			log.info("   File f2 exist   : " + f2.exists());
			log.info("   File f2 created : " + f2.createNewFile());
			log.info("   File f2 writable: " + f2.canWrite());

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
