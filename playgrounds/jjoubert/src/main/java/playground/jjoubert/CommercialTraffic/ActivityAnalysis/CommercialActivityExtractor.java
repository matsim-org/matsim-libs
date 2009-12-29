/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractActivityDurations.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.GPSPoint;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * A class to extract a <code>List</code> of commercial <code>Activity</code>s from
 * raw vehicle GPS logs, such as those provided by <i>DigiCore Holdings (Ltd)</i> for
 * the South African study.  
 * @author johanwjoubert
 *
 */
public class CommercialActivityExtractor {
	private final Logger log = Logger.getLogger(CommercialActivityExtractor.class);
	private final String fromCoordinateSystem;
	private final String toCoordinateSystem;
	private List<Integer> startSignals;
	private List<Integer> stopSignals;
	private int totalBoinkPoints = 0;
	
	/**
	 * Creates an instance of the object <i>without</i> specifying coordinate systems.
	 * This constructor is useful when you do not want to process read and process raw
	 * input data, <i>or</i> when you do not need to transform the raw data from one
	 * coordinate system to another.
	 */
	public CommercialActivityExtractor(){
		this(null,null);		
	}

	/**
	 * Creates an instance of the object. This constructor is useful when the raw input
	 * data is in a different coordinate system than what you need the <code>Activity</code>s
	 * to be in.
	 * @param fromCoordinateSystem the coordinate system in which the data currently is, 
	 * 		expressed in the <i>Well Known Text</i> (WKT) format for coordinate systems.
	 * @param fromCoordinateSystem the coordinate system to which the data must be 
	 * 		transformed, expressed in the <i>Well Known Text</i> (WKT) format for 
	 * 		coordinate systems.
	 */
	public CommercialActivityExtractor(String fromCoordinateSystem, 
									   String toCoordinateSystem){
		this.fromCoordinateSystem = fromCoordinateSystem;
		this.toCoordinateSystem = toCoordinateSystem;
		this.startSignals = null;
		this.stopSignals = null;
		log.info("A CommercialActivityExtractor has been created");
	}
	
	/**
	 * Returns a list of <code>Activity</code>s.
	 * @param file the single vehicle file. The comma-separated flat file layout is 
	 * 		assumed to be something like the following example. <i>NOTE: a single header
	 * 		line is assumed.</i></p>
	 * 		<ul><i>Example:</i>
	 * 		<p><ul><code>
	 * 		VehicleID,Time,Long,Lat,Status,Speed<br>
	 * 		211,1199218558,31.899667,-28.782367,143,0<br>
	 *		211,1199250226,31.899733,-28.782317,18,0<br>
	 *		211,1199250525,31.899667,-28.782283,17,0<br>
	 *		...</code></p></ul>
	 *		where the columns are:
	 *		<p>
	 *		<code>VehicleId</code>, a unique vehicle identifier from the vehicle sending 
	 * 				the GPS log entry;<br> 
	 * 		<code>Time</code>, a UNIX-based time stamp (in seconds);<br>
	 * 		<code>Long</code>, the x-coordinate (longitude) of the GPS log (in decimal 
	 * 				degrees);<br>
	 * 		<code>Lat</code>, the y-coordinate (latitude) of the GPS log (in decimal 
	 * 				degrees); <br>
	 * 		<code>Status</code>, an integer digit indicating a predefined vehicle status. 
	 * 				Status codes are described in <code>Statuses.xls</code> as provided 
	 * 				by <i>DigiCore</i>;<br>
	 * 		<code>Speed</code>, a field reflecting the speed of the vehicle when sending 
	 * 				the log entry. This field, however, proved to be quite useless.
	 *		</p></ul>
	 * @return an <code>ArrayList</code> of <code>Activity</code>s.
	 */
	public List<Activity> extractActivities(File file){
		int boinkPoints = 0;
		// Read every data line in the file as GPSPoint.
		List<GPSPoint> vehicleLog = readGpsPoints(file);
		
		// Extract activities from vehicleLog.
		List<Activity> result = processVehicleLog(vehicleLog);
		
		//log.info("Extracted " + result.size() + " activities from vehicle " + file.getName() + ", of which " + boinkPoints + " (" + ((int)(((double) boinkPoints / (double) result.size())*100)) + "%) are 'boink'");
		totalBoinkPoints += boinkPoints;
		return result;
	}
	
