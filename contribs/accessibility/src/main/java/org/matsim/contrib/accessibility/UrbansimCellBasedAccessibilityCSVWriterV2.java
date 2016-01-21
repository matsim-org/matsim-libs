package org.matsim.contrib.accessibility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;

// urbansim accessibility writer; better do not touch except when working on matsim-urbansim integration. kai, feb'14
// yy move to matsim4urbansim
final class UrbansimCellBasedAccessibilityCSVWriterV2 implements FacilityDataExchangeInterface {
	private static final Logger log = Logger.getLogger(UrbansimCellBasedAccessibilityCSVWriterV2.class);

	private static final String ACCESSIBILITY_INDICATORS= "accessibility_indicators.csv";

	private BufferedWriter accessibilityDataWriter ;

	/**
	 * writes the header of accessibility data csv file
	 */
	public UrbansimCellBasedAccessibilityCSVWriterV2(String matsimOutputDirectory){
		log.info("Initializing  ...");
		try {
		accessibilityDataWriter = IOUtils.getBufferedWriter( matsimOutputDirectory + "/" + ACCESSIBILITY_INDICATORS );
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
//					Labels.NEARESTNODE_ID + "," +
//					Labels.NEARESTNODE_X_COORD + "," +
//					Labels.NEARESTNODE_Y_COORD + "," + 
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
	public UrbansimCellBasedAccessibilityCSVWriterV2(String matsimOutputDirectory, String modeName){
		try{
			log.info("Initializing ...");
			accessibilityDataWriter = IOUtils.getBufferedWriter( matsimOutputDirectory + "/" + ACCESSIBILITY_INDICATORS + "_" + modeName + ".csv" );
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
	 * Design thoughts:<ul>
	 * <li> yyyy I am not sure why it is meaningful to use zones or nodes for the coordinates.  Accessibility refers directly
	 * to coordinates, and maybe directly to zones (if averaged). --> remove eventually.  kai, jul'13
	 * <ul>
	 * 
	 */
	@Override
	public void setFacilityAccessibilities(ActivityFacility startZone, Double timeOfDay, Map<Modes4Accessibility,Double> accessibilities ) {
		// (this is what, I think, writes the urbansim data, and should thus better not be touched. kai, feb'14)
		
		log.info( "here2");

		try{
			assert(accessibilityDataWriter != null);
			accessibilityDataWriter.write( startZone.getId().toString() + "," +
					startZone.getCoord().getX() + "," +
					startZone.getCoord().getY() // + "," +
//					node.getId() + "," + 
//					node.getCoord().getX() + "," +  
//					node.getCoord().getY() 
) ;
			for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
				accessibilityDataWriter.write( "," + accessibilities.get( mode ) );
			}
			accessibilityDataWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(-1) ;
		}
	}

	/**
	 * finalize and close csv file
	 */
	@Override
	public void finish() {
		try {
			log.info("Closing ...");
			assert(accessibilityDataWriter != null);
			accessibilityDataWriter.flush();
			accessibilityDataWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
