package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;

public class AnalysisCellBasedAccessibilityCSVWriterV2 {
	
	private static final Logger log = Logger.getLogger(AnalysisCellBasedAccessibilityCSVWriterV2.class);
	private static BufferedWriter accessibilityDataWriter = null;
	
	/**
	 * writes the header of accessibility data csv file
	 */
	public AnalysisCellBasedAccessibilityCSVWriterV2(String fileExtension){
		try{
			log.info("Initializing AnalysisCellBasedAccessibilityCSVWriterV2 ...");
			accessibilityDataWriter = IOUtils.getBufferedWriter( Constants.MATSIM_4_OPUS_TEMP + fileExtension +"_accessibility_indicators_ersa.csv" );
			
			// create header
			accessibilityDataWriter.write( Constants.ZONE_ID + "," +
										   Constants.X_COORDINATE + "," +
										   Constants.Y_COORDINATE + "," + 
										   Constants.NEARESTNODE_ID + "," +
										   Constants.NEARESTNODE_X_COORD + "," +
										   Constants.NEARESTNODE_Y_COORD + "," + 
										   Constants.ACCESSIBILITY_BY_CAR + "," +
										   Constants.ACCESSIBILITY_BY_WALK);
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
	 * @param carAccessibility
	 * @param freespeedTravelTimesCarLogSum
	 * @param walkAccessibility
	 */
	public void write(Zone<CounterObject> startZone, 
							 Coord coordFromZone,
							 Node node, 
							 double carAccessibility, 
							 double walkAccessibility){
		
		try{
			assert(AnalysisCellBasedAccessibilityCSVWriterV2.accessibilityDataWriter != null);
			accessibilityDataWriter.write( startZone.getAttribute().getCounter() + "," +
										   coordFromZone.getX() + "," +
										   coordFromZone.getY() + "," +
										   node.getId() + "," + 
										   node.getCoord().getX() + "," +  
										   node.getCoord().getY() + "," + 
										   carAccessibility + "," + 
										   walkAccessibility );
			accessibilityDataWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * finalize and close csv file
	 */
	public void close(){
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
