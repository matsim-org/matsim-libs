package org.matsim.contrib.matsim4urbansim.run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;


/**
 * This writer produces an UrbanSim input data for the zones dataset 
 * including accessibility measures for each zone
 * 
 * @author thomas
 *
 */
 final class UrbanSimZoneCSVWriterV2 implements FacilityDataExchangeInterface {
	
	private static final Logger log 	= Logger.getLogger(UrbanSimZoneCSVWriterV2.class);
	private BufferedWriter zoneWriter = null;
	public static final String FILE_NAME= "zones.csv";
	private  String matsim4opusTempDirectory ;
	private String matsimOutputDirectory;
	
	boolean first = true ;

	/**
	 * writes the header for zones csv file
	 */
	public UrbanSimZoneCSVWriterV2(String matsim4opusTempDirectory, String matsimOutputDirectory){
		// yyyy move to matsim4urbansim project
		
		this.matsim4opusTempDirectory = matsim4opusTempDirectory;
		this.matsimOutputDirectory = matsimOutputDirectory;

		try{
			log.info("Initializing UrbanSimZoneCSVWriterV2 ...");
			zoneWriter = IOUtils.getBufferedWriter( matsim4opusTempDirectory + FILE_NAME );
			log.info("Writing data into " + matsim4opusTempDirectory + FILE_NAME + " ...");
						
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing the accessibility measures into csv file
	 * @param startZone
	 */
	@Override
	public void setFacilityAccessibilities( ActivityFacility startZone, Double timeOfDay, Map<String,Double> accessibilities ) {
		// (this is what, I think, writes the urbansim data, and should thus better not be touched. kai, feb'14)
		
		try{
			assert(zoneWriter != null);
			if ( first ) {
				first = false ;
				zoneWriter.write( Labels.ZONE_ID );
				for ( String mode : accessibilities.keySet() ) {
					zoneWriter.write("," + mode + Labels._ACCESSIBILITY );
				}
				zoneWriter.newLine(); 
			}
			// ---
			zoneWriter.write( startZone.getId().toString() ) ;
			for ( Modes4Accessibility mode : Modes4Accessibility.values() ) { // seems a bit safer with respect to sequence. kai, feb'14
				zoneWriter.write( "," + accessibilities.get( mode) ) ;
			}
			zoneWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("io did not work") ;
		}
	}

	/**
	 * finalize and close csv file
	 */
	@Override
	public void finish() {
		try {
			log.info("Closing UrbanSimZoneCSVWriterV2 ...");
			assert(zoneWriter != null);
			zoneWriter.flush();
			zoneWriter.close();

			// copy the zones file to the outputfolder...
			log.info("Copying " + matsim4opusTempDirectory + FILE_NAME + " to " + matsimOutputDirectory + FILE_NAME);
            try {
                Files.copy(new File( matsim4opusTempDirectory + FILE_NAME).toPath(), new File( matsimOutputDirectory + FILE_NAME).toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("io did not work") ;
		}
	}

}
