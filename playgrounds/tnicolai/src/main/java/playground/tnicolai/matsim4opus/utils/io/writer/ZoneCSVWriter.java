package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;

public class ZoneCSVWriter {

	private static final Logger log = Logger.getLogger(ZoneCSVWriter.class);
	private static BufferedWriter zoneCSVWriter = null;
	
	/**
	 * writes the header of accessibility data csv file
	 */
	public static void initAccessiblityWriter(String file){
		try{
			log.info("Initializing ZoneWriter ...");
			zoneCSVWriter = IOUtils.getBufferedWriter( file );
			
			// create header
			zoneCSVWriter.write( Constants.ERSA_ZONE_ID + "," +
										   Constants.ERSA_ZONE_X_COORD + "," +
										   Constants.ERSA_ZONE_Y_COORD + "," +
										   Constants.ERSA_NEARESTNODE_X_COORD + "," +
										   Constants.ERSA_NEARESTNODE_Y_COORD + "," +
										   Constants.ERSA_CONGESTED_TRAVEL_TIME_ACCESSIBILITY + "," +
										   Constants.ERSA_FREESPEED_TRAVEL_TIME_ACCESSIBILITY + "," + 
										   Constants.ERSA_WALK_TRAVEL_TIME_ACCESSIBILITY);
			zoneCSVWriter.newLine();
			
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
							 Coord zoneCentroid,
							 Coord nearestNode,
							 double congestedTravelTimesCarLogSum, 
							 double freespeedTravelTimesCarLogSum, 
							 double travelTimesWalkLogSum){
		
		try{
			assert(ZoneCSVWriter.zoneCSVWriter != null);
			zoneCSVWriter.write( zoneID + "," + 
								 zoneCentroid.getX() + "," + 
								 zoneCentroid.getY() + "," + 
								 nearestNode.getX() + "," + 
								 nearestNode.getY() + "," + 
								 congestedTravelTimesCarLogSum + "," + 
								 freespeedTravelTimesCarLogSum + "," + 
								 travelTimesWalkLogSum );
			zoneCSVWriter.newLine();
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
			log.info("Closing ZoneCSVWriter ...");
			assert(ZoneCSVWriter.zoneCSVWriter != null);
			zoneCSVWriter.flush();
			zoneCSVWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
