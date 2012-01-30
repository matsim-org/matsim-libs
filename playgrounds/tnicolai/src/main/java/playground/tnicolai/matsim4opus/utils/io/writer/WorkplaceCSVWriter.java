package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobsObject;

public class WorkplaceCSVWriter {
	
	private static final Logger log = Logger.getLogger(WorkplaceCSVWriter.class);
	
	/**
	 * Writing aggregated workplace data to disc
	 * @param file
	 * @param jobClusterArray
	 */
	public static void writeAggregatedWorkplaceData2CSV(String file, JobClusterObject[] jobClusterArray){
		
		try{
			log.info("Dumping aggregated workplace information as csv to " + file + " ...");
			BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( file );
			
			// create header
			bwAggregatedWP.write(Constants.ERSA_ZONE_ID +","+ 
								 Constants.ERSA_PARCEL_ID +","+ 
								 Constants.ERSA_NEARESTNODE_ID +","+
								 Constants.ERSA_NEARESTNODE_X_COORD +","+ 
								 Constants.ERSA_NEARESTNODE_Y_COORD +","+
								 Constants.ERSA_WORKPLACES_COUNT);
			bwAggregatedWP.newLine();
			
			for(int i = 0; i < jobClusterArray.length; i++){
				bwAggregatedWP.write(jobClusterArray[i].getZoneID() + "," + 
									 jobClusterArray[i].getParcelID() + "," +
									 jobClusterArray[i].getNearestNode().getId()  + "," +
									 jobClusterArray[i].getCoordinate().getX() + "," +
									 jobClusterArray[i].getCoordinate().getY() + "," +
									 jobClusterArray[i].getNumberOfJobs());
				bwAggregatedWP.newLine();
			}
			
			bwAggregatedWP.flush();
			bwAggregatedWP.close();
			log.info("... done!");
		}
		catch(Exception e){ 
			e.printStackTrace(); 
		}
	}
	
	/**
	 * writing raw workplace data to disc
	 * @param file
	 * @param jobSampleList
	 */
	public static void writeWorkplaceData2CSV(String file, List<JobsObject> jobSampleList){
		
		try{
			log.info("Dumping workplace information as csv to " + file + " ...");
			BufferedWriter bwWorkplaces = IOUtils.getBufferedWriter( file );
			
			// create header
			bwWorkplaces.write(Constants.ERSA_JOB_ID +","+ 
								 Constants.ERSA_PARCEL_ID +","+ 
								 Constants.ERSA_ZONE_ID +","+
								 Constants.ERSA_X_COORDNIATE +","+ 
								 Constants.ERSA_Y_COORDINATE);
			bwWorkplaces.newLine();
			
			Iterator<JobsObject> jobIterator = jobSampleList.iterator();

			while(jobIterator.hasNext()){
				
				JobsObject job = jobIterator.next();
				
				bwWorkplaces.write(job.getJobID() + "," + 
								   job.getParcelID() + "," +
								   job.getZoneID() + "," +
								   job.getCoord().getX() + "," +
								   job.getCoord().getY());
				bwWorkplaces.newLine();
			}
			
			bwWorkplaces.flush();
			bwWorkplaces.close();
			log.info("... done!");
		}
		catch(Exception e){ 
			e.printStackTrace(); 
		}
	}
}
