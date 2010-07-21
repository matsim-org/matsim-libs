/* *********************************************************************** *
 * project: org.matsim.*
 * RunMyThroughTrafficAnalyser.java
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

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import playground.jjoubert.CommercialTraffic.CommercialVehicle;
import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.MyXmlConverter;
import playground.jjoubert.Utilities.FileSampler.MyFileFilter;
import playground.jjoubert.Utilities.FileSampler.MyFileSampler;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class RunMyThroughTrafficAnalyser {
	private static Logger log = Logger.getLogger(RunMyThroughTrafficAnalyser.class);
	private static String root = "/home/jwjoubert/MATSim/MATSimData/";
	 /*=============================================================================
	 * String value that must be set. Allowed study areas are:						|
	 * 		- SouthAfrica															|
	 * 		- Gauteng																|
	 * 		- KZN																	|
	 * 		- WesternCape															|
	 *=============================================================================*/
	private static String studyAreaName;
	private static int year;
	private static String version;
	private static int threshold;
	private static int sample;
	private static double withinThreshold ;

	/**
	 * Implements the <code>MyThroughTrafficAnalyser</code> class. 
	 * @param args A number of arguments <b><i>MUST</i></b> be passed, and in 
	 * the following order:
	 * <ol>
	 * 	<li> <b>root</b> absolute path of the data root;
	 * 	<li> <b>studyArea</b> the name of the are considered, e.g. "Gauteng";
	 * 	<li> <b>year</b> indicating what <i>DigiCore</i> data set should be used;
	 * 	<li> <b>version</b> of the activity analysis to use;
	 * 	<li> <b>threshold</b> of activity duration used to distinguish between 
	 * 		 <i>minor</i> and <i>major</i> traffic, typically "300";
	 * 	<li> <b>sample</b> indicating which (of possibly many) samples of the
	 * 		 activity analysis to use; and
	 * 	<li> <b>withinThreshold</b> used to distinguish between <i>within</i> 
	 * 		 and <i>through</i> traffic, typically should be "0.6"
	 * </ol>
	 * An optional 8th argument may be passed indicating the number of vehicles 
	 * for which the chain analysis must be done. If not provided, all vehicle 
	 * files will be processed.
	 */
	public static void main(String[] args) {
		int numberOfVehiclesToSample;
		if(args.length == 8){
			numberOfVehiclesToSample = Integer.parseInt(args[7]);
		} else if(args.length == 7){
			numberOfVehiclesToSample = Integer.MAX_VALUE;
		} else{
			throw new RuntimeException("Improper number of arguments passed");
		}
		root = args[0];
		studyAreaName = args[1];
		year = Integer.parseInt(args[2]);
		version = args[3];
		threshold = Integer.parseInt(args[4]);
		sample = Integer.parseInt(args[5]);
		withinThreshold = Double.parseDouble(args[6]);
		
		/*
		 * Read study area.
		 */
		String studyAreaShapefile = String.format("%sShapefiles/%s/%s_UTM35S.shp",
				root, studyAreaName, studyAreaName);
		MyShapefileReader msr1 = new MyShapefileReader(studyAreaShapefile);
		MultiPolygon studyArea = msr1.readMultiPolygon();
		
		/*
		 * Read entry points.
		 */
		String entryPointsShapefile = String.format("%sShapefiles/%s/%sEntries_UTM35S.shp",
				root, studyAreaName, studyAreaName);
		MyShapefileReader msr2 = new MyShapefileReader(entryPointsShapefile);
		List<Point> entryPoints = msr2.readPoints();
		
		MyThroughTrafficAnalyser mtta = new MyThroughTrafficAnalyser(studyArea, entryPoints);
		
		/*
		 * Identify through-vehicles.
		 */
		String vehicleFolder = String.format("%sDigiCore/%d/XML/%s/%04d/Sample%02d/", 
				root, year, version, threshold, sample);
		File folder = new File(vehicleFolder);
		if(!folder.isDirectory()){
			throw new RuntimeException("The location " + vehicleFolder + " is not a folder.");
		}
		MyFileSampler mfs = new MyFileSampler(folder.getAbsolutePath());
		MyFileFilter mff = new MyFileFilter(".xml");
		List<File> files = mfs.sampleFiles(numberOfVehiclesToSample, mff);
		
		int counter = 0;
		int multiplier = 1;
		log.info("Processing vehicles from " + folder.getAbsolutePath());
		log.info("Total number of vehicles to process: " + files.size());
		for(File f : files){
			if(f.exists() && f.isFile()){
				String vehicleXML = f.getAbsolutePath();
				CommercialVehicle cv = null;
				MyXmlConverter xc = new MyXmlConverter(true);
				Object o = xc.readObjectFromFile(vehicleXML);
				if(o instanceof CommercialVehicle){
					cv = (CommercialVehicle) o;
				}
				if(cv.getFractionMinorInStudyArea() > 0 && cv.getFractionMinorInStudyArea() < withinThreshold){
					// It is a through-traffic vehicle.
					mtta.processVehicle(cv);
				}
			}
			/*
			 * Report progress.
			 */
			if(++counter == multiplier){
				log.info("   Vehicles processed: " + counter);
				multiplier *= 2;
			}			
		}
		log.info("   Vehicles processed: " + counter + " (Done)");	
		
		String outputFoldername = "./Output/";
		File locationFolder = new File(outputFoldername);
		boolean folderCreated = locationFolder.mkdirs();
		log.info("Output folder created (" + folderCreated + ") at " + locationFolder.getAbsolutePath());
		
		String location = String.format("%s%s_%03.0fp_%d_", 
				outputFoldername, studyAreaName, withinThreshold*100, files.size());
		mtta.writeListsToFile(location);
		
		log.info("----------------------------------------");
		log.info("           Process completed.");
		log.info("========================================");
		}
}
