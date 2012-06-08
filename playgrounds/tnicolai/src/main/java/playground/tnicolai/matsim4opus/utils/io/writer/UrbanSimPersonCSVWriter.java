package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;

public class UrbanSimPersonCSVWriter {
	
	private static final Logger log = Logger.getLogger(UrbanSimPersonCSVWriter.class);
	private static BufferedWriter personWriter = null;
	public static final String FILE_NAME= "persons.csv";
	
	/**
	 * writes the header for persons csv file
	 */
	public static void initUrbanSimPersonWriter(){
		try{
			log.info("Initializing UrbanSimZoneCSVWriter ...");
			personWriter = IOUtils.getBufferedWriter( InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME );
			log.info("Writing data into " + InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME + " ...");
			
			// create header
			personWriter.write( InternalConstants.PERSON_ID + "," +
							    "home2work_travel_time_min," +
								"home2work_distance_meter," + 
								"work2home_travel_time_min," +
								"work2home_distance_meter");
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
	public static void write(String personID, double home2WorkTravelTime, double home2WorkDistance, double work2HomeTravelTime, double work2HomeDistance) {

		try {
			assert (UrbanSimPersonCSVWriter.personWriter != null);
			personWriter.write(personID + "," +
							   String.valueOf(home2WorkTravelTime) + "," +
							   String.valueOf(home2WorkDistance) + "," +
							   String.valueOf(work2HomeTravelTime) + "," +
							   String.valueOf(work2HomeDistance));
			personWriter.newLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * finalize and close csv file
	 */
	public static void close(){
		try {
			log.info("Closing UrbanSimPersonCSVWriter ...");
			assert(UrbanSimPersonCSVWriter.personWriter != null);
			personWriter.flush();
			personWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
