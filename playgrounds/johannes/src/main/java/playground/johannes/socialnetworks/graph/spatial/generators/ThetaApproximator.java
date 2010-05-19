/* *********************************************************************** *
 * project: org.matsim.*
 * ThetaApproximator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

/**
 * @author illenberger
 *
 */
public class ThetaApproximator {
	
	private static final Logger logger = Logger.getLogger(ThetaApproximator.class);

	public TObjectDoubleHashMap<SpatialVertex> approximate(Set<SpatialVertex> vertices, double budget, EdgeCostFunction costFunction) {
		double xMin = Double.MAX_VALUE;
		double yMin = Double.MAX_VALUE;
		double xMax = -Double.MAX_VALUE;
		double yMax = -Double.MAX_VALUE;
		
		for(SpatialVertex vertex : vertices) {
			xMin = Math.min(xMin, vertex.getPoint().getCoordinate().x);
			yMin = Math.min(yMin, vertex.getPoint().getCoordinate().y);
			xMax = Math.max(xMax, vertex.getPoint().getCoordinate().x);
			yMax = Math.max(yMax, vertex.getPoint().getCoordinate().y);
		}
		
		double binsize = 1000;
		
		int xDim = (int) Math.ceil((xMax - xMin)/binsize);
		int yDim = (int) Math.ceil((yMax - yMin)/binsize);
		Set<SpatialVertex>[][] grid = new Set[xDim][yDim];
		
		for(SpatialVertex vertex : vertices) {
			Set<SpatialVertex> cell = getCell(vertex, grid, binsize, xMin, yMin);
			cell.add(vertex);
		}
		
		TObjectDoubleHashMap<SpatialVertex> sample = new TObjectDoubleHashMap<SpatialVertex>();
		
		for(int x = 0; x < xDim; x++) {
			for(int y = 0; y < yDim; y++) {
				Set<SpatialVertex> cell = grid[x][y];
				SpatialVertex vertex = null;
				if(cell != null && !cell.isEmpty())
					vertex = cell.iterator().next();
				if(vertex != null) {
					sample.put(vertex, budget);
				}
			}
		}
		
		logger.info(String.format("Original size = %1$s, reduced size = %2$s.", vertices.size(), sample.size()));
		
		ThetaSolver solver = new ThetaSolver(costFunction);
		TObjectDoubleHashMap<SpatialVertex> cellThetas = solver.solve(sample);
		
		TObjectDoubleHashMap<SpatialVertex> thetas = new TObjectDoubleHashMap<SpatialVertex>();
		
		TObjectDoubleIterator<SpatialVertex> it = cellThetas.iterator();
		for(int i = 0; i < cellThetas.size(); i++) {
			it.advance();
			SpatialVertex vertex = it.key();
			
			Set<SpatialVertex> cell = getCell(vertex, grid, binsize, xMin, yMin);
			
			for(SpatialVertex v : cell) {
				thetas.put(v, it.value());
			}
		} 
		
		return thetas;
	}
	
	private Set<SpatialVertex> getCell(SpatialVertex vertex, Set<SpatialVertex>[][] grid, double binsize, double xMin, double yMin) {
		int xBin = (int)Math.floor((vertex.getPoint().getCoordinate().x - xMin)/binsize);
		int yBin = (int)Math.floor((vertex.getPoint().getCoordinate().y - yMin)/binsize);
		
		Set<SpatialVertex> cell = grid[xBin][yBin];
		if(cell == null) {
			cell = new HashSet<SpatialVertex>();
			grid[xBin][yBin] = cell;
		}
		
		return cell;
	}
}
