package org.matsim.contrib.matrixbasedptrouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.matrixbasedptrouter.utils.HeaderParser;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Matrix;

final class FileUtils {

	private static final Logger log = LogManager.getLogger(FileUtils.class);

	static QuadTree<PtStop> readPtStops(String ptStopInputFile, final BoundingBox bb) {

		long wrnCntSkip = 0;
		long wrnCntParse = 0;

		log.info("Building QuadTree for pt stops.");

		QuadTree<PtStop> qTree = new QuadTree<PtStop>(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax() );

		try (BufferedReader bwPtStops = IOUtils.getBufferedReader(ptStopInputFile)) {
			String separator = ",";

			// read header
			String line = bwPtStops.readLine();
			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, separator);
			final int idIDX 	= idxFromKey.get("id");
			final int xCoordIDX = idxFromKey.get("x");
			final int yCoordIDX = idxFromKey.get("y");

			String parts[];

			// read input file line by line
			while( (line = bwPtStops.readLine()) != null ){

				parts = line.split(separator);

				if(parts.length < idIDX || parts.length < xCoordIDX || parts.length < yCoordIDX){

					if(wrnCntParse < 20){
						log.warn("Could not parse line: " + line);
						wrnCntParse++;
					}
					else if(wrnCntParse == 20)
						log.error( "Found " + wrnCntParse + " warnings of type 'could nor parse line'. There is probably something seriously wrong. Please check. Reasons for this error may be that attributes like the pt sto id and/or x and y coordinates are missing.");
					continue;
				}

				// create id for pt stop
				Id<PtStop> stopId = Id.create(parts[idIDX], PtStop.class);
				// create pt stop coordinate
				Coord ptStopCoord = new Coord(Double.parseDouble(parts[xCoordIDX]), Double.parseDouble(parts[yCoordIDX]));

				boolean isInBoundary = qTree.getMaxEasting() >= ptStopCoord.getX() &&
						qTree.getMinEasting() <= ptStopCoord.getX() &&
						qTree.getMaxNorthing() >= ptStopCoord.getY() &&
						qTree.getMinNorthing() <= ptStopCoord.getY();
						if(!isInBoundary){
							if(wrnCntSkip < 20)
								log.warn("Pt stop " + stopId + " lies outside the network boundary and will be skipped!");
							else if(wrnCntSkip == 20)
								log.error("Found " + wrnCntParse + " warnings of type 'pt stop lies outside the network boundary'. Reasons for this error is that the network defines the boundary for the quad tree that determines the nearest pt station for a given origin/destination location.");
							wrnCntSkip++;
							continue;
						}

						PtStop ptStop = new PtStop(stopId, ptStopCoord);

						qTree.put(ptStopCoord.getX(), ptStopCoord.getY(), ptStop);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info("QuadTree for pt stops created.");
		return qTree;
	}

	static void fillODMatrix(Matrix odMatrix, Map<Id<PtStop>, PtStop> ptStopHashMap,
			BufferedReader br, boolean isTravelTimes) {

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

		try {
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

//				try{
					// trying to convert items into integers
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
					Id<PtStop> originPtStopID 			= Id.create(parts[originPtStopIDX], PtStop.class);
					Id<PtStop> destinationPtStopID 		= Id.create(parts[destinationPtStopIDX], PtStop.class);

					// check if a pt stop with the given id exists
					if( ptStopHashMap.containsKey(originPtStopID) &&
							ptStopHashMap.containsKey(destinationPtStopID)){

						// add to od matrix
						odMatrix.createAndAddEntry(originPtStopID.toString(), destinationPtStopID.toString(), value);
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
//				} catch(NumberFormatException nfe){
//					if(wrnCntFormat < 20)
//						log.warn("Could not convert values into integer: " + line);
//					else if(wrnCntFormat == 20)
//						log.error("Found " + wrnCntFormat + " warnings of type 'pt stop id not found'. There is probably something seriously wrong. Please check if id's are provided as 'long' type and travel values (times, distances) as 'double' type.");
//					wrnCntFormat++;
//					continue;
//				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//		Print the warn count after reading is finished. We want to know exactly how many stops are missing. Daniel, may '13
		if(wrnCntId > 0){
			log.error( "Found " + wrnCntId + " warnings of type 'pt stop id not found'. There is probably something seriously wrong. Please check. Reasons for this error may be:");
			log.error( "The list of pt stops is incomplete or the stop ids of the VISUM files do not match the ids from the pt stop file.");
		}
	}

}
