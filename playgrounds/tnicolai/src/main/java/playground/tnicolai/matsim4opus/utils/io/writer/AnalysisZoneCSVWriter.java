package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;

public class AnalysisZoneCSVWriter {

	private static final Logger log = Logger.getLogger(AnalysisZoneCSVWriter.class);
	private static BufferedWriter zoneCSVWriter = null;
	
	/**
	 * writes the header of accessibility data csv file
	 */
	public static void initAccessiblityWriter(String file){
		try{
			log.info("Initializing AnalysisZoneCSVWriter ...");
			zoneCSVWriter = IOUtils.getBufferedWriter( file );
			
			// create header
			zoneCSVWriter.write( InternalConstants.ZONE_ID + "," +
								 InternalConstants.ZONE_CENTROID_X_COORD + "," +
								 InternalConstants.ZONE_CENTROID_Y_COORD + "," +
								 InternalConstants.NEARESTNODE_X_COORD + "," +
								 InternalConstants.NEARESTNODE_Y_COORD + "," +
								 InternalConstants.CONGESTED_TRAVEL_TIME_ACCESSIBILITY + "," +
								 InternalConstants.FREESPEED_TRAVEL_TIME_ACCESSIBILITY + "," + 
								 InternalConstants.WALK_TRAVEL_TIME_ACCESSIBILITY);
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
			assert(AnalysisZoneCSVWriter.zoneCSVWriter != null);
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
			log.info("Closing AnalysisZoneCSVWriter ...");
			assert(AnalysisZoneCSVWriter.zoneCSVWriter != null);
			zoneCSVWriter.flush();
			zoneCSVWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
