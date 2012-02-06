package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;

public class ZoneBasedAccessibilityCSVWriter {
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityCSVWriter.class);
	private static BufferedWriter accessibilityDataWriter = null;
	
	/**
	 * writes the header of accessibility data csv file
	 */
	public static void initAccessiblityWriter(String file){
		try{
			log.info("Initializing ZoneBasedAccessibilityCSVWriter ...");
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
	 * writing the accessibility measures into csv file
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
			assert(ZoneBasedAccessibilityCSVWriter.accessibilityDataWriter != null);
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
			log.info("Closing ZoneBasedAccessibilityCSVWriter ...");
			assert(ZoneBasedAccessibilityCSVWriter.accessibilityDataWriter != null);
			accessibilityDataWriter.flush();
			accessibilityDataWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
