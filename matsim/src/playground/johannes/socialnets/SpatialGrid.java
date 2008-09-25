/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGrid.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnets;

import org.matsim.utils.geometry.Coord;

/**
 * @author illenberger
 *
 */
public class SpatialGrid<T> {

	private Object[][] matrix;
	
	private final double minX;
	
	private final double minY;
	
	private final double maxX;
	
	private final double maxY;
	
	private final double resolution;
	
	public SpatialGrid(Coord lowerLeft, Coord upperRight, double resolution) {
		minX = lowerLeft.getX();
		minY = lowerLeft.getY();
		maxX = upperRight.getX();
		maxY = upperRight.getY();
		this.resolution = resolution;
		int numXBins = (int)Math.ceil((maxX - minX) / resolution);
		int numYBins = (int)Math.ceil((maxY - minY) / resolution);
		matrix = new Object[numXBins][numYBins];
	}
	
	@SuppressWarnings("unchecked")
	public T getValue(Coord coord) {
		if(isInBounds(coord))
			return (T)matrix[getXIndex(coord.getX())][getYIndex(coord.getY())];
		else
			return null;
	}
	
	public boolean setValue(T value, Coord coord) {
		if(isInBounds(coord)) {
			matrix[getXIndex(coord.getX())][getYIndex(coord.getY())] = value;
			return true;
		} else
			return false;
	}
	
	public boolean isInBounds(Coord coord) {
		return coord.getX() >= minX && coord.getX() <= maxX &&
				coord.getY() >= minY && coord.getY() <= maxY;
	}
	
	private int getXIndex(double xCoord) {
		return (int)Math.floor((xCoord - minX) / resolution);
	}
	
	private int getYIndex(double yCoord) {
		return (int)Math.floor((yCoord - minY) / resolution);
	}
}
