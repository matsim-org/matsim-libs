/* *********************************************************************** *
 * project: org.matsim.*
 * DgGrid
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;



/**
 * @author dgrether
 *
 */
public class DgGrid implements Iterable<Polygon> {

	private static final Logger log = Logger.getLogger(DgGrid.class);

	private Polygon[][] grid;

	private GeometryFactory geofac = new GeometryFactory();

	private Envelope boundingBox;
	
	public DgGrid(int cellsX, int cellsY, Envelope boundaries){
		this.boundingBox = boundaries;
		this.grid = new Polygon[cellsX][cellsY];
		double cellSizeX = (boundaries.getMaxX() - boundaries.getMinX()) / cellsX;
		double cellSizeY = (boundaries.getMaxY() - boundaries.getMinY()) / cellsY;
		double lastX = boundaries.getMinX();
		double lastY = boundaries.getMinY();
		double lastX2 = lastX + cellSizeX;
		double lastY2 = lastY + cellSizeY;
		for (int i = 0; i < cellsX; i++) {
			for (int j = 0; j < cellsY; j++){
				Coordinate[] coords = new Coordinate[5];
				coords[0] = new Coordinate(lastX, lastY);
				coords[1] = new Coordinate(lastX2, lastY);
				coords[2] = new Coordinate(lastX2, lastY2);
				coords[3] = new Coordinate(lastX, lastY2);
				coords[4] = new Coordinate(lastX, lastY);
				LinearRing bounds = this.geofac.createLinearRing(coords);
				this.grid[i][j] = this.geofac.createPolygon(bounds, null);
				lastY = lastY2;
				lastY2 += cellSizeY;
			}
			lastY = boundaries.getMinY();
			lastY2 = lastY + cellSizeY;
			lastX = lastX2;
			lastX2 += cellSizeX;
		}
		log.info("Created grid with boundaries min: (" + boundaries.getMinX() + ", " + boundaries.getMinY()
				+ ") max; (" + boundaries.getMaxX() + ", " + boundaries.getMaxY() + ") cellsizeX: " + cellSizeX
				+ " cellsizeY " + cellSizeY + " number of cells x " + cellsX + " number of cells y " + cellsY);
	}

	public Iterator<Polygon> iterator() {
		return new GridIterator() ;
	}
	
	public Polygon[][] getGrid(){
		return this.grid;
	}
	
	public Envelope getBoundingBox(){
		return this.boundingBox;
	}
	
	
	private class GridIterator implements Iterator<Polygon>{
		
		private List<Polygon> list;
		private Iterator<Polygon> delegate;
		
		public GridIterator(){
			this.list = new ArrayList<Polygon>();
			for (int i = 0; i < grid.length; i++) {
				for (int j = 0; j < grid[i].length; j++){
					this.list.add(grid[i][j]);
				}
			}
			this.delegate = list.iterator();
		}
		
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		public Polygon next() {
			return this.delegate.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("Not supported operation!");
		}
	}
}
