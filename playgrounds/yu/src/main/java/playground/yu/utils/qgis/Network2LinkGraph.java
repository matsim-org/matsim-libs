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

package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * this class is only a copy of <class>NetworkToGraph</class> Gregor Laemmels
 *
 * @author ychen
 */
public class Network2LinkGraph extends X2GraphImpl implements X2Graph {

	public Network2LinkGraph(NetworkLayer network,
			CoordinateReferenceSystem coordinateReferenceSystem) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = coordinateReferenceSystem;
		features = new ArrayList<Feature>();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, this.crs);
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
				Integer.class);
		AttributeType freespeed = AttributeTypeFactory.newAttributeType(
				"freespeed", Double.class);
		defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
		defaultFeatureTypeFactory.setName("link");
		defaultFeatureTypeFactory.addTypes(new AttributeType[] { geom, id,
				fromNode, toNode, length, cap, type, freespeed });
	}

	// ////////////////////////////////////////////
	/**
	 * @return the features
	 * @throws SchemaException
	 * @throws IllegalAttributeException
	 * @throws NumberFormatException
	 */
	public Collection<Feature> getFeatures() throws SchemaException,
			NumberFormatException, IllegalAttributeException {
		for (int i = 0; i < attrTypes.size(); i++)
			defaultFeatureTypeFactory.addType(attrTypes.get(i));
		FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
		for (LinkImpl link : this.network.getLinks().values()) {
			LineString ls = new LineString(
					new CoordinateArraySequence(
							new Coordinate[] {
									MGC.coord2Coordinate(link.getFromNode()
											.getCoord()),
									MGC.coord2Coordinate(link.getToNode()
											.getCoord()) }), this.geofac);
			int size = 8 + parameters.size();
			Object[] o = new Object[size];
			o[0] = ls;
			o[1] = link.getId().toString();
			o[2] = link.getFromNode().getId().toString();
			o[3] = link.getToNode().getId().toString();
			o[4] = link.getLength();
			o[5] = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
			o[6] = Integer.parseInt(link.getType());
			o[7] = link.getFreespeed();
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 8] = parameters.get(i).get(link.getId().toString());
			}
			// parameters.get(link.getId().toString()) }
			Feature ft = ftRoad.create(o, "network");
			features.add(ft);
		}
		return features;
	}
}
