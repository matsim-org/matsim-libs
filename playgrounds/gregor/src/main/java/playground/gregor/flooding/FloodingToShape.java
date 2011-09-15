/* *********************************************************************** *
 * project: org.matsim.*
 * FloodingToShape.java
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
package playground.gregor.flooding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.contrib.evacuation.flooding.FloodingInfo;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class FloodingToShape {
	
	private static final Logger log = Logger.getLogger(FloodingToShape.class);
	
	public static void main(String [] args) {
		List<List<FloodingInfo>> geos = new ArrayList<List<FloodingInfo>>();
		for (int i = 0; i < MY_STATIC_STUFF.SWW_COUNT; i++) {
			log.info("Reading netcdf file:" + i);
			String file = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + i + MY_STATIC_STUFF.SWW_SUFFIX;
			TriangularMeshSimplifier tr = new TriangularMeshSimplifier(file);
			geos.addAll(tr.getInundationGeometries());
		}
		ConcurrentLinkedQueue<List<FloodingInfo>> tris = new ConcurrentLinkedQueue<List<FloodingInfo>>(geos);
		geos.clear();
		
		GeometryFactory geofac = new GeometryFactory();
		FeatureType ft = createFeatureType();
		Collection<Feature> fts = new ArrayList<Feature>();
		while (tris.size() > 0) {
			List<FloodingInfo> tri = tris.poll();
			Coordinate [] coords = new Coordinate [4];
			double time = 0;
			double height = 0;
			FloodingInfo info = tri.get(0);
			double maxH = 0;
			for (double d : info.getFloodingSeries()) {
				if (maxH < d) {
					maxH = d;
				}
			}
			height += maxH;
			time += info.getFloodingTime();
			coords[0] = info.getCoordinate();
			
			info = tri.get(1);
			maxH = 0;
			for (double d : info.getFloodingSeries()) {
				if (maxH < d) {
					maxH = d;
				}
			}
			height += maxH;
			time += info.getFloodingTime();
			coords[1] = info.getCoordinate();
			
			info = tri.get(2);
			maxH = 0;
			for (double d : info.getFloodingSeries()) {
				if (maxH < d) {
					maxH = d;
				}
			}
			height += maxH;
			time += info.getFloodingTime();
			coords[2] = info.getCoordinate();
			
			coords[3] = coords[0];
			time /= 3;
			height /= 3;
			
			LinearRing lr = geofac.createLinearRing(coords);
			Polygon p = geofac.createPolygon(lr, null);
			
			try {
				fts.add(ft.create(new Object[]{p,time, height}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		
		ShapeFileWriter.writeGeometries(fts, "/home/laemmel/arbeit/diss/qgis/flooding.shp");
	}

	private static FeatureType createFeatureType() {
		
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"Polygon", Polygon.class, true, null, null,crs);
		AttributeType dblTime = AttributeTypeFactory.newAttributeType(
				"floodingTime", Double.class);
		AttributeType dblHeight = AttributeTypeFactory.newAttributeType(
				"floodingHeight", Double.class);		
		Exception ex;
		try {
			return  FeatureTypeFactory.newFeatureType(new AttributeType[] {
					geom, dblTime, dblHeight }, "FloodingTriangle");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}

}
