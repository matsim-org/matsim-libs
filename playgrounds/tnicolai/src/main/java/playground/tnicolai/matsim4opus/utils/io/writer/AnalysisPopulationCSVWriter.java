package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.SpatialReferenceObject;

public class AnalysisPopulationCSVWriter {
	
	private static final Logger log = Logger.getLogger(AnalysisPopulationCSVWriter.class);
	
	public static final String FILE_NAME= "population.csv";
	/**
	 * writing raw population data to disc
	 * @param file
	 * @param personLocations
	 */
	public static void writePopulationData2CSV(final Map<Id, SpatialReferenceObject> personLocations){
		
		try{
			log.info("Initializing AnalysisPopulationCSVWriter ...");
			BufferedWriter bwPopulation = IOUtils.getBufferedWriter( InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME );
			log.info("Writing (population) data into " + InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME + " ...");
			
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
	 * @param file
	 * @param personClusterMap
	 */
	public static void writeAggregatedPopulationData2CSV(final String file, final Map<Id, AggregateObject2NearestNode> personClusterMap){
		
		try{
			log.info("Dumping aggregated person information as csv to " + file + " ...");
			BufferedWriter bwAggregatedPopulation = IOUtils.getBufferedWriter( file );
			
			// create header
			bwAggregatedPopulation.write(InternalConstants.PARCEL_ID +","+ 
					 		   InternalConstants.NEARESTNODE_ID +","+
					 		   InternalConstants.NEARESTNODE_X_COORD +","+ 
					 		   InternalConstants.NEARESTNODE_Y_COORD +","+
					 		   InternalConstants.PERSONS_COUNT);
			bwAggregatedPopulation.newLine();
			
			Iterator<AggregateObject2NearestNode> personIterator = personClusterMap.values().iterator();

			while(personIterator.hasNext()){
				
				AggregateObject2NearestNode person = personIterator.next();
				
				bwAggregatedPopulation.write(person.getParcelID() + "," +
								   person.getNearestNode().getId() + "," +
								   person.getCoordinate().getX() + "," +
								   person.getCoordinate().getY() + "," +
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
