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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * this class is only a copy of <class>NetworkToGraph</class> Gregor Laemmels
 *
 * @author ychen
 */
public class Network2LinkGraph extends X2GraphImpl implements X2Graph {

	private final PolylineFeatureFactory.Builder factoryBuilder;
	
	public Network2LinkGraph(NetworkImpl network,
			CoordinateReferenceSystem coordinateReferenceSystem) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = coordinateReferenceSystem;
		features = new ArrayList<SimpleFeature>();
		
		this.factoryBuilder = new PolylineFeatureFactory.Builder().
				setCrs(this.crs).
				setName("links").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("capacity", Double.class).
				addAttribute("freespeed", Double.class);
	}

	public Collection<SimpleFeature> getFeatures() {
		for (int i = 0; i < attrTypes.size(); i++) {
			Tuple<String, Class<?>> att = attrTypes.get(i);
			factoryBuilder.addAttribute(att.getFirst(), att.getSecond());
		}
		PolylineFeatureFactory factory = factoryBuilder.create();
		for (Link link : this.network.getLinks().values()) {
			Coordinate[] coords = new Coordinate[] {
					MGC.coord2Coordinate(link.getFromNode().getCoord()),
					MGC.coord2Coordinate(link.getToNode().getCoord())
				};
			int size = 6 + parameters.size();
			Object[] o = new Object[size];
			o[0] = link.getId().toString();
			o[1] = link.getFromNode().getId().toString();
			o[2] = link.getToNode().getId().toString();
			o[3] = link.getLength();
			o[4] = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
			o[5] = link.getFreespeed();
			for (int i = 0; i < parameters.size(); i++) {
				o[i + 6] = parameters.get(i).get(link.getId().toString());
			}
			SimpleFeature ft = factory.createPolyline(coords, o, null);
			features.add(ft);
		}
		return features;
	}
}
