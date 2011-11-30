package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;

public class AccessibilityCSVWriter {
	
	private static final Logger log = Logger.getLogger(AccessibilityCSVWriter.class);
	private static BufferedWriter accessibilityIndicatorWriter = null;
	
	public static void initAccessiblityWriter(String file){
		try{
			log.info("Initializing AccessibilityCSVWriter ...");
			accessibilityIndicatorWriter = IOUtils.getBufferedWriter( file );
			
			// create header
			accessibilityIndicatorWriter.write( Constants.ERSA_NEARESTNODE_ID + "," +
												Constants.ERSA_NEARESTNODE_X_COORD + "," +
												Constants.ERSA_NEARESTNODE_Y_COORD + "," + 
												Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "," +
												Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "," + 
												Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY);
			accessibilityIndicatorWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void write(Node node, 
							 double travelTimeAccessibility, 
							 double travelCostAccessibility, 
							 double travelDistanceAccessibility){
		
		try{
			assert(AccessibilityCSVWriter.accessibilityIndicatorWriter != null);
			accessibilityIndicatorWriter.write( node.getId() + "," + 
												node.getCoord().getX() + "," +  
												node.getCoord().getY() + "," + 
												travelTimeAccessibility + "," + 
												travelCostAccessibility + "," + 
												travelDistanceAccessibility );
			accessibilityIndicatorWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void close(){
		try {
			log.info("Closing AccessibilityCSVWriter ...");
			assert(AccessibilityCSVWriter.accessibilityIndicatorWriter != null);
			accessibilityIndicatorWriter.flush();
			accessibilityIndicatorWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
