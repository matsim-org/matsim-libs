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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
public class Volume2PolygonGraph extends Network2PolygonGraph {
	private Set<Id<Link>> linkIds;
	private PolygonFeatureFactory.Builder factoryBuilder;

	public Volume2PolygonGraph(Network network, CoordinateReferenceSystem crs,
			Set<Id<Link>> linkIds) {
		super(network, crs);
		this.linkIds = linkIds;
		geofac = new GeometryFactory();
		features = new ArrayList<SimpleFeature>();
		

		this.factoryBuilder = new PolygonFeatureFactory.Builder().
				setCrs(crs).
				setName("links").
				addAttribute("ID", String.class);
	}

	@Override
	protected double getLinkWidth(Link link) {
		Integer i = (Integer) parameters.get(0).get(link.getId());
		if (i == null) {
			return 0d;
		}
		return i.intValue() / 20.0;
	}

	@Override
	public Collection<SimpleFeature> getFeatures() {
		for (int i = 0; i < attrTypes.size(); i++) {
			Tuple<String, Class<?>> att = attrTypes.get(i);
			factoryBuilder.addAttribute(att.getFirst(), att.getSecond());
		}
		PolygonFeatureFactory factory = factoryBuilder.create();
		for (Id linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			LinearRing lr = getLinearRing(link);
			Polygon p = new Polygon(lr, null, geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, geofac);
			int size = 2 + parameters.size();
			Object[] o = new Object[size];
			o[0] = link.getId().toString();
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 1] = parameters.get(i).get(link.getId());
			}
			SimpleFeature ft = factory.createPolygon(mp, o, null);
			features.add(ft);
		}
		return features;
	}

}
