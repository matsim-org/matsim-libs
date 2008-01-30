/* *********************************************************************** *
 * project: org.matsim.*
 * Veh2Kml.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.basic.v01.Id;
import org.matsim.mobsim.snapshots.KmlSnapshotWriter;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.transformations.GK4toWGS84;
import org.matsim.utils.geometry.transformations.IdentityTransformation;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.ArgumentParser;

/**
 * Converts a TRANSIMS vehicle file into a google earth file.
 * 
 * @author mrieser
 */
public class Veh2Kml {

	private String vehfile = null;
	private String kmlFile = null;
	private double percentage = 1.0;
	private double minX = Double.NEGATIVE_INFINITY;
	private double minY = Double.NEGATIVE_INFINITY;
	private double maxX = Double.POSITIVE_INFINITY;
	private double maxY = Double.POSITIVE_INFINITY;
	private CoordinateTransformationI coordTransform = new IdentityTransformation();
	
	private KmlSnapshotWriter writer = null;
	
	private double lastTime = -1.0;
	
	private int cntPositions = 0;
	private int cntTimesteps = 0;
	
	public Veh2Kml(String[] args) {
		parseArguments(args);
	}

	public void run() {
		System.out.println("conversion started...");
		convert();
		System.out.println("conversion finished.");
		System.out.println(" timesteps: " + this.cntTimesteps);
		System.out.println(" positions: " + this.cntPositions);
		
	}

	private void printUsage() {
		System.out.println();
		System.out.println("Veh2Kml");
		System.out.println("Converts a transims vehicle file to a keyhole markup file for Google Earth.");
		System.out.println();
		System.out.println("usage: Veh2Kml [OPTIONS] vehfile kmlfile");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-p percentage:  Only write a certain percentage [0-100] of all vehicles.");
		System.out.println("                Google Earth may be very slow if too many vehicles are loaded.");
		System.out.println("-c coordinates: Specifies the coordinate system used in the vehicle file.");
		System.out.println("                If this option is specified, the coordinates will be trans-");
		System.out.println("                formed from the specified coordinate system to WGS84 used by");
		System.out.println("                Google Earth.");
		System.out.println("                Currently supported coordiante systems:");
		System.out.println("                - WGS84");
		System.out.println("                - CH1903_LV03, the swiss national grid coordinates");
		System.out.println("                - GK4, Gauss-Krueger 4");
		System.out.println("                - Atlantis, for synthetical coordinate systems");
		System.out.println("--area min_x,min_y,max_x,max_y|preset:");
		System.out.println("                Only convert vehicle locations that are within the specified");
		System.out.println("                area. The area is described by the coordinates used in the");
		System.out.println("                transims vehicle file.");
		System.out.println("                Available presets are:");
		System.out.println("                * berlin (GK4: 4580000,5807000,4617000,5835000)");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2007, matsim.org");
		System.out.println();
	}

	/**
	 * Parses all arguments and sets the corresponding members.  
	 *
	 * @param args
	 */
	private void parseArguments(String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		while (argIter.hasNext()) {
			String arg = argIter.next();
			if (arg.equals("-p")) {
				ensureNextElement(argIter);
				arg = argIter.next();
				try {
					double percentage = Double.parseDouble(arg);					
					this.percentage = percentage / 100.0;
				} catch (Exception e) {
					System.out.println("Cannot understand argument: -p " + arg);
					printUsage();
					System.exit(1);
				}
			} else if (arg.equals("-c")) {
				ensureNextElement(argIter);
				arg = argIter.next();
				try {
					this.coordTransform = TransformationFactory.getCoordinateTransformation(arg, TransformationFactory.WGS84);
				} catch (IllegalArgumentException e) {
					System.out.println("Unknown coordinate system: " + arg);
					printUsage();
					System.exit(1);
				}
			} else if (arg.equals("-h") || arg.equals("--help")) {
				printUsage();
				System.exit(0);
			} else if (arg.equals("--area")) {
				ensureNextElement(argIter);
				arg = argIter.next();
				if (arg.equals("berlin")) {
					this.minX = 4580000;
					this.minY = 5807000;
					this.maxX = 4617000;
					this.maxY = 5835000;
					this.coordTransform = new GK4toWGS84();
				} else {
					String[] bounds = arg.split(",");
					if (bounds.length != 4) {
						System.out.println("Area bounds not recognized.");
						System.exit(1);
					}
					this.minX = Double.parseDouble(bounds[0]);
					this.minY = Double.parseDouble(bounds[1]);
					this.maxX = Double.parseDouble(bounds[2]);
					this.maxY = Double.parseDouble(bounds[3]);
				}
			} else if (arg.startsWith("-")) {
				System.out.println("Unrecognized option " + arg);
				System.exit(1);
			} else {
				this.vehfile = arg;
				ensureNextElement(argIter);
				this.kmlFile = argIter.next();
				if (argIter.hasNext()) {
					System.out.println("Too many arguments.");
					printUsage();
					System.exit(1);
				}
			}
		}
	}
	
	/**
	 * Helper function to ensure there is at least one next element in the iterator.
	 * If not, the program exits with the message "Too few arguments". 
	 *
	 * @param iter
	 */
	private void ensureNextElement(Iterator<String> iter) {
		if (!iter.hasNext()) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
	}
	
	private void convert() {

		this.writer = new KmlSnapshotWriter(this.kmlFile, this.coordTransform);

		// read and convert data from veh-file
		
		BufferedReader reader = null;
		try {
			reader = IOUtils.getBufferedReader(this.vehfile);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		try {
			reader.readLine(); // header, we do not use it
			String line = null;
			while ( (line = reader.readLine()) != null) {
	
				String[] result = line.split("\t");
				if (result.length == 16) {
					double easting = Double.parseDouble(result[11]);
					double northing = Double.parseDouble(result[12]);
					if (easting >= minX && easting <= maxX && northing >= minY && northing <= maxY) {
						String agent = result[0];
						String time = result[1];
//					String dist = result[5];
						String speed = result[6];
						String elevation = result[13];
						String azimuth = result[14];
						PositionInfo position = new PositionInfo(new Id(agent), easting, northing,
								Double.parseDouble(elevation), Double.parseDouble(azimuth), Double.parseDouble(speed), PositionInfo.VehicleState.Driving,null);
						addVehicle(Double.parseDouble(time), position);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		finish();
	}

	private void addVehicle(double time, PositionInfo position) {
		if (Math.random() >= this.percentage) return;
		this.cntPositions++;

		if (time != this.lastTime) {
			this.cntTimesteps++;
			// the time changes
			if (this.lastTime >= 0) {
				this.writer.endSnapshot();
			}
			this.writer.beginSnapshot(time);
			this.lastTime = time;
		}			

		this.writer.addAgent(position);
//		if (speed == 0.0) {
//			this.redMultigeom.addGeometry(point);
//		} else if (speed <= 30.0/3.6) {
//			this.yellowMultigeom.addGeometry(point);
//		} else {
//			this.greenMultigeom.addGeometry(point);
//		}
	}
	
	private void finish() {
		if (this.lastTime >= 0) {
			this.writer.endSnapshot();
		}
		this.writer.finish();
	}

	public static void main(String[] args) {
		Veh2Kml app = new Veh2Kml(args);
		app.run();
	}

}
