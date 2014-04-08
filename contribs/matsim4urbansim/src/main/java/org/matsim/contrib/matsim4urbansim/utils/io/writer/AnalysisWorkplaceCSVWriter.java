package org.matsim.contrib.matsim4urbansim.utils.io.writer;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.SpatialReferenceObject;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;


public class AnalysisWorkplaceCSVWriter {
	
	private static final Logger log = Logger.getLogger(AnalysisWorkplaceCSVWriter.class);
	
	public static final String FILE_NAME= "workplaces.csv";
	public static final String FILE_NAME_AGGREGATED= "aggregated_workplaces.csv";
	
	public static final String WORKPLACES_COUNT = "workplaces";
	/**
	 * Writing aggregated workplace data to disc
	 * @param jobClusterArray
	 * @param config TODO
	 * @param file
	 */
	public static void writeAggregatedWorkplaceData2CSV(final AggregationObject[] jobClusterArray, Config config){
		UrbanSimParameterConfigModuleV3 module = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		try{
			log.info("Initializing AnalysisWorkplaceCSVWriter ...");
			BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter(module.getMATSim4OpusOutput() + FILE_NAME_AGGREGATED );
			log.info("Writing (aggregated workplace) data into " + module.getMATSim4OpusOutput() + FILE_NAME_AGGREGATED + " ...");
			
			// create header
			bwAggregatedWP.write(InternalConstants.ZONE_ID +","+ 
								 InternalConstants.PARCEL_ID +","+ 
								 InternalConstants.NEARESTNODE_ID +","+
								 InternalConstants.NEARESTNODE_X_COORD +","+ 
								 InternalConstants.NEARESTNODE_Y_COORD +","+
								 WORKPLACES_COUNT);
			bwAggregatedWP.newLine();
			
			for(int i = 0; i < jobClusterArray.length; i++){
				bwAggregatedWP.write(jobClusterArray[i].getZoneID() + "," + 
									 jobClusterArray[i].getParcelID() + "," +
									 jobClusterArray[i].getNearestNode().getId()  + "," +
									 jobClusterArray[i].getNearestNode().getCoord().getX() + "," +
									 jobClusterArray[i].getNearestNode().getCoord().getY() + "," +
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
	 * @param jobSampleList
	 * @param config TODO
	 * @param file
	 */
	public static void writeWorkplaceData2CSV(final List<SpatialReferenceObject> jobSampleList, Config config){
		UrbanSimParameterConfigModuleV3 module = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		try{
			log.info("Initializing AnalysisWorkplaceCSVWriter ...");
			BufferedWriter bwWorkplaces = IOUtils.getBufferedWriter( module.getMATSim4OpusOutput() + FILE_NAME );
			log.info("Writing data into " + module.getMATSim4OpusOutput() + FILE_NAME + " ...");
			
			
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