	public int getTotalBoinkPoints() {
		return totalBoinkPoints;
	}

	/**
	 * The private method reads every line from the input file, and converts each line
	 * to and object of type <code>GPSPoint</code>.
	 * @param file the input file to be read.
	 * @return a <code>List</code> of <code>GPSPoint</code>s.
	 */
	private List<GPSPoint> readGpsPoints(File file) { 
		int vehID;
		long time;
		double longitude;
		double latitude;
		int status;
		MathTransform mt = null;
		
		if(fromCoordinateSystem != null && toCoordinateSystem != null){
			mt = getMathTransform();
		}

		List<GPSPoint> vehicleLog = new ArrayList<GPSPoint>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(file) ) );
			try{
				input.nextLine();
				int lineNumber = 1;
				while( input.hasNextLine() ){
					String [] inputString = input.nextLine().split(",");
					if( inputString.length == 6){
						try{
							vehID = Integer.parseInt( inputString[0] );
							time =  Long.parseLong( inputString[1] );
							longitude = Double.parseDouble( inputString[2] );
							latitude = Double.parseDouble( inputString[3] );
							status = Integer.parseInt( inputString[4] );
							// I decided to not read in the speed... useless in DigiCore set

							Coordinate c = new Coordinate( longitude, latitude );
							// Transform the coordinate if 
							if(mt != null){
								JTS.transform(c, c, mt);
							}
							vehicleLog.add( new GPSPoint(vehID, time, status, c) );
						} catch(NumberFormatException e2){
							e2.printStackTrace();
						} catch(Exception e3){
							// Points with coordinates outside the range (±90º) are ignored.
//							e3.printStackTrace();						
						}
					} else{
						log.warn("Line number " + lineNumber + " of vehicle " + file.getName() + "does not have the right fields, and have been omitted!");
					}
					lineNumber++;
				}
			} finally{
				input.close();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		return vehicleLog;
	}

	
	private List<Activity> processVehicleLog(List<GPSPoint> vehicleLog) {
		List<Activity> activityList = new ArrayList<Activity>(); 
		List<GPSPoint> locationList = new ArrayList<GPSPoint>();
		
		findActivityStartSignal(vehicleLog);
		if(vehicleLog.size() > 0){
			locationList.add(vehicleLog.get(0));
		}
		
		while(vehicleLog.size() > 1){
			/*
			 * Remove all the points until an activity stop signal is found, i.e. a 
			 * vehicle start signal is found.
			 */
			if( !startSignals.contains(vehicleLog.get(1).getStatus())){
				locationList.add( vehicleLog.get(1));
				vehicleLog.remove(1);
			} else{
				// Get the centre of gravity of the possible locations.
				double x = 0;
				double y = 0;
				if( locationList.size() > 0 ){
					double xSum = 0;
					double ySum = 0;
					for( GPSPoint gps: locationList ){
						xSum += gps.getCoordinate().x;
						ySum += gps.getCoordinate().y;
					}
					x = xSum / locationList.size();
					y = ySum / locationList.size();
				} else{
					log.error("There does not seem to be a possible location for this activity?!");
				}
				/*
				 * I am adjusting the activity start location to the calculated centre of 
				 * gravity. A new activity is then created, using the adjusted start 
				 * location as the activity location.
				 * TODO I may want to make a call here, and DROP this activity if the start
				 * and end locations of the activity is different, which I call here a 'boink'
				 * point. I have arbitrarily chosen the 'boink' threshold as 1000m.
				 */
				vehicleLog.get(0).setCoordinate(new Coordinate(x, y) );
				Activity a = new Activity(vehicleLog.get(0).getTime(), vehicleLog.get(1).getTime(), vehicleLog.get(0));
				if(a.getDuration() > 0){
					activityList.add(a);
				}

				double distance = (vehicleLog.get(1).getCoordinate().distance(vehicleLog.get(0).getCoordinate()));
				if(distance > 1000){
					totalBoinkPoints++;
				}
				
				vehicleLog.remove(0);
				vehicleLog.remove(0);
				locationList = new ArrayList<GPSPoint>();
				
				findActivityStartSignal(vehicleLog);
				if(vehicleLog.size() > 0){
					locationList.add(vehicleLog.get(0));
				}
			}
		}
		return activityList;
	}

	private void findActivityStartSignal(List<GPSPoint> vehicleLog) {
		boolean startFound = false;
		while( !startFound && vehicleLog.size() > 0){
			if(!stopSignals.contains(vehicleLog.get(0).getStatus())){
				vehicleLog.remove(0);
			} else{
				startFound = true;
			}			
		}
	}


	/**
	 * Gets the mathematical transformation to convert a value, say a coordinate, from 
	 * one coordinate system to another. The coordinate system, in <i>Well Known Text</i>
	 * format, should have been provided in the constructor.
	 * @return The mathematical transformation of type <code>org.opengis.referencing.operation.MathTransform</code>
	 */
	private MathTransform getMathTransform() {
		MathTransform mt = null;

			CoordinateReferenceSystem sourceCRS;
			try {
				sourceCRS = CRS.parseWKT(fromCoordinateSystem);
				final CoordinateReferenceSystem targetCRS = CRS.parseWKT(toCoordinateSystem);
				mt = CRS.findMathTransform(sourceCRS, targetCRS, true);
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		return mt;
	}
	
	/**
	 * <p>This methods reads the <b><u>vehicle</u></b> <i>start</i> and <i>stop</i> 
	 * signals used to identify activities from GPS logs where a status is available, 
	 * such as the data provided by <i>DigiCore</i> in the South African commercial 
	 * vehicle study. The user should not get confused: these are <i>vehicle</i> stop 
	 * signals, i.e. a vehicle stop signal might be something like <i>`ignition off'</i>.
	 * The two-line, comma-separated file format of the file must be as shown in the 
	 * example below, and the status codes are only allowed to be <i>integer</i> values.</p>
	 * 		<p><i>Example:</i>
	 * 		<ul><code>
	 * 			Start,2,3,4,5,16,18,20<br>
	 * 			Stop,1,6,7,8,9,10,11,12<br>
	 *  	</code></ul>
	 * Only the first two lines of the signal file will be read. Ensure thus that the 
	 * first line of the file is not blank.
	 * @param string the absolute path of the file containing the <i>start</i> and 
	 * 		<i>stop</i> signals.
	 */
	public void readSignals(String string){
		startSignals = new ArrayList<Integer>();
		stopSignals = new ArrayList<Integer>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(string))));
			try{
				String[] listStart = input.nextLine().split(",");
				if(listStart[0].equalsIgnoreCase("start")){
					for(int i = 1; i < listStart.length; i++){
						startSignals.add(Integer.parseInt(listStart[i]));
					}
					if(startSignals.size()==0){
						log.warn("No start signals were identified!");
					}
				} else{
					log.error("The first line of the signal file does not start with 'Start'");
					throw new RuntimeException("The signal file is in the wrong format!");
				}
				String[] listStop = input.nextLine().split(",");
				if(listStop[0].equalsIgnoreCase("stop")){
					for(int i = 1; i < listStop.length; i++){
						stopSignals.add(Integer.parseInt(listStop[i]));
					}
					if(stopSignals.size()==0){
						log.warn("No stop signals were identified!");
					}
				} else{
					log.error("The second line of the signal file does not start with 'Stop'");
					throw new RuntimeException("The signal file is in the wrong format!");
				}
			} finally{
				input.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * A method that converts a <code>List</code> of <code>Activity</code>s into a 
	 * <code>List</code> of <code>Point</code>s. The purpose of the list conversion is 
	 * to allow the activities to be clustered using the <code>DJCLuster</code> class, 
	 * whose input must be a <code>List</code> of <code>Point</code>s.  
	 * @param activities the <code>List</code> of <code>Activity</code>s.
	 * @return a <code>List</code> of <code>Point</code>s without any additional 
	 * 		attributes from the <code>Activity</code>s. 
	 */
	public List<Point> convertActivityToPoint(List<Activity> activities){
		List<Point> points = new ArrayList<Point>(activities.size());
		GeometryFactory gf = new GeometryFactory();
		for (Activity activity : activities) {
			points.add(gf.createPoint(activity.getLocation().getCoordinate()));
		}
		return points;
	}

}
