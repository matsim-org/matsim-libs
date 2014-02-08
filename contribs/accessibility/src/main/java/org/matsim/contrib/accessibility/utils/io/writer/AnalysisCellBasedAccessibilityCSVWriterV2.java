package org.matsim.contrib.accessibility.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.io.IOUtils;


public class AnalysisCellBasedAccessibilityCSVWriterV2 {
	private static final Logger log = Logger.getLogger(AnalysisCellBasedAccessibilityCSVWriterV2.class);

	public static final String FILE_NAME= "accessibility_indicators.csv";

	private BufferedWriter accessibilityDataWriter ;

	/**
	 * writes the header of accessibility data csv file
	 */
	public AnalysisCellBasedAccessibilityCSVWriterV2(String matsimOutputDirectory){
		log.info("Initializing AnalysisCellBasedAccessibilityCSVWriterV2 ...");
		try {
		accessibilityDataWriter = IOUtils.getBufferedWriter( matsimOutputDirectory + "/" + FILE_NAME );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated") ;
		}

		if ( accessibilityDataWriter==null ) {
			throw new RuntimeException( "writer is null") ;
		}
		
		// create header
		try {
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
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("io did not work") ;
		}

		log.info("... done!");
	}
	/**
	 * writes the header of accessibility data csv file
	 */
	public AnalysisCellBasedAccessibilityCSVWriterV2(String matsimOutputDirectory, String modeName){
		try{
			log.info("Initializing AnalysisCellBasedAccessibilityCSVWriterV2 ...");
			accessibilityDataWriter = IOUtils.getBufferedWriter( matsimOutputDirectory + "/" + "accessibility_indicators" + "_" + modeName + ".csv" );
			// yyyyyy in some calling sequences, this is called too early, and the directory is not yet there. kai, feb'14

			// create header
			accessibilityDataWriter.write( "x" + "\t" + "y" + "\t" + "accessibility" );
			accessibilityDataWriter.newLine();

			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("io not possible") ;
		}
	}

	/**
	 * writing the accessibility measures into csv file.
	 * <p/>
	 * Design thoughs:<ul>
	 * <li> yyyy I am not sure why it is meaningful to use zones or nodes for the coordinates.  Accessibility refers directly
	 * to coordinates, and maybe directly to zones (if averaged). --> remove eventually.  kai, jul'13
	 * <ul>
	 * 
	 * @param startZone
	 * @param node
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	public void writeRecord(ActivityFacility startZone, 
			Node node, 
			double freeSpeedAccessibility,
			double carAccessibility, 
			double bikeAccessibility,
			double walkAccessibility,
			double ptAccessibility){

		try{
			assert(accessibilityDataWriter != null);
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
			System.exit(-1) ;
		}
	}

	public void writeRecord( Coord coord, double accessibility ) {
		try {
			accessibilityDataWriter.write( coord.getX() + "\t" +  coord.getY() + "\t" +  accessibility ) ;
			accessibilityDataWriter.newLine() ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("io error") ;
		}
	}

	public void writeNewLine() {
		try {
			accessibilityDataWriter.newLine() ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("i/o failure") ;
		}
	}

	/**
	 * finalize and close csv file
	 */
	public void close(){
		try {
			log.info("Closing AnalysisCellBasedAccessibilityCSVWriterV2 ...");
			assert(accessibilityDataWriter != null);
			accessibilityDataWriter.flush();
			accessibilityDataWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
