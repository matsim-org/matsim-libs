package org.matsim.contrib.matrixbasedptrouter;

import java.io.BufferedReader;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matrixbasedptrouter.utils.HeaderParser;
import org.matsim.contrib.matrixbasedptrouter.utils.MyBoundingBox;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

class PtStops {

	public static QuadTree<PtStop> readPtStops(String ptStopInputFile, final MyBoundingBox bb) {
		
		long wrnCntSkip = 0;
		long wrnCntParse = 0;
		
		PtMatrix.log.info("Building QuadTree for pt stops.");
	
		QuadTree<PtStop> qTree = new QuadTree<PtStop>(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax() );
		
		try {
			BufferedReader bwPtStops = IOUtils.getBufferedReader(ptStopInputFile);
			
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
						PtMatrix.log.warn("Could not parse line: " + line);
						wrnCntParse++;
					}
					else if(wrnCntParse == 20)
						PtMatrix.log.error( "Found " + wrnCntParse + " warnings of type 'could nor parse line'. There is probably something seriously wrong. Please check. Reasons for this error may be that attributes like the pt sto id and/or x and y coordinates are missing.");
					continue;
				}
				
				// create id for pt stop
				Long id = Long.parseLong( parts[idIDX] );
				Id stopId = new IdImpl(id);
				// create pt stop coordinate
				Coord ptStopCoord = new CoordImpl(parts[xCoordIDX], parts[yCoordIDX]);
				
				boolean isInBoundary = qTree.getMaxEasting() >= ptStopCoord.getX() && 
									   qTree.getMinEasting() <= ptStopCoord.getX() && 
									   qTree.getMaxNorthing() >= ptStopCoord.getY() && 
									   qTree.getMinNorthing() <= ptStopCoord.getY();
				if(!isInBoundary){
					if(wrnCntSkip < 20)
						PtMatrix.log.warn("Pt stop " + id + " lies outside the network boundary and will be skipped!");
					else if(wrnCntSkip == 20)
						PtMatrix.log.error("Found " + wrnCntParse + " warnings of type 'pt stop lies outside the network boundary'. Reasons for this error is that the network defines the boundary for the quad tree that determines the nearest pt station for a given origin/destination location.");
					wrnCntSkip++;
					continue;
				}
				
				PtStop ptStop = new PtStop(stopId, ptStopCoord);
				
				qTree.put(ptStopCoord.getX(), ptStopCoord.getY(), ptStop);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		PtMatrix.log.info("QuadTree for pt stops created.");
		return qTree;
	}

}
