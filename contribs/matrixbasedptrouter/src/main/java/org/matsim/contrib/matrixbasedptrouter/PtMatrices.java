package org.matsim.contrib.matrixbasedptrouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.matrices.Matrix;

public class PtMatrices {

	static final Logger log = Logger.getLogger(PtMatrices.class);

	static void fillODMatrix(Matrix odMatrix, Map<Id, PtStop> ptStopHashMap,
			BufferedReader br, boolean isTravelTimes) throws IOException {

		long wrnCnt = 0;
		long wrnCntParam = 0;
		long wrnCntId = 0;
		long wrnCntFormat = 0;

		int originPtStopIDX 		= 0;		// column index for origin pt stop id
		int destinationPtStopIDX 	= 1;		// column index for destination pt stop id
		int valueIDX 				= 2;		// column index for travel time or distance value

		final double EMPTY_ENTRY 	= 999999.;	// indicates that a value is not set (VISUM specific)
		final String SEPARATOR 		= ";";		// 

		String line 				= null;		// line from input file
		String parts[] 				= null;		// values accessible via array

		while ( (line = br.readLine()) != null ) {

			line = line.trim().replaceAll("\\s+", SEPARATOR);
			parts = line.split(SEPARATOR);

			if(parts.length != 3){

				if(wrnCnt < 20)
					log.warn("Could not parse line: " + line);
				else if(wrnCnt == 20)
					log.error( "Found " + wrnCnt + " warnings of type 'could nor parse line'. There is probably something seriously wrong. Please check. Reasons for this error may be that attributes like the pt sto id and/or x and y coordinates are missing.");
				wrnCnt++;
				continue;
			}

			try{
				// trying to convert items into integers
				Long originPtStopAsLong 	= Long.parseLong(parts[originPtStopIDX]);
				Long destinationPtStopAsLong= Long.parseLong(parts[destinationPtStopIDX]);
				double value 				= Double.parseDouble(parts[valueIDX]);
				if(isTravelTimes)
					value = value * 60.; 	// convert value from minutes into seconds

				if(value == EMPTY_ENTRY){
					if(wrnCntParam < 20)
						log.warn("No parameter set: " + line);
					else if(wrnCntParam == 20)
						log.error("Found " + wrnCntParam + " warnings of type 'no parameter set'. This means that the VISUM model does not provide any travel times or distances. This message is not shown any further.");
					wrnCntParam++;
					continue;
				}

				// create Id's
				Id originPtStopID 			= new IdImpl(originPtStopAsLong);
				Id destinationPtStopID 		= new IdImpl(destinationPtStopAsLong);

				// check if a pt stop with the given id exists
				if( ptStopHashMap.containsKey(originPtStopID) && 
						ptStopHashMap.containsKey(destinationPtStopID)){

					// add to od matrix
					odMatrix.createEntry(originPtStopID, destinationPtStopID, value);
				}
				else{
					// Print the warn count after reading is finished. We want to know exactly how many stops are missing. Daniel, may '13
					//					if(wrnCntId == 20){
					//						log.error( "Found " + wrnCntId + " warnings of type 'pt stop id not found'. There is probably something seriously wrong. Please check. Reasons for this error may be:");
					//						log.error( "The list of pt stops is incomplete or the stop ids of the VISUM files do not match the ids from the pt stop file.");
					//					} else 
					if(! ptStopHashMap.containsKey(originPtStopID) && wrnCntId < 20)
						log.warn("Could not find an item in QuadTree (i.e. pt station has no coordinates) with pt stop id:" + originPtStopID);
					else if(! ptStopHashMap.containsKey(destinationPtStopID) && wrnCntId < 20)
						log.warn("Could not find an item in QuadTree (i.e. pt station has no coordinates) with pt stop id:" + destinationPtStopID);

					wrnCntId++;
					continue;
				}
			} catch(NumberFormatException nfe){
				if(wrnCntFormat < 20)
					log.warn("Could not convert values into integer: " + line);
				else if(wrnCntFormat == 20)
					log.error("Found " + wrnCntFormat + " warnings of type 'pt stop id not found'. There is probably something seriously wrong. Please check if id's are provided as 'long' type and travel values (times, distances) as 'double' type.");
				wrnCntFormat++;
				continue;
			}
		}
		//		Print the warn count after reading is finished. We want to know exactly how many stops are missing. Daniel, may '13
		if(wrnCntId > 0){
			log.error( "Found " + wrnCntId + " warnings of type 'pt stop id not found'. There is probably something seriously wrong. Please check. Reasons for this error may be:");
			log.error( "The list of pt stops is incomplete or the stop ids of the VISUM files do not match the ids from the pt stop file.");
		}
	}





	static Map<Id, PtStop> convertQuadTree2HashMap(QuadTree<PtStop> qTree){

		Iterator<PtStop> ptStopIterator = qTree.values().iterator();
		Map<Id, PtStop> ptStopHashMap = new ConcurrentHashMap<Id, PtStop>();

		while(ptStopIterator.hasNext()){
			PtStop ptStop = ptStopIterator.next();
			ptStopHashMap.put(ptStop.getId(), ptStop);
		}
		return ptStopHashMap;
	}


}
