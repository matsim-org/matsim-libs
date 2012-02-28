/* *********************************************************************** *
 * project: org.matsim.*
 * BiTreeGridBuilder.java
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
package playground.johannes.socialnetworks.gis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import playground.johannes.socialnetworks.gis.BiTreeGrid.Tile;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class BiTreeGridBuilder {

	public static <T> BiTreeGrid<T> createEqualCountGrid(Set<Point> points, int splitThreshold, double minSize) {
		Envelope env = PointUtils.envelope(points);
		BiTreeGrid<List<Point>> grid = new BiTreeGrid<List<Point>>(env);
		grid.getRoot().data = new ArrayList<Point>(points);
		split(grid, grid.getRoot(), points, splitThreshold, minSize);
		
		return new BiTreeGrid<T>(grid);
	}
	
	private static void split(BiTreeGrid<List<Point>> grid, Tile<List<Point>> tile, Set<Point> points, int threshold, double minSize) {
		if(tile.data.size() > threshold) {
			double dx = tile.envelope.getWidth();
			double dy = tile.envelope.getHeight();
			
			if(dx > minSize && dy > minSize) {
				grid.splitTile(tile);
				
				for(Tile<List<Point>> child : tile.children) {
					child.data = new ArrayList<Point>(tile.data.size());
				}
				
				for(Point p : tile.data) {
					for(Tile<List<Point>> child : tile.children) {
						if(child.envelope.contains(p.getCoordinate())) {
							child.data.add(p);
						}
					}
				}
				
				for(Tile<List<Point>> child : tile.children) {
					split(grid, child, points, threshold, minSize);
				}
			}
		}
	}

	
}
