package org.matsim.contrib.matsim4urbansim.run;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;

// urbansim accessibility writer; better do not touch except when working on matsim-urbansim integration. kai, feb'14
// yy move to matsim4urbansim
final class UrbansimCellBasedAccessibilityCSVWriterV2 implements FacilityDataExchangeInterface {
	private static final Logger log = Logger.getLogger(UrbansimCellBasedAccessibilityCSVWriterV2.class);

	public static final String ACCESSIBILITY_INDICATORS= "accessibility_indicators.csv";

	private BufferedWriter accessibilityDataWriter ;
	
	private boolean ini = true ;
	private List<String> modes = new ArrayList<>() ; 

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
		log.info("... done!");
	}

	/**
	 * writing the accessibility measures into csv file.
	 * <p></p>
	 * Design thoughts:<ul>
	 * <li> yyyy I am not sure why it is meaningful to use zones or nodes for the coordinates.  Accessibility refers directly
	 * to coordinates, and maybe directly to zones (if averaged). --> remove eventually.  kai, jul'13
	 * <ul>
	 * 
	 */
	@Override
	public void setFacilityAccessibilities(ActivityFacility startZone, Double timeOfDay, Map<String,Double> accessibilities ) {
		// (this is what, I think, writes the urbansim data, and should thus better not be touched. kai, feb'14)

		// memorize the modes:
		
		if ( ini ) {
			ini = false ;
			modes.add( Modes4Accessibility.freespeed.name() ) ; // needs to be first to pass existing test. kai,, nov'16
			for ( String mode : accessibilities.keySet() ) {
				if ( !mode.equals( Modes4Accessibility.freespeed.name() ) ) {
					modes.add( mode ) ; // copy this into a data structure where we know that the sequence is stable
				}
			}
			// create header
			try {
				accessibilityDataWriter.write( Labels.ZONE_ID + "," +
						Labels.X_COORDINATE + "," +
						Labels.Y_COORDINATE ) ; 
//						Labels.NEARESTNODE_ID + "," +
//						Labels.NEARESTNODE_X_COORD + "," +
//						Labels.NEARESTNODE_Y_COORD + "," +
				for ( String mode : modes ) {
					accessibilityDataWriter.write( "," + mode + "_accessibility");
				}
				accessibilityDataWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("io did not work") ;
			}
		}
		
		log.warn(""); 
		log.warn("receiving accessibilities; start zone=" + startZone.getId() );
		for ( String mode : modes ) {
			log.warn( "mode=" + mode + "; accessibility=" + accessibilities.get(mode) ) ;
		}

		try{
			assert(accessibilityDataWriter != null);
			accessibilityDataWriter.write( startZone.getId().toString() + "," +
					startZone.getCoord().getX() + "," +
					startZone.getCoord().getY() // + "," +
//					node.getId() + "," + 
//					node.getCoord().getX() + "," +  
//					node.getCoord().getY() 
) ;
			for ( String mode : modes ) {
				final Double accessibilityByMode = accessibilities.get( mode );
				Gbl.assertNotNull(accessibilityByMode);
				accessibilityDataWriter.write( "," + accessibilityByMode );
			}
			accessibilityDataWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException() ;
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
