package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;

/**
 * This writer produces an UrbanSim input data for the zones dataset 
 * including accessibility measures for each zone
 * 
 * @author thomas
 *
 */
public class UrbanSimZoneCSVWriter {
	
	private static final Logger log 	= Logger.getLogger(UrbanSimZoneCSVWriter.class);
	private static BufferedWriter accessibilityDataWriter = null;
	public static final String FILE_NAME= "zones.csv";
	
	/**
	 * writes the header for zones csv file
	 */
	public static void initUrbanSimZoneWriter(String file){
		try{
			log.info("Initializing UrbanSimZoneCSVWriter ...");
			accessibilityDataWriter = IOUtils.getBufferedWriter( file );
			
			// create header
			accessibilityDataWriter.write( Constants.ERSA_ZONE_ID + "," +
										   Constants.ERSA_CONGESTED_TRAVEL_TIME_ACCESSIBILITY + "," +
										   Constants.ERSA_FREESPEED_TRAVEL_TIME_ACCESSIBILITY + "," + 
										   Constants.ERSA_WALK_TRAVEL_TIME_ACCESSIBILITY);
			accessibilityDataWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing the zone data (accessibilities) to csv file
	 * @param node
	 * @param congestedTravelTimesCarLogSum
	 * @param freespeedTravelTimesCarLogSum
	 * @param travelTimesWalkLogSum
	 */
	public static void write(Id zoneID,
							 double congestedTravelTimesCarLogSum, 
							 double freespeedTravelTimesCarLogSum, 
							 double travelTimesWalkLogSum){
		
		try{
			assert(UrbanSimZoneCSVWriter.accessibilityDataWriter != null);
			accessibilityDataWriter.write( zoneID + "," + 
										   congestedTravelTimesCarLogSum + "," + 
										   freespeedTravelTimesCarLogSum + "," + 
										   travelTimesWalkLogSum );
			accessibilityDataWriter.newLine();
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
			log.info("Closing UrbanSimZoneCSVWriter ...");
			assert(UrbanSimZoneCSVWriter.accessibilityDataWriter != null);
			accessibilityDataWriter.flush();
			accessibilityDataWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
