package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.PersonAndJobsObject;

public class AnalysisWorkplaceCSVWriter {
	
	private static final Logger log = Logger.getLogger(AnalysisWorkplaceCSVWriter.class);
	
	/**
	 * Writing aggregated workplace data to disc
	 * @param file
	 * @param jobClusterArray
	 */
	public static void writeAggregatedWorkplaceData2CSV(final String file, final AggregateObject2NearestNode[] jobClusterArray){
		
		try{
			log.info("Dumping aggregated workplace information as csv to " + file + " ...");
			BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( file );
			
			// create header
			bwAggregatedWP.write(Constants.ZONE_ID +","+ 
								 Constants.PARCEL_ID +","+ 
								 Constants.NEARESTNODE_ID +","+
								 Constants.NEARESTNODE_X_COORD +","+ 
								 Constants.NEARESTNODE_Y_COORD +","+
								 Constants.WORKPLACES_COUNT);
			bwAggregatedWP.newLine();
			
			for(int i = 0; i < jobClusterArray.length; i++){
				bwAggregatedWP.write(jobClusterArray[i].getZoneID() + "," + 
									 jobClusterArray[i].getParcelID() + "," +
									 jobClusterArray[i].getNearestNode().getId()  + "," +
									 jobClusterArray[i].getCoordinate().getX() + "," +
									 jobClusterArray[i].getCoordinate().getY() + "," +
									 jobClusterArray[i].getNumberOfObjects());
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
	public static void writeWorkplaceData2CSV(final String file, final List<PersonAndJobsObject> jobSampleList){
		
		try{
			log.info("Dumping workplace information as csv to " + file + " ...");
			BufferedWriter bwWorkplaces = IOUtils.getBufferedWriter( file );
			
			// create header
			bwWorkplaces.write(Constants.JOB_ID +","+ 
								 Constants.PARCEL_ID +","+ 
								 Constants.ZONE_ID +","+
								 Constants.X_COORDINATE +","+ 
								 Constants.Y_COORDINATE);
			bwWorkplaces.newLine();
			
			Iterator<PersonAndJobsObject> jobIterator = jobSampleList.iterator();

			while(jobIterator.hasNext()){
				
				PersonAndJobsObject job = jobIterator.next();
				
				bwWorkplaces.write(job.getObjectID() + "," + 
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
