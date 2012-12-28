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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
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
	private final PolygonFeatureFactory.Builder factoryBuilder;

	public Network2PolygonGraph(Network network, CoordinateReferenceSystem crs) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = crs;
		features = new ArrayList<SimpleFeature>();
		
		this.factoryBuilder = new PolygonFeatureFactory.Builder().
				setCrs(this.crs).
				setName("links").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("capacity", Double.class).
				addAttribute("type", String.class).
				addAttribute("freespeed", Double.class).
				addAttribute("transMode", String.class);
	}

	@Override
	protected double getLinkWidth(Link link) {
		return link.getCapacity() / network.getCapacityPeriod() * 3600.0 / 50.0;
	}

	@Override
	public Collection<SimpleFeature> getFeatures() {
		for (int i = 0; i < attrTypes.size(); i++) {
			Tuple<String, Class<?>> att = attrTypes.get(i);
			factoryBuilder.addAttribute(att.getFirst(), att.getSecond());
		}
		PolygonFeatureFactory factory = factoryBuilder.create();

		for (Link link : (this.links2paint == null ? this.network.getLinks().values() : this.links2paint)) {
			LinearRing lr = getLinearRing(link);
			Polygon p = new Polygon(lr, null, this.geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);
			int size = 8 + parameters.size();
			Object[] o = new Object[size];
			o[0] = link.getId().toString();
			o[1] = link.getFromNode().getId().toString();
			o[2] = link.getToNode().getId().toString();
			o[3] = link.getLength();
			o[4] = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
			o[5] = (((LinkImpl) link).getType() != null) ? Integer
					.parseInt(((LinkImpl) link).getType()) : 0;
			o[6] = link.getFreespeed();
			o[7] = link.getAllowedModes() != null ? link.getAllowedModes().toString() : TransportMode.car;
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 8] = parameters.get(i).get(link.getId());
			}
			// parameters.get(link.getId().toString()) }
			SimpleFeature ft = factory.createPolygon(mp, o, null);
			features.add(ft);
		}
		return features;
	}

	public void setLinks2paint(Set<Link> links2paint) {
		this.links2paint = links2paint;
	}
}
