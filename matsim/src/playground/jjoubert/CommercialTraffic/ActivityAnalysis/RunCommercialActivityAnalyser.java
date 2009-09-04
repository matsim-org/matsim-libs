/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseActivityDuration.java
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

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.DateString;
import playground.jjoubert.Utilities.FileSampler.MyFileFilter;
import playground.jjoubert.Utilities.FileSampler.MyFileSampler;

public class RunCommercialActivityAnalyser {
	private final static Logger log = Logger.getLogger(RunCommercialActivityAnalyser.class);

	private static String WGS84 = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Lon\", EAST],AXIS[\"Lat\", NORTH]]";
	private static String WGS84_UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";
	private static String signalFilename = "/Users/johanwjoubert/MATSim/workspace/MATSimData/DigiCore/Signals.txt";
	//====================================================
	// Parameters that must be set
	//----------------------------------------------------		
	private static int numberOfSamples = 1;
	private static int sampleSize = 20;
	private static float clusterRadius = 10;
	private static int clusterCount = 10;
	private static double majorThreshold = 500;
	//====================================================
	// Processes that must be run
	//----------------------------------------------------		
	private static boolean analyseForR = false;
	private static boolean extractChains = true;
	//====================================================
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String allVehicleFolder = "/Users/johanwjoubert/MATSim/workspace/MATSimData/DigiCore/SortedVehicles/";
		String xmlFoldername = "/Users/johanwjoubert/MATSim/workspace/MATSimData/DigiCore/XML";

		MyFileSampler sampler = new MyFileSampler(allVehicleFolder);
		MyFileFilter filter = new MyFileFilter(".txt");
		
		DateString ds = new DateString();
		
		for(int sample = 1; sample <= numberOfSamples; sample++){
			// Sample the files
			List<File> sampleFiles = sampler.sampleFiles(sampleSize, filter);
			
			// Analyse the sampled files
			CommercialActivityAnalyser caa = new CommercialActivityAnalyser(signalFilename, WGS84, WGS84_UTM35S);
			caa.extractChains(sampleFiles, sample, ds, majorThreshold, clusterRadius, clusterCount, xmlFoldername);
		}
				
	}

}
