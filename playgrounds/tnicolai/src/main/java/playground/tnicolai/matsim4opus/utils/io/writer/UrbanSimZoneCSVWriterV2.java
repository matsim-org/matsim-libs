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
public class UrbanSimZoneCSVWriterV2 {
	
	private static final Logger log 	= Logger.getLogger(UrbanSimZoneCSVWriterV2.class);
	private static BufferedWriter zoneWriter = null;
	public static final String FILE_NAME= "zones.csv";
	
	/**
	 * writes the header for zones csv file
	 */
	public static void initUrbanSimZoneWriter(String file){
		try{
			log.info("Initializing UrbanSimZoneCSVWriterV2 ...");
			zoneWriter = IOUtils.getBufferedWriter( file );
			
			// create header
			zoneWriter.write( Constants.ZONE_ID + "," +
							  Constants.ACCESSIBILITY_BY_CAR + "," +
							  Constants.ACCESSIBILITY_BY_WALK);
			zoneWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing the zone data (accessibilities) to csv file
	 * @param node
	 * @param carAccessibility
	 * @param walkAccessibility
	 */
	public static void write(Id zoneID,
							 double carAccessibility, 
							 double walkAccessibility){
		
		try{
			assert(UrbanSimZoneCSVWriterV2.zoneWriter != null);
			zoneWriter.write( zoneID + "," + 
										   carAccessibility + "," + 
										   walkAccessibility );
			zoneWriter.newLine();
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
			assert(UrbanSimZoneCSVWriterV2.zoneWriter != null);
			zoneWriter.flush();
			zoneWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
