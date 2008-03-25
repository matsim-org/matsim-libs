/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkToGraph.java
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

package playground.gregor.shapeFileToMATSim;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class NetworkToGraph {

	private NetworkLayer network;
	private CoordinateReferenceSystem crs;
	private GeometryFactory geofac;

	public NetworkToGraph(NetworkLayer network, CoordinateReferenceSystem coordinateReferenceSystem) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = coordinateReferenceSystem;
	}

	public Collection<Feature> generateFromNet() throws FactoryRegistryException, SchemaException, IllegalAttributeException {

		Collection<Feature> features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.crs);
		AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", String.class);
		AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", String.class);
		AttributeType toNode = AttributeTypeFactory.newAttributeType(
				"toID", String.class);
		AttributeType length = AttributeTypeFactory.newAttributeType(
				"length", Double.class);
		AttributeType minWidth = AttributeTypeFactory.newAttributeType(
				"minWidth", Double.class);
		AttributeType effectiveWidth = AttributeTypeFactory.newAttributeType(
				"effWidth", Double.class);
		FeatureType ftRoad = FeatureTypeFactory.newFeatureType(
				new AttributeType[] { geom, id, fromNode, toNode, length, minWidth, effectiveWidth }, "link");


		for (Link link : this.network.getLinks().values()){
			LinearRing lr = getLinearRing(link);
			Polygon p = new Polygon(lr, null, this.geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] {p},this.geofac);
			LineString ls = new LineString(new CoordinateArraySequence(new Coordinate [] {MGC.coord2Coordinate(link.getFromNode().getCoord()),MGC.coord2Coordinate(link.getToNode().getCoord())}),this.geofac);

			Feature ft = ftRoad.create(new Object [] {mp , link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(),link.getLength(),"",""},"network");
			features.add(ft);
		}
		return features;
	}

	private LinearRing getLinearRing(Link link) {
		double minWidth = link.getCapacity() / GISToMatsimConverter.CAPACITY_COEF;
//		minWidth = 10;
		Coordinate zero = new Coordinate(0,0);
		Coordinate  from = new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()) ;

		Coordinate  to = new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY()) ;

		double xdiff = to.x - from.x;
		double ydiff = to.y - from.y;

		double distA = from.distance(zero);
		double distB = to.distance(zero);

		double ogradient = Double.MAX_VALUE;
		if (ydiff != 0)
			ogradient = -xdiff / ydiff;
		double csq = Math.pow(minWidth,2);
//		double csq = Math.pow(4,2);
		double xwidth = Math.sqrt(csq/(1+Math.pow(ogradient,2)));
		double ywidth = xwidth*ogradient;

		Coordinate fromB = new Coordinate(from.x+xwidth,from.y+ywidth,0);
		Coordinate toB = new Coordinate(to.x+xwidth,to.y+ywidth,0);

		CoordinateSequence coords = new CoordinateArraySequence(new Coordinate [] {from, to, toB, fromB, from});

		return new LinearRing(coords,this.geofac);



	}

}
