/* *********************************************************************** *
 * project: org.matsim.*
 * Volume2PolygonGraph.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
public class Volume2PolygonGraph extends Network2PolygonGraph {
	private Set<Id> linkIds;

	public Volume2PolygonGraph(NetworkLayer network,
			CoordinateReferenceSystem crs, Set<Id> linkIds) {
		super(network, crs);
		this.linkIds = linkIds;
		this.geofac = new GeometryFactory();
		features = new ArrayList<Feature>();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID",
				String.class);
		defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
		defaultFeatureTypeFactory.setName("link");
		defaultFeatureTypeFactory.addTypes(new AttributeType[] { geom, id });
	}

	@Override
	protected double getLinkWidth(Link link) {
		Integer i = (Integer) parameters.get(0).get(link.getId());
		return (i.intValue()) / 20.0;
	}

	@Override
	public Collection<Feature> getFeatures() throws SchemaException,
			NumberFormatException, IllegalAttributeException {
		for (int i = 0; i < attrTypes.size(); i++)
			defaultFeatureTypeFactory.addType(attrTypes.get(i));
		FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
		for (Id linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			LinearRing lr = getLinearRing(link);
			Polygon p = new Polygon(lr, null, this.geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);
			int size = 2 + parameters.size();
			Object[] o = new Object[size];
			o[0] = mp;
			o[1] = link.getId().toString();
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 2] = parameters.get(i).get(link.getId());
			}
			Feature ft = ftRoad.create(o, "network");
			features.add(ft);
		}
		return features;
	}

}
