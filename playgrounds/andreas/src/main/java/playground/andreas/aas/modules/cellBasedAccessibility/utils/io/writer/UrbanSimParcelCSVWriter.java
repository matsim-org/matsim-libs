/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package playground.andreas.aas.modules.cellBasedAccessibility.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.andreas.aas.modules.cellBasedAccessibility.constants.InternalConstants;

/**
 * @author thomas
 *
 */
public class UrbanSimParcelCSVWriter {

	private static final Logger log 	= Logger.getLogger(UrbanSimParcelCSVWriter.class);
	private static BufferedWriter parcelWriter = null;
	public static final String FILE_NAME= "parcels.csv";
	
	/**
	 * writes the header for zones csv file
	 */
	public static void initUrbanSimZoneWriter(){
		try{
			log.info("Initializing UrbanSimParcelCSVWriter ...");
			parcelWriter = IOUtils.getBufferedWriter( InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME );
			log.info("Writing data into " + InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME + " ...");
			
			// create header
			parcelWriter.write( InternalConstants.PARCEL_ID + "," +
								InternalConstants.ACCESSIBILITY_BY_FREESPEED + "," +
								InternalConstants.ACCESSIBILITY_BY_CAR + "," +
								InternalConstants.ACCESSIBILITY_BY_BIKE + "," +
								InternalConstants.ACCESSIBILITY_BY_WALK);
			parcelWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing the parcel data (accessibilities) to csv file
	 * @param node
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param walkAccessibility
	 */
	public static void write(Id parcelID,
							 double freeSpeedAccessibility,
							 double carAccessibility, 
							 double bikeAccessibility,
							 double walkAccessibility){
		
		try{
			assert(UrbanSimParcelCSVWriter.parcelWriter != null);
			parcelWriter.write( parcelID + "," + 
								freeSpeedAccessibility + "," +
								carAccessibility + "," +
								bikeAccessibility + "," +
								walkAccessibility);
			parcelWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * finalize and close csv file
	 */
	public static void close(){
		try {
			log.info("Closing UrbanSimZoneCSVWriterV2 ...");
			assert(UrbanSimParcelCSVWriter.parcelWriter != null);
			parcelWriter.flush();
			parcelWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
