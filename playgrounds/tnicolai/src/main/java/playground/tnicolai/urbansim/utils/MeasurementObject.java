/* *********************************************************************** *
 * project: org.matsim.*
 * MeasurementObject.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.tnicolai.urbansim.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.constants.Constants;

/**
 * @author thomas
 *
 */
public class MeasurementObject {
	
	// self reference
	private static MeasurementObject instance = null;
	// logger
	private static final Logger log = Logger.getLogger(MeasurementObject.class);
	
	// measurement values
	private static long DURATION_READ_FACILITIES_URBANSIM_OUTPUT;	// in milliseconds
	private static long DURATION_READ_PERSONS_URBANSIM_OUTPUT;		// in milliseconds
	
	private static long DURATION_WRITE_FACILITIES_MATSIM_OUTPUT;	// in milliseconds
	private static long SIZE_WRITE_FACILITIES_MATSIM_OUTPUT;		// in byte
	
	private static long DURATION_WRITE_PERSONS_MATSIM_OUTPUT;		// in milliseconds
	private static long SIZE_WRITE_PERSONS_MATSIM_OUTPUT;			// in byte
	
	private static long SIZE_TARVEL_DATA_MATSIM_OUTPUT;				// in byte
	private static long DURATION_WRITE_TRAVEL_DATA_MATSIM_OUTPUT;	// in milliseconds
	
	/**
	 * prevent usage of default constructor from external classes
	 */
	private MeasurementObject(){
		DURATION_READ_FACILITIES_URBANSIM_OUTPUT 	= -1;
		DURATION_READ_PERSONS_URBANSIM_OUTPUT		= -1;
		
		SIZE_WRITE_FACILITIES_MATSIM_OUTPUT			= -1;
		DURATION_WRITE_FACILITIES_MATSIM_OUTPUT 	= -1;
		
		SIZE_WRITE_PERSONS_MATSIM_OUTPUT			= -1;
		DURATION_WRITE_PERSONS_MATSIM_OUTPUT		= -1;
		
		SIZE_TARVEL_DATA_MATSIM_OUTPUT				= -1;
		DURATION_WRITE_TRAVEL_DATA_MATSIM_OUTPUT	= -1;
	}
	
	/**
	 * return singelton instance of MeasurementObject
	 * @return MeasurementObject
	 */
	public static MeasurementObject getInstance(){
		if(instance == null){
			instance = new MeasurementObject();
			log.info("Creating singelton instance of MeasurementObject");
		}
		return instance;
	}
	
	/**
	 * setter
	 * @param duration
	 */
	public static void setDurationReadFacilities(long duration){
		DURATION_READ_FACILITIES_URBANSIM_OUTPUT = duration;
	}
	
	/**
	 * setter
	 * @param duration
	 */
	public static void setDurationReadPersons(long duration){
		DURATION_READ_PERSONS_URBANSIM_OUTPUT = duration;
	}
	
	/**
	 * setter
	 * @param size
	 */
	public static void setFacilitiesOutputSize(long size){
		SIZE_WRITE_FACILITIES_MATSIM_OUTPUT = size;
	}
	
	/**
	 * setter
	 * @param duration
	 */
	public static void setDurationWriteFacilities(long duration){
		DURATION_WRITE_FACILITIES_MATSIM_OUTPUT = duration;
	}
	
	/**
	 * setter
	 * @param size
	 */
	public static void setPersonsOutputSize(long size){
		SIZE_WRITE_PERSONS_MATSIM_OUTPUT = size;
	}
	
	/**
	 * setter
	 * @param duration
	 */
	public static void setDurationWritePersons(long duration){
		DURATION_WRITE_PERSONS_MATSIM_OUTPUT = duration;
	}	
	
	/**
	 * setter
	 * @param size
	 */
	public static void setTravelDataSize(long size){
		SIZE_TARVEL_DATA_MATSIM_OUTPUT = size;
	}
	 /**
	  * setter
	  * @param duration
	  */
	public static void setDurationWriteTravelData(long duration){
		DURATION_WRITE_TRAVEL_DATA_MATSIM_OUTPUT = duration;
	}
	
	
	/**
	 * 
	 */
	public static void wirteLogfile(){
		
		File logFile;
	    FileWriter fstream;
	    BufferedWriter bstream;
	    
		try {
			logFile = new File( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + Constants.MEASUREMENT_LOGFILE );
			if(!logFile.exists()){
				logFile.createNewFile();
				log.info("Logfile " + logFile.getCanonicalPath() + " not found. Logfile created.");
			}
			
			fstream = new FileWriter(logFile,true);
			bstream = new BufferedWriter(fstream);
		    
			bstream.write("Duration reading parcel table and creating facilities and zones in seconds:" + DURATION_READ_FACILITIES_URBANSIM_OUTPUT/1000);
			bstream.write("\n");
			bstream.write("Duration reading person table and creating a population in seconds:" + DURATION_READ_PERSONS_URBANSIM_OUTPUT/1000);
			bstream.write("\n");
			bstream.write("Duration writing travel_data.csv ins seconds:" + DURATION_WRITE_TRAVEL_DATA_MATSIM_OUTPUT/1000);
			bstream.write("\n\n");
			
			bstream.flush();
			bstream.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

