/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayer.java
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
package playground.johannes.socialnetworks.spatial;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @author illenberger
 *
 */
public class GeometryLayer {

	private Quadtree quadtree;
	
	private GeometryFactory factory;
	
	public GeometryLayer(Set<Geometry> geometries) {
		factory = new GeometryFactory();
		quadtree = new Quadtree();
		
		for(Geometry geometry : geometries) {
			quadtree.insert(geometry.getEnvelopeInternal(), geometry);
		}
	}
	
	public Geometry getZone(Coord coord) {
		Coordinate c = new Coordinate(coord.getX(), coord.getY());
		Envelope env = new Envelope(c);
		List<Geometry> indices = quadtree.query(env);
		
		for(Geometry geometry : indices) {
			if(geometry.contains(factory.createPoint(c))) {
				return geometry;
			}
		}
		
		return null;
	}

	public List<Geometry> getZones() {
		return quadtree.queryAll();
	}
	
	public static GeometryLayer creatFromShapeFile(String filename) throws IOException {
		FeatureSource source = ShapeFileReader.readDataFile(filename);
	
		Set<Geometry> geometries = new HashSet<Geometry>();
		
		Iterator<Feature> it = source.getFeatures().iterator();
		while(it.hasNext()) {
			Feature feature = it.next();
			geometries.add(feature.getDefaultGeometry());
		}
		
		GeometryLayer zoneLayer = new GeometryLayer(geometries);
		
		return zoneLayer;
	}

}
