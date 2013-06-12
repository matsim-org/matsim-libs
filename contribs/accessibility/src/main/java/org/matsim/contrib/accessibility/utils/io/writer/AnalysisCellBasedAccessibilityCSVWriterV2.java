package org.matsim.contrib.accessibility.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.io.IOUtils;


public class AnalysisCellBasedAccessibilityCSVWriterV2 {
	
	private static final Logger log = Logger.getLogger(AnalysisCellBasedAccessibilityCSVWriterV2.class);
	private static BufferedWriter accessibilityDataWriter = null;
	public static final String FILE_NAME= "accessibility_indicators.csv";
	
	/**
	 * writes the header of accessibility data csv file
	 */
	public static void initAnalysisCellBasedAccessibilityCSVWriterV2(String matsimOutputDirectory){
		try{
			log.info("Initializing AnalysisCellBasedAccessibilityCSVWriterV2 ...");
			accessibilityDataWriter = IOUtils.getBufferedWriter( matsimOutputDirectory + FILE_NAME );
			
			// create header
			accessibilityDataWriter.write( Labels.ZONE_ID + "," +
										   Labels.X_COORDINATE + "," +
										   Labels.Y_COORDINATE + "," + 
										   Labels.NEARESTNODE_ID + "," +
										   Labels.NEARESTNODE_X_COORD + "," +
										   Labels.NEARESTNODE_Y_COORD + "," + 
										   Labels.ACCESSIBILITY_BY_FREESPEED + "," +
										   Labels.ACCESSIBILITY_BY_CAR + "," +
										   Labels.ACCESSIBILITY_BY_BIKE + "," +
										   Labels.ACCESSIBILITY_BY_WALK + "," +
										   Labels.ACCESSIBILITY_BY_PT);
			accessibilityDataWriter.newLine();
			
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
	 * @param node
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	public static void write(ActivityFacility startZone, 
							 Node node, 
							 double freeSpeedAccessibility,
							 double carAccessibility, 
							 double bikeAccessibility,
							 double walkAccessibility,
							 double ptAccessibility){
		
		try{
			assert(AnalysisCellBasedAccessibilityCSVWriterV2.accessibilityDataWriter != null);
			accessibilityDataWriter.write( startZone.getId().toString() + "," +
										   startZone.getCoord().getX() + "," +
										   startZone.getCoord().getY() + "," +
										   node.getId() + "," + 
										   node.getCoord().getX() + "," +  
										   node.getCoord().getY() + "," + 
										   freeSpeedAccessibility + "," +
										   carAccessibility + "," +
										   bikeAccessibility + "," + 
										   walkAccessibility + "," +
										   ptAccessibility);
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
			log.info("Closing AnalysisCellBasedAccessibilityCSVWriterV2 ...");
			assert(AnalysisCellBasedAccessibilityCSVWriterV2.accessibilityDataWriter != null);
			accessibilityDataWriter.flush();
			accessibilityDataWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
