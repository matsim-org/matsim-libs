/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.tnicolai.matsim4opus.gis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.helperObjects.AccessibilityStorage;

public class FixedSizeGrid {
	
	private static final Logger logger = Logger.getLogger(FixedSizeGrid.class);
	
	private AccessibilityStorage[][] grid;
	
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	private int rowPoints;
	private int colPoints;
	
	private int coarseningSteps;
	private double resolution;
	
	/**
	 * creates numbers 
	 * @param gridSizeInMeter
	 * @param network
	 * @param resultMap
	 * @param coarseningSteps
	 */
	public FixedSizeGrid(final double gridSizeInMeter, final NetworkImpl network, final Map<Id, AccessibilityStorage> resultMap, int coarseningSteps){
		
		logger.info("Initializing Grid ...");
		
		if(coarseningSteps < 0){
			logger.warn("Detected a negative number of steps to coarse the grid! Setting number of steps to zero!");
			this.coarseningSteps = 0;
		}
		else
			this.coarseningSteps = coarseningSteps;
		
		this.resolution = gridSizeInMeter;
		
		assert(network != null);
		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());
		
		this.minX = networkBoundingBox[0];
		this.minY = networkBoundingBox[1];
		this.maxX = minX + ((colPoints - 1) * gridSizeInMeter); // round up max x-coordinate 
		this.maxY = minY + ((rowPoints - 1) * gridSizeInMeter); // round up max y-coordinate
		this.rowPoints = (int)Math.ceil( (this.maxY - this.minY) / gridSizeInMeter ) + 1;
		this.colPoints = (int)Math.ceil( (this.maxX - this.minX) / gridSizeInMeter ) + 1;

		
		logger.info("Determined area:");
		logger.info("Y Min: " + this.minY);
		logger.info("Y Max: " + this.maxY + "(this extended from " + networkBoundingBox[3] + ").");
		logger.info("X Min: " + this.minX);
		logger.info("X Max: " + this.maxX + "(this extended from " + networkBoundingBox[2] + ").");
		
		logger.info("Create Grid with " + colPoints + " columns and " + rowPoints + " rows ...");
		grid = new AccessibilityStorage[rowPoints][colPoints];
		
		double xCoord;
		double yCoord;
		assert(resultMap != null);
		
		for(int col = 0; col < colPoints; col++){
			
			xCoord = minX + (col * gridSizeInMeter);
			
			for(int row = 0; row < rowPoints; row++){
				
				yCoord = minY + (row * gridSizeInMeter);

				// create coordinate from current x, y values
				Coord coordinate = new CoordImpl(xCoord, yCoord);
				// get corresponding nearest network Node
				Node node = network.getNearestNode(coordinate);
				
				// set accessibility values (AccessibilityStorage object)
				grid[row][col] = resultMap.get(node.getId());
			}
		}
		
		logger.info("Done initializing Grid!");
	}
	
	public void writeGrid(){
		for(int coarseFactor = 0; coarseFactor <= this.coarseningSteps; coarseFactor++)
			write(coarseFactor);
	}
	
	/**
	 * writing grid matrices as txt file into "matsim4opus/tmp" directory
	 * 
	 * @param coarseFactor
	 */
	private void write(int coarseFactor){
		
		logger.info("Writing accessibility matrix with coarse factor = " + coarseFactor);
		double currResolution = Math.pow(2, coarseFactor) * this.resolution;
		logger.info("The matrix has a relolution of " + currResolution + " meter.");
		
		try{
			BufferedWriter ttWriter = IOUtils.getBufferedWriter(InternalConstants.MATSIM_4_OPUS_TEMP + currResolution + InternalConstants.CONGESTED_TRAVEL_TIME_ACCESSIBILITY + InternalConstants.FILE_TYPE_TXT);
			
			// writing x coordinates (header)
			for(int col = 0; (col < colPoints); col++ ){
				
				if(skipData(col, coarseFactor))
					continue;
				
				
				// determine x coord
				double xCoord = this.minX + (col * currResolution);
				
				ttWriter.write("\t");
				ttWriter.write( String.valueOf( xCoord ));
			}
			ttWriter.newLine();
			
			// writing accessibility values row by row with corresponding y-coordinates in first column (as header)
			for(int row = rowPoints - 1; row >= 0; row--){
				
				if(skipData(row, coarseFactor))
					continue;
				
				//determine y coord
				double yCoord = this.maxY - (row * currResolution);
				// writing y-coordinates (header)
				ttWriter.write( String.valueOf( yCoord ) );
				
				// writing a row (at yCoord)
				for(int col = 0; col < colPoints; col++ ){
					
					if(skipData(col, coarseFactor))
						continue;
					
					
					ttWriter.write("\t");
					ttWriter.write( String.valueOf( grid[row][col].getCongestedTravelTimeAccessibility() ));
				}
				ttWriter.newLine();
			}
			// close writer
			ttWriter.flush();
			ttWriter.close();
			
			logger.info("Done writing accessibility matrix with coase factor " + coarseFactor + "!");
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	private boolean skipData(int row, int coarseFactor){
		int mod = (int) (row % Math.pow(2, coarseFactor));
		boolean b = (mod != 0);
		return b;
	}

	/**
	 * Testing only
	 * @param args
	 */
	public static void main(String args[]){
		
		for(int i = 0; i < 30; i++){
			for(int f = 1; f < 5; f++){
			System.out.println("i=" + i + " with modulo (2**"+f+")= " + (i % (Math.pow(2., f))));
			}
			System.out.println("---");
		}
	}
}
