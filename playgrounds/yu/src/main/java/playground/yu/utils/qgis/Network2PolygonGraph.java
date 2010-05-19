/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkToGraph3.java
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

/**
 *
 */
package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author yu
 * 
 */
public class Network2PolygonGraph extends X2GraphImpl {
	protected Set<Link> links2paint = null;

	/**
	 * @param network
	 * @param coordinateReferenceSystem
	 */
	public Network2PolygonGraph(NetworkLayer network,
			CoordinateReferenceSystem crs) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = crs;
		features = new ArrayList<Feature>();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, this.crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID",
				String.class);
		AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", String.class);
		AttributeType toNode = AttributeTypeFactory.newAttributeType("toID",
				String.class);
		AttributeType length = AttributeTypeFactory.newAttributeType("length",
				Double.class);
		AttributeType cap = AttributeTypeFactory.newAttributeType("capacity",
				Double.class);
		AttributeType type = AttributeTypeFactory.newAttributeType("type",
				String.class);
		AttributeType freespeed = AttributeTypeFactory.newAttributeType(
				"freespeed", Double.class);
		AttributeType transMode = AttributeTypeFactory.newAttributeType(
				"transMode", String.class);
		defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
		defaultFeatureTypeFactory.setName("link");
		defaultFeatureTypeFactory.addTypes(new AttributeType[] { geom, id,
				fromNode, toNode, length, cap, type, freespeed, transMode });
	}

	@Override
	protected double getLinkWidth(Link link) {
		return link.getCapacity() / network.getCapacityPeriod() * 3600.0 / 50.0;
	}

	@Override
	public Collection<Feature> getFeatures() throws SchemaException,
			NumberFormatException, IllegalAttributeException {
		for (int i = 0; i < attrTypes.size(); i++)
			defaultFeatureTypeFactory.addType(attrTypes.get(i));
		FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
		for (Link link : (this.links2paint == null ? this.network.getLinks()
				.values() : this.links2paint)) {
			LinearRing lr = getLinearRing(link);
			Polygon p = new Polygon(lr, null, this.geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);
			int size = 9 + parameters.size();
			Object[] o = new Object[size];
			o[0] = mp;
			o[1] = link.getId().toString();
			o[2] = link.getFromNode().getId().toString();
			o[3] = link.getToNode().getId().toString();
			o[4] = link.getLength();
			o[5] = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
			o[6] = (((LinkImpl) link).getType() != null) ? Integer
					.parseInt(((LinkImpl) link).getType()) : 0;
			o[7] = link.getFreespeed();
			o[8] = link.getAllowedModes() != null ? link.getAllowedModes()
					.toString() : TransportMode.car.toString();
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 9] = parameters.get(i).get(link.getId());
			}
			// parameters.get(link.getId().toString()) }
			Feature ft = ftRoad.create(o, link.getId().toString());
			features.add(ft);
		}
		return features;
	}

	public void setLinks2paint(Set<Link> links2paint) {
		this.links2paint = links2paint;
	}
}
