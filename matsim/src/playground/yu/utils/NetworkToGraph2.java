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

package playground.yu.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.geotools.MGC;
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
public class NetworkToGraph2 {

	private NetworkLayer network;
	private CoordinateReferenceSystem crs;
	private GeometryFactory geofac;

	public NetworkToGraph2(NetworkLayer network,
			CoordinateReferenceSystem coordinateReferenceSystem) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = coordinateReferenceSystem;
	}

	public Collection<Feature> generateFromNet()
			throws FactoryRegistryException, SchemaException,
			IllegalAttributeException {

		Collection<Feature> features = new ArrayList<Feature>();

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
		DefaultFeatureTypeFactory dftf = new DefaultFeatureTypeFactory();
		dftf.setName("link");
		dftf.addTypes(new AttributeType[] { geom, id, fromNode, toNode, length,
				cap, type });
		FeatureType ftRoad = dftf.getFeatureType();

		for (Link link : this.network.getLinks().values()) {
			LineString ls = new LineString(
					new CoordinateArraySequence(
							new Coordinate[] {
									MGC.coord2Coordinate(link.getFromNode()
											.getCoord()),
									MGC.coord2Coordinate(link.getToNode()
											.getCoord()) }), this.geofac);

			Feature ft = ftRoad.create(new Object[] { ls,
					link.getId().toString(),
					link.getFromNode().getId().toString(),
					link.getToNode().getId().toString(), link.getLength(),
					link.getCapacity() / network.getCapacityPeriod() * 3600.0,
					Integer.parseInt(link.getType()) }, "network");
			features.add(ft);
		}
		return features;
	}
}
