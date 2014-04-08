package org.matsim.contrib.matsim4urbansim.utils.io.writer;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.SpatialReferenceObject;
import org.matsim.core.utils.io.IOUtils;


public class AnalysisPopulationCSVWriter {
	
	private static final Logger log = Logger.getLogger(AnalysisPopulationCSVWriter.class);
	
	public static final String FILE_NAME= "population.csv";
	public static final String FILE_NAME_AGGREGATED= "aggregated_population.csv";
	public static final String PERSONS_COUNT = "persons";
	/**
	 * writing raw population data to disc
	 * @param personLocations
	 * @param module2 TODO
	 * @param file
	 */
	public static void writePopulationData2CSV(final Map<Id, SpatialReferenceObject> personLocations, UrbanSimParameterConfigModuleV3 module){
		
		try{
			log.info("Initializing AnalysisPopulationCSVWriter ...");
			BufferedWriter bwPopulation = IOUtils.getBufferedWriter( module.getMATSim4OpusOutput() + FILE_NAME );
			log.info("Writing (population) data into " + module.getMATSim4OpusOutput() + FILE_NAME + " ...");
			
			// create header
			bwPopulation.write(InternalConstants.PERSON_ID +","+ 
								 InternalConstants.PARCEL_ID +","+ 
								 InternalConstants.X_COORDINATE +","+ 
								 InternalConstants.Y_COORDINATE);
			bwPopulation.newLine();
			
			Iterator<SpatialReferenceObject> personIterator = personLocations.values().iterator();

			while(personIterator.hasNext()){
				
				SpatialReferenceObject person = personIterator.next();
				
				bwPopulation.write(person.getObjectID() + "," + 
								   person.getParcelID() + "," +
								   person.getCoord().getX() + "," +
								   person.getCoord().getY());
				bwPopulation.newLine();
			}
			
			bwPopulation.flush();
			bwPopulation.close();
			log.info("... done!");
		}
		catch(Exception e){ 
			e.printStackTrace(); 
		}
	}
	
	/**
	 * writing aggregated population data to disc
	 * @param personClusterMap
	 * @param module2 TODO
	 */
	public static void writeAggregatedPopulationData2CSV(final Map<Id, AggregationObject> personClusterMap, UrbanSimParameterConfigModuleV3 module){
		try{
			log.info("Initializing AnalysisPopulationCSVWriter ...");
			BufferedWriter bwAggregatedPopulation = IOUtils.getBufferedWriter( module.getMATSim4OpusOutput() + FILE_NAME_AGGREGATED );
			log.info("Writing (population) data into " + module.getMATSim4OpusOutput() + FILE_NAME_AGGREGATED + " ...");
			
			// create header
			bwAggregatedPopulation.write(InternalConstants.PARCEL_ID +","+ 
					 		   InternalConstants.NEARESTNODE_ID +","+
					 		   InternalConstants.NEARESTNODE_X_COORD +","+ 
					 		   InternalConstants.NEARESTNODE_Y_COORD +","+
					 		   PERSONS_COUNT);
			bwAggregatedPopulation.newLine();
			
			Iterator<AggregationObject> personIterator = personClusterMap.values().iterator();

			while(personIterator.hasNext()){
				
				AggregationObject person = personIterator.next();
				
				bwAggregatedPopulation.write(person.getParcelID() + "," +
								   person.getNearestNode().getId() + "," +
								   person.getNearestNode().getCoord().getX() + "," +
								   person.getNearestNode().getCoord().getY() + "," +
								   person.getNumberOfObjects());
				bwAggregatedPopulation.newLine();
			}
			
			bwAggregatedPopulation.flush();
			bwAggregatedPopulation.close();
			log.info("... done!");
		}
		catch(Exception e){ 
			e.printStackTrace(); 
		}
	}
}
