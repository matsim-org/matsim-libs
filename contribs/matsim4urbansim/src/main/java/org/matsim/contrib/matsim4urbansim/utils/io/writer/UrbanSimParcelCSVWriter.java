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
package org.matsim.contrib.matsim4urbansim.utils.io.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;


/**
 * @author thomas
 *
 */
public class UrbanSimParcelCSVWriter {

	private static final Logger log 	= Logger.getLogger(UrbanSimParcelCSVWriter.class);
	private static BufferedWriter parcelWriter = null;
	public static final String FILE_NAME= "parcels.csv";
	public static final String ACCESSIBILITY_BY_FREESPEED = "freespeed_accessibility";
	public static final String ACCESSIBILITY_BY_CAR = "car_accessibility";
	public static final String ACCESSIBILITY_BY_BIKE = "bike_accessibility";
	public static final String ACCESSIBILITY_BY_WALK = "walk_accessibility";
	public static final String ACCESSIBILITY_BY_PT = "pt_accessibility";
	
	/**
	 * writes the header for zones csv file
	 * @param config TODO
	 */
	public static void initUrbanSimZoneWriter(Config config){
		UrbanSimParameterConfigModuleV3 module = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		try{
			log.info("Initializing UrbanSimParcelCSVWriter ...");
			parcelWriter = IOUtils.getBufferedWriter( module.getMATSim4OpusTemp() + FILE_NAME );
			log.info("Writing data into " + module.getMATSim4OpusTemp() + FILE_NAME + " ...");
			
			// create header
			parcelWriter.write( InternalConstants.PARCEL_ID + "," +
								ACCESSIBILITY_BY_FREESPEED + "," +
								ACCESSIBILITY_BY_CAR + "," +
								ACCESSIBILITY_BY_BIKE + "," +
								ACCESSIBILITY_BY_WALK + "," +
								ACCESSIBILITY_BY_PT);
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
	public static void write(Id parcelID, Map<Modes4Accessibility,Double> accessibilities ) {
		try{
			assert(UrbanSimParcelCSVWriter.parcelWriter != null);
			parcelWriter.write( parcelID.toString() ) ;
			for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
				parcelWriter.write( "," + accessibilities.get( mode ) ) ;
			}
			parcelWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("could not write") ;
		}
	}
	
	/**
	 * finalize and close csv file
	 * @param config TODO
	 */
	public static void close(Config config){
		UrbanSimParameterConfigModuleV3 module = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		try {
			log.info("Closing UrbanSimZoneCSVWriterV2 ...");
			assert(UrbanSimParcelCSVWriter.parcelWriter != null);
			parcelWriter.flush();
			parcelWriter.close();
			
			// copy the zones file to the outputfolder...
			log.info("Copying " + module.getMATSim4OpusTemp() + FILE_NAME + " to " + module.getMATSim4OpusOutput() + FILE_NAME);
            try {
                Files.copy(new File( module.getMATSim4OpusTemp() + FILE_NAME).toPath(), new File( module.getMATSim4OpusOutput()+ FILE_NAME).toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
