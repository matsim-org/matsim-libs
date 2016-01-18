package org.matsim.contrib.accessibility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;


/**
 * This writer produces an UrbanSim input data for the zones dataset 
 * including accessibility measures for each zone
 * 
 * @author thomas
 *
 */
public final class UrbanSimZoneCSVWriterV2 implements FacilityDataExchangeInterface {
	
	private static final Logger log 	= Logger.getLogger(UrbanSimZoneCSVWriterV2.class);
	private BufferedWriter zoneWriter = null;
	public static final String FILE_NAME= "zones.csv";
	private  String matsim4opusTempDirectory ;
	private String matsimOutputDirectory;

	/**
	 * writes the header for zones csv file
	 */
	public UrbanSimZoneCSVWriterV2(String matsim4opusTempDirectory, String matsimOutputDirectory){
		
		this.matsim4opusTempDirectory = matsim4opusTempDirectory;
		this.matsimOutputDirectory = matsimOutputDirectory;

		try{
			log.info("Initializing UrbanSimZoneCSVWriterV2 ...");
			zoneWriter = IOUtils.getBufferedWriter( matsim4opusTempDirectory + FILE_NAME );
			log.info("Writing data into " + matsim4opusTempDirectory + FILE_NAME + " ...");
			
			// create header
			zoneWriter.write(Labels.ZONE_ID + "," +
							 Labels.ACCESSIBILITY_BY_FREESPEED + "," +
							 Labels.ACCESSIBILITY_BY_CAR + "," +
							 Labels.ACCESSIBILITY_BY_BIKE + "," +
							 Labels.ACCESSIBILITY_BY_WALK + "," +
							 Labels.ACCESSIBILITY_BY_PT);
			zoneWriter.newLine();
			
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
	public void setFacilityAccessibilities( ActivityFacility startZone, Double timeOfDay, Map<Modes4Accessibility,Double> accessibilities ) {
		// (this is what, I think, writes the urbansim data, and should thus better not be touched. kai, feb'14)

		try{
			assert(zoneWriter != null);
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
			IOUtils.copyFile(new File( matsim4opusTempDirectory + FILE_NAME),	new File( matsimOutputDirectory + FILE_NAME));

			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("io did not work") ;
		}
	}

}
