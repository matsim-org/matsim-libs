package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.SpatialReferenceObject;

public class AnalysisWorkplaceCSVWriter {
	
	private static final Logger log = Logger.getLogger(AnalysisWorkplaceCSVWriter.class);
	
	public static final String FILE_NAME= "aggregated_workplaces.csv";
	/**
	 * Writing aggregated workplace data to disc
	 * @param file
	 * @param jobClusterArray
	 */
	public static void writeAggregatedWorkplaceData2CSV(final AggregateObject2NearestNode[] jobClusterArray){
		
		try{
			log.info("Initializing AnalysisWorkplaceCSVWriter ...");
			BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME );
			log.info("Writing (aggregated workplace) data into " + InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME + " ...");
			
			// create header
			bwAggregatedWP.write(InternalConstants.ZONE_ID +","+ 
								 InternalConstants.PARCEL_ID +","+ 
								 InternalConstants.NEARESTNODE_ID +","+
								 InternalConstants.NEARESTNODE_X_COORD +","+ 
								 InternalConstants.NEARESTNODE_Y_COORD +","+
								 InternalConstants.WORKPLACES_COUNT);
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
	public static void writeWorkplaceData2CSV(final String file, final List<SpatialReferenceObject> jobSampleList){
		
		try{
			log.info("Dumping workplace information as csv to " + file + " ...");
			BufferedWriter bwWorkplaces = IOUtils.getBufferedWriter( file );
			
			// create header
			bwWorkplaces.write(InternalConstants.JOB_ID +","+ 
								 InternalConstants.PARCEL_ID +","+ 
								 InternalConstants.ZONE_ID +","+
								 InternalConstants.X_COORDINATE +","+ 
								 InternalConstants.Y_COORDINATE);
			bwWorkplaces.newLine();
			
			Iterator<SpatialReferenceObject> jobIterator = jobSampleList.iterator();

			while(jobIterator.hasNext()){
				
				SpatialReferenceObject job = jobIterator.next();
				
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
