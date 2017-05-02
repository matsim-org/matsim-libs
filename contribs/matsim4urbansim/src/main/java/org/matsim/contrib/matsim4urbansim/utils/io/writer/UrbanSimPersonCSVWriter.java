package org.matsim.contrib.matsim4urbansim.utils.io.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;


public class UrbanSimPersonCSVWriter {
	
	private static final Logger log = Logger.getLogger(UrbanSimPersonCSVWriter.class);
	private static BufferedWriter personWriter = null;
	public static final String FILE_NAME= "persons.csv";
	
	/**
	 * writes the header for persons csv file
	 * @param config TODO
	 */
	public static void initUrbanSimPersonWriter(UrbanSimParameterConfigModuleV3 module){
		try{
			log.info("Initializing UrbanSimZoneCSVWriter ...");
			personWriter = IOUtils.getBufferedWriter( module.getMATSim4OpusTemp() + FILE_NAME );
			log.info("Writing data into " + module.getMATSim4OpusTemp() + FILE_NAME + " ...");
			
			// create header
			personWriter.write( InternalConstants.PERSON_ID + "," +
							    "home2work_travel_time_min," +
								"home2work_distance_meter," + 
								"work2home_travel_time_min," +
								"work2home_distance_meter," +
								"mode");
			personWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing person/agent performances to csv file
	 * @param personID
	 * @param home2WorkTravelTime
	 * @param home2WorkDistance
	 * @param work2HomeTravelTime
	 * @param work2HomeDistance
	 */
	public static void write(String personID, double home2WorkTravelTime, double home2WorkDistance, double work2HomeTravelTime, double work2HomeDistance, String mode) {

		try {
			assert (UrbanSimPersonCSVWriter.personWriter != null);
			personWriter.write(personID + "," +
							   String.valueOf(home2WorkTravelTime) + "," +
							   String.valueOf(home2WorkDistance) + "," +
							   String.valueOf(work2HomeTravelTime) + "," +
							   String.valueOf(work2HomeDistance) + "," +
							   mode);
			personWriter.newLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * finalize and close csv file
	 * @param config TODO
	 */
	public static void close(UrbanSimParameterConfigModuleV3 module){
		try {
			log.info("Closing UrbanSimPersonCSVWriter ...");
			assert(UrbanSimPersonCSVWriter.personWriter != null);
			personWriter.flush();
			personWriter.close();
			
			// copy the zones file to the outputfolder...
			log.info("Copying " + module.getMATSim4OpusTemp() + FILE_NAME + " to " + module.getMATSim4OpusOutput() + FILE_NAME);
            try {
                Files.copy(new File( module.getMATSim4OpusTemp() + FILE_NAME).toPath(), new File( module.getMATSim4OpusOutput() + FILE_NAME).toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
