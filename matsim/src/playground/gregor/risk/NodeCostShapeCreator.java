/* *********************************************************************** *
 * project: org.matsim.*
 * NodeCostShapeCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.risk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class NodeCostShapeCreator {

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;
	private static final double DIST = 5.0;
	
	private FeatureType ft;
	private final CoordinateReferenceSystem targetCRS;
	private Collection<Feature> features;
	private final Map<Link, Double> links;
	private FeatureType ftPoint;
	private Collection<Feature> pointFeatures;

	public NodeCostShapeCreator(final Map<Link, Double> map,final CoordinateReferenceSystem crs) {
		this.targetCRS = crs;
		this.links = map;
		initFeatures();
		createFeatures();
		writeFeatures();
	}

	private void writeFeatures() {
		try {
			ShapeFileWriter.writeGeometries(this.features, "tmp/linkCost.shp");
			ShapeFileWriter.writeGeometries(this.pointFeatures, "tmp/linkCostDirection.shp");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	private void createFeatures() {
		GeometryFactory geofac = new GeometryFactory();
		this.features = new ArrayList<Feature>();
		this.pointFeatures = new ArrayList<Feature>();
		for (Entry<Link, Double> e : this.links.entrySet()) {
			Link l = e.getKey();
			Coordinate[] coords = {MGC.coord2Coordinate(l.getFromNode().getCoord()),MGC.coord2Coordinate(l.getToNode().getCoord())};
			LineString ls = geofac.createLineString(coords);
			
			Coordinate c = new Coordinate((l.getFromNode().getCoord().getX()+l.getToNode().getCoord().getX())/2,(l.getFromNode().getCoord().getY()+l.getToNode().getCoord().getY())/2);
			
			Coordinate from = MGC.coord2Coordinate(l.getFromNode().getCoord());
			Coordinate to = MGC.coord2Coordinate(l.getToNode().getCoord());

			final double dx = -from.x   + to.x;
			final double dy = -from.y   + to.y;		

			double theta = 0.0;
			if (dx > 0) {
				theta = Math.atan(dy/dx);
			} else if (dx < 0) {
				theta = Math.PI + Math.atan(dy/dx);
			} else { // i.e. DX==0
				if (dy > 0) {
					theta = PI_HALF;
				} else {
					theta = -PI_HALF;
				}
			}
			if (theta < 0.0) theta += TWO_PI;
			
			
			double centerX = c.x  + Math.sin(theta) * DIST;
			double centerY = c.y  - Math.cos(theta) * DIST;
			
			Point p = geofac.createPoint(new Coordinate(centerX,centerY));
			double deg = 90 - Math.toDegrees(theta);
//			double deg = 90;
			try {
				Feature ft = this.ft.create(new Object[] {ls,e.getValue(),l.getFromNode().getId().toString(),l.getToNode().getId().toString(),l.getId().toString()});
				Feature ftP = this.ftPoint.create(new Object[] {p,e.getValue(),l.getFromNode().getId().toString(),l.getToNode().getId().toString(),l.getId().toString(),deg});
				this.features.add(ft);
				this.pointFeatures.add(ftP);
			} catch (IllegalAttributeException e1) {
				throw new RuntimeException(e1);
			}
			
		}
		
	}

	private void initFeatures() {
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.targetCRS);
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.targetCRS);
		AttributeType dblCost = AttributeTypeFactory.newAttributeType("dblCost", Double.class);
		AttributeType dblAngle = AttributeTypeFactory.newAttributeType("dblAngle", Double.class);
		AttributeType strFrom = AttributeTypeFactory.newAttributeType("strFrom", String.class);
		AttributeType strTo = AttributeTypeFactory.newAttributeType("strTo", String.class);
		AttributeType strId = AttributeTypeFactory.newAttributeType("strId", String.class);		
		
		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, dblCost, strFrom, strTo, strId}, "NodeCost");
			this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {point, dblCost, strFrom, strTo, strId, dblAngle}, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
		
	}
}

