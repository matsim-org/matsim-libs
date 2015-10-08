/* *********************************************************************** *
 * project: org.matsim.*
 * DigiGrid2D.java
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

package playground.southafrica.projects.digicore.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class that acts as the container for the two-dimensional grid containing
 * the centroids of the hexagons that is used for the Digicore accelerometer
 * research. Associated with the grid is the number of observations in each
 * polygon (cell) and also the rating of each cell. The x and y-dimensions
 * relate to lateral and longitudinal acceleration, while the z-dimension is
 * ignored.
 * 
 * @see DigiGrid3D for a three-dimensional implementation.
 *
 * @author jwjoubert
 */
public abstract class DigiGrid2D extends DigiGrid {
	final private Logger log = Logger.getLogger(DigiGrid2D.class);
	final private GeometryFactory gf;
	protected GeneralGrid grid;
	private final double scale;
	private double pointsConsidered = 0.0;

	protected Map<Point, Double> map;
	protected Map<Point, Integer> mapRating;

	public DigiGrid2D(final double scale) {
		this.scale = scale;
		this.grid = new GeneralGrid(scale, GridType.HEX);
		this.gf = new GeometryFactory();
	}
	
	@Override
	public void setupGrid(String filename) {
		log.info("Setting up grid from " + filename);
		/* Get the extreme points from the data.
		 * TODO For now (11 May) this will only hard coded values. In future
		 * one may want to parse this from the raw data. */
		double minX = -1000d;
		double maxX = 1000d;
		double minY = -1200d;
		double maxY = 1200d;
		
		/* Establish the geometry used to build the grid. */
		Coordinate c1 = new Coordinate(minX, minY); // Bottom-left
		Coordinate c2 = new Coordinate(maxX, minY); // Bottom-right
		Coordinate c3 = new Coordinate(maxX, maxY); // Top-right
		Coordinate c4 = new Coordinate(minX, maxY); // Top-left
		Coordinate[] ca = {c1, c2, c3, c4, c1};
		Polygon polygon = gf.createPolygon(ca);
		
		/* Build the grid. */
		grid.generateGrid(polygon);
		
		/* Initialise each cell with a zero count. */
		this.map = new HashMap<Point, Double>();
		for(Point p : this.grid.getGrid().values()){
			this.map.put(p, new Double(0.0));
		}
		
		log.info("Done setting up grid.");
	}
	
	
	public void incrementValue(double x, double y, double weight){
		Point point = gf.createPoint(new Coordinate(x, y));
		if(grid.isInGrid(point)){
			Point centroid = grid.getGrid().getClosest(x, y);
			map.put(centroid, map.get(centroid) + weight);
			this.pointsConsidered += weight;
			this.setPopulated(true);
		} else{
			String coord = String.format("(%f;%f)", x, y);
			throw new RuntimeException("The point " + coord + 
					" is not within the grid extent. Reconsider the method DigiGrid2D.setupGrid(...)");
		}
	}
	
	public double getValue(Point p){
		return this.map.get(p);
	}
	
	
	/**
	 * 
	 * @param p
	 * @return
	 * @throws IllegalArgumentException if the method is called without having 
	 * 		   the grid first being ranked.  
	 */
	public int getCellRisk(Point p) throws IllegalArgumentException{
		if(!this.isRanked()){
			throw new IllegalArgumentException("Cannot get cell risk. Grid has been ranked yet.");
		} else{
			return this.mapRating.get(p);
		}
	}
	

	/**
	 * Sort the cells based on their values only.
	 */
	@Override
	public void rankGridCells() {
		if(!this.isPopulated() || this.map.size() == 0){
			throw new RuntimeException("Cannot rank zero cells. Grid has possibly not been populated yet.");
		}
		log.info("Ranking grid cells...");

		/* Cells are ranked based on the weighted number of records associated 
		 * with them. Sorted from highest to lowest. */
		Comparator<Point> comparator = new Comparator<Point>() {
			@Override
			public int compare(Point o1, Point o2) {
				return map.get(o2).compareTo(map.get(o1));
			}
		};
		List<Point> sortedCells = new ArrayList<>(map.keySet());
		Collections.sort(sortedCells, comparator);
		
		/* Report the top 20 cell values. */
		log.info("   20 polyhedra with largest number of observations:");
		for(int i = 0; i < 20; i++){
			log.info(String.format("      %d: %.1f observations", i+1, map.get(sortedCells.get(i))));
		}

		double totalAdded = 0.0;
		double cumulative = 0.0;
		mapRating = new TreeMap<Point, Integer>(comparator);
		
		List<Point> cellsToRemove = new ArrayList<Point>();

		double maxValue = 0.0;
		for(int i = 0; i < sortedCells.size(); i++){
			Point p = sortedCells.get(i);
			double obs = map.get(p);
			if(obs > 0){
				maxValue = Math.max(maxValue, (double)obs);
				totalAdded += (double)obs;
				cumulative = totalAdded / pointsConsidered;
				
				/* Get the rating class for this value. */
				Integer ratingZone = null;
				int zoneIndex = 0;
				while(ratingZone == null && zoneIndex < getRiskThresholds().size()){
					if(cumulative <= getRiskThresholds().get(zoneIndex)){
						ratingZone = new Integer(zoneIndex);
					} else{
						zoneIndex++;
					}
				}
				mapRating.put(p, ratingZone);
			} else{
				cellsToRemove.add(p);
			}
		}

		/* Remove zero-count hexagons. */
		for(Point p : cellsToRemove){
			map.remove(p);
		}
		
		this.setRanked(true);
		log.info("Done ranking grid cells.");
		log.info("A total of " + map.size() + " hexagons contain points (max value: " + maxValue + ")");
	}
	
	public int getCellRisk(double x, double y){
		Point record = this.gf.createPoint(new Coordinate(x, y));
		if(this.grid.isInGrid(record)){
			Point p = this.grid.getGrid().getClosest(x, y);
			return this.getCellRisk(p);
		} else{
			/* The record is not in the grid! */
			String coord = String.format("(%f;%f)", x, y);
			throw new RuntimeException("The point " + coord + 
					" is not within the grid extent. Reconsider the method DigiGrid2D.setupGrid(...)");
		}
	}
	
	public double getScale(){
		return this.scale;
	}
	
	
	public abstract void writeCellCountsAndRiskClasses(String outputFolder);
	

	
}
