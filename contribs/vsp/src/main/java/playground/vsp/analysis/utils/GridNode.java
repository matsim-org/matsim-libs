/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.utils;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

/**
 * Stores Type-Count pairs clustered to one node. Optional: shifts the node's centroid to the average of all given data points.
 * 
 * @author aneumann
 *
 */
public class GridNode {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GridNode.class);
	private String id;
	private boolean isShifting = true;
	private double xMean;
	private double yMean;
	private double nEntries = 0;
	
	private HashMap<String, Integer> type2countMap = new HashMap<String, Integer>();

	/**
	 * Creates a unique id from two different slot numbers
	 * 
	 * @param x - number of xSlot
	 * @param y - number of ySlot
	 * @return Unique combination of x and y
	 */
	public static String createGridNodeId(int x, int y){
		return (x + "_" + y);
	}

	/**
	 * Calculates the slot number for a given one-dimensional coordinate 
	 * 
	 * @param coord - the coordinate, e.g. x-coordinate
	 * @param gridSize - distance between two slots
	 * @return the slot number corresponding to the given coordinate
	 */
	public static int getSlotForCoord(double coord, double gridSize){
		return (int) (coord / gridSize);
	}

	/**
	 * Returns the unique node id for a given x,y-coodinate and the gridsize
	 * 
	 * @param coord - the coordinate for which the node is searched for
	 * @param gridSize - distance between two nodes/slots
	 * @return The unique id of the corresponding node
	 */
	public static String getGridNodeIdForCoord(Coord coord, double gridSize){
		int xSlot = GridNode.getSlotForCoord(coord.getX(), gridSize);
		int ySlot = GridNode.getSlotForCoord(coord.getY(), gridSize);
		
		return GridNode.createGridNodeId(xSlot, ySlot);
	}

	/**
	 * Creates a node with shifting centroid
	 * 
	 * @param id
	 */
	public GridNode(String id){
		this.id = id;
	}
	
	/**
	 * Creates a node with fixed coord
	 * 
	 * @param id
	 * @param coord
	 */
	public GridNode(String id, Coord coord){
		this(id);
		this.isShifting = false;
		this.xMean = coord.getX();
		this.yMean = coord.getY();
	}
	
	public void addPoint(String type, Coord coord){
		if(isShifting){
			// register coord
			if (nEntries == 0) {
				xMean = coord.getX();
				yMean = coord.getY();			
			} else {
				xMean =  (this.nEntries * this.xMean + coord.getX()) / (this.nEntries + 1);
				yMean =  (this.nEntries * this.yMean + coord.getY()) / (this.nEntries + 1);
			}
		}
		nEntries++;
		
		// register actType
		if (this.type2countMap.get(type) == null) {
			this.type2countMap.put(type, new Integer(0));
		}
		
		this.type2countMap.put(type, new Integer(this.type2countMap.get(type) + 1));
	}

	public String getId() {
		return id;
	}
	
	public double getX() {
		return xMean;
	}

	public double getY() {
		return yMean;
	}
	
	public int getCountForType(String type) {
		if (this.type2countMap.get(type) == null) {
			return 0;
		} else {
			return this.type2countMap.get(type);
		}
	}

	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("Type Count");
		for (Entry<String, Integer> typeEntry : this.type2countMap.entrySet()) {
			strB.append(" | " + typeEntry.getKey() + " " + typeEntry.getValue().toString());
		}
		return strB.toString();
	}
}