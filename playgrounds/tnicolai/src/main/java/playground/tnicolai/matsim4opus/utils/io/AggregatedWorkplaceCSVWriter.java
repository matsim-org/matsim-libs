package playground.tnicolai.matsim4opus.utils.io;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;

public class AggregatedWorkplaceCSVWriter {
	
	private static final Logger log = Logger.getLogger(AggregatedWorkplaceCSVWriter.class);
	
	public static void writeWorkplaceData2CSV(String file, JobClusterObject[] jobClusterArray){
		
		try{
			log.info("Dumping workplace information as csv to " + file + " ...");
			BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( file );
			
			// create header
			bwAggregatedWP.write(Constants.ERSA_ZONE_ID +","+ Constants.ERSA_PARCEL_ID +","+ Constants.ERSA_NEARESTNODE_ID +","+
								 Constants.ERSA_NEARESTNODE_X_COORD +","+ Constants.ERSA_NEARESTNODE_Y_COORD +","+
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
}
