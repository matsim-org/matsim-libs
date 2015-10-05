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
package playground.vsp.analysis.utils.heatMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author droeder
 *
 */
public class HeatMap {

	private static final Logger log = Logger.getLogger(HeatMap.class);
	private List<Tuple<Coord, Double>> values;
	private double maxX = -Double.MAX_VALUE;
	private double minX = Double.MAX_VALUE;
	private double minY = Double.MAX_VALUE;
	private double maxY = -Double.MAX_VALUE;
	private QuadTree<Tile> tiles;
	private Integer gridSize;
	
	private class Tile{
		
		private Polygon area;
		private Double value;

		/**
		 * @param x
		 * @param d
		 * @param y
		 * @param e
		 */
		public Tile(double minX, double maxX, double minY, double maxY) {
			GeometryFactory factory = new GeometryFactory();
			Coordinate[] c = new Coordinate[5];
			Double min = 0.999999999;
			Double max = 1.000000001;
			// extend the tile for a very small distance, so that the border is within the tile!
			c[0] = new Coordinate(min * minX, min * minY, 0.);
			c[1] = new Coordinate(max * maxX, min * minY, 0.);
			c[2] = new Coordinate(max * maxX, max * maxY, 0.);
			c[3] = new Coordinate(min * minX, max * maxY, 0.);
			c[4] = c[0];
			this.area = factory.createPolygon(factory.createLinearRing(c), null);
			this.value = 0.;
		}
		
		public Coord getCentroid(){
			return MGC.point2Coord(this.area.getCentroid());
		}
		
		public Polygon getGeometry(){
			return this.area;
		}
		
		public void add(Double d){
			this.value += d;
		}
		
		public double getValue(){
			return this.value;
		}
	}
	
	/**
	 * a class to create a simple HeatMap from coordinates and corresponding values.
	 * 
	 * @param gridSize, the number of tiles of the HeatMap
	 */
	public HeatMap(Integer gridSize) {
		this.gridSize = gridSize;
		this.values = new ArrayList<Tuple<Coord,Double>>();
	}
	
	public void addValue(Coord coord, Double value){
		this.values.add(new Tuple<Coord, Double>(coord, value));
		findMinMax(coord);
	}

	/**
	 * @param coord
	 */
	private void findMinMax(Coord coord) {
		if(coord.getX() > this.maxX ){
			this.maxX = coord.getX();
		}
		if(coord.getX() < this.minX ){
			this.minX = coord.getX();
		}
		if(coord.getY() > this.maxY ){
			this.maxY = coord.getY();
		}
		if(coord.getY() < this.minY ){
			this.minY = coord.getY();
		}
	}
	
	public void createHeatMap(){
		this.tiles = new QuadTree<Tile>(minX, minY, maxX, maxY);
		Double dx, dy, inc;
		dx = maxX - minX;
		dy = maxY - minY;
		if(dx > dy){
			inc = dx / (1.0 * this.gridSize);
		}else{
			inc = dy / (1.0 * this.gridSize);
		}
		Double minX, maxX, minY, maxY;
		minX = Math.min(0.9999999 * this.minX, 1.0000001 * this.minX);
		maxX = Math.max(0.9999999 * this.maxX, 1.0000001 * this.maxX);
		minY = Math.min(0.9999999 * this.minY, 1.0000001 * this.minY);
		maxY = Math.max(0.9999999 * this.maxY, 1.0000001 * this.maxY);
		//create tiles
		Tile t;
		for(double x = minX; x < maxX; x += inc){
			for(double y = minY; y < maxY; y += inc){
				t = new Tile (x, x + inc, y , y + inc);
				this.tiles.put(t.getCentroid().getX(), t.getCentroid().getY(), t);
			}
		}
		// add values
		for(Tuple<Coord, Double> v: this.values){
			// ass we used for the location within the quadtree the centroid of the tile,
			// this should be the tile containing the object
			t = this.tiles.getClosest(v.getFirst().getX(), v.getFirst().getY());
			// but anyway check it and warn the user if sth is wrong!
			Point p = new GeometryFactory().createPoint(new Coordinate(v.getFirst().getX(), v.getFirst().getY(), 0.));
			if(!t.getGeometry().contains(p)){
				//should never happen
				log.warn("check the created heatMap. A point will be added to a tile, but is not part of it!");
				log.warn(p.getCoordinate().toString());
				for(Coordinate c: t.getGeometry().getCoordinates()){
					System.out.print(c.toString() + "\t");
				}
				System.out.println();
			}
			t.add(v.getSecond());
		}
	}
	
	private Collection<Tile> getTiles() {
		return this.tiles.values();
	}

	public static void writeHeatMapShape(String name, HeatMap heatmap, String file, String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.setName(name);
		b.add("location", Polygon.class);
		b.add("name", String.class);
		b.add("count", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Object[] featureAttribs;
		int i = 0;
		for(Tile t: heatmap.getTiles()){
			featureAttribs = new Object[3];
			featureAttribs[0] = t.getGeometry();
			featureAttribs[1] = Integer.valueOf(i);
			featureAttribs[2] = t.getValue();
			try {
				features.add(builder.buildFeature(null, featureAttribs));
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
			i++;
		}
		if (heatmap.getTiles().isEmpty()) {
			log.info("There are no tiles for " + name);
		} else {
			try{
				ShapeFileWriter.writeGeometries(features, file);
			}catch(ServiceConfigurationError e){
				e.printStackTrace();
			}
		}
	}
}
