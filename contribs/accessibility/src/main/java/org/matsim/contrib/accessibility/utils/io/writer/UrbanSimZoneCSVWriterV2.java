package org.matsim.contrib.accessibility.utils.io.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.io.IOUtils;


/**
 * This writer produces an UrbanSim input data for the zones dataset 
 * including accessibility measures for each zone
 * 
 * @author thomas
 *
 */
public class UrbanSimZoneCSVWriterV2 {
	
	private static final Logger log 	= Logger.getLogger(UrbanSimZoneCSVWriterV2.class);
	private static BufferedWriter zoneWriter = null;
	public static final String FILE_NAME= "zones.csv";
	private static String matsim4opusTempDirectory = null;
	
	/**
	 * writes the header for zones csv file
	 */
	public static void initUrbanSimZoneWriter(String matsim4opusTempDirectory){
		
		UrbanSimZoneCSVWriterV2.matsim4opusTempDirectory = matsim4opusTempDirectory;
		
		try{
			log.info("Initializing UrbanSimZoneCSVWriterV2 ...");
			zoneWriter = IOUtils.getBufferedWriter( matsim4opusTempDirectory + FILE_NAME );
			log.info("Writing data into " + matsim4opusTempDirectory + FILE_NAME + " ...");
			
			// create header
			zoneWriter.write(Labels.ZONE_ID + "," +
							 Labels.ACCESSIBILITY_BY_FREESPEED + "," +
							 Labels.ACCESSIBILITY_BY_CAR + "," +
							 Labels.ACCESSIBILITY_BY_BIKE + "," +
							 Labels.ACCESSIBILITY_BY_WALK + "," +
							 Labels.ACCESSIBILITY_BY_PT);
			zoneWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing the accessibility measures into csv file
	 * 
	 * @param startZone
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	public static synchronized void write( ActivityFacility startZone,
										 double freeSpeedAccessibility,
										 double carAccessibility, 
										 double bikeAccessibility,
										 double walkAccessibility,
										 double ptAccessibility){
		
		try{
			assert(UrbanSimZoneCSVWriterV2.zoneWriter != null);
			zoneWriter.write( startZone.getId().toString() + "," + 
							  freeSpeedAccessibility + "," + 
							  carAccessibility + "," + 
							  bikeAccessibility + "," +
							  walkAccessibility + "," +
							  ptAccessibility);
			zoneWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * finalize and close csv file
	 */
	public static void close(String matsimOutputDirectory){
		try {
			log.info("Closing UrbanSimZoneCSVWriterV2 ...");
			assert(UrbanSimZoneCSVWriterV2.zoneWriter != null);
			zoneWriter.flush();
			zoneWriter.close();
			
			// copy the zones file to the outputfolder...
			log.info("Copying " + UrbanSimZoneCSVWriterV2.matsim4opusTempDirectory + FILE_NAME + " to " + matsimOutputDirectory + FILE_NAME);
			IOUtils.copyFile(new File( UrbanSimZoneCSVWriterV2.matsim4opusTempDirectory + FILE_NAME),	new File( matsimOutputDirectory + FILE_NAME));
			
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
