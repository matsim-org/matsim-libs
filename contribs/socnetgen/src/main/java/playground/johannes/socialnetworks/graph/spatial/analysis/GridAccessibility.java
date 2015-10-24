/* *********************************************************************** *
 * project: org.matsim.*
 * GridAccessibility.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.gis.PointUtils;
import playground.johannes.socialnetworks.gis.SpatialCostFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class GridAccessibility extends Accessibility {

	private double resolution;
	
	private final SpatialCostFunction function;
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	/**
	 * @param function
	 */
	public GridAccessibility(SpatialCostFunction function, double resolution) {
		super(function);
		this.function = function;
		this.resolution = resolution;
	}

	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		List<Point> points = new ArrayList<Point>(vertices.size());
		for(Vertex v : vertices) {
			points.add(((SpatialVertex) v).getPoint());
		}
		Envelope env = PointUtils.envelope(points);
		
		int rows = (int) Math.ceil(env.getHeight() / resolution);
		int cols = (int) Math.ceil(env.getWidth() / resolution);
		
		double[][] matrix = new double[rows][cols];
		
		ProgressLogger.init(rows * cols, 1, 5);
		for(int row = 0; row < rows; row++) {
			for(int col = 0; col < cols; col++) {
				Point p1 = geoFactory.createPoint(new Coordinate(col * resolution + (resolution/2.0), row * resolution + (resolution/2.0)));
				double cost = 0;
				for(int i = 0; i < rows; i++) {
					for(int j = 0; j < cols; j++) {
						Point p2 = geoFactory.createPoint(new Coordinate(j * resolution + (resolution/2.0), i * resolution + (resolution/2.0)));
						cost +=  Math.exp(-function.costs(p1, p2));
					}
				}
				matrix[row][col] = cost/(rows * cols);
				ProgressLogger.step();
			}
		}
		ProgressLogger.termiante();
		
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>(vertices.size());
		for(Vertex v : vertices) {
			int row = (int) Math.floor((((SpatialVertex)v).getPoint().getY() - env.getMinY()) / resolution);
			int col = (int) Math.floor((((SpatialVertex)v).getPoint().getX() - env.getMinX())/ resolution);
			values.put(v, matrix[row][col]);
		}
		
		return values;
	}

}
