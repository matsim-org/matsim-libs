/* *********************************************************************** *
 * project: org.matsim.*
 * Trip2PolygonGraph.java
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

/**
 * 
 */
package playground.yu.utils.qgis;

import java.util.Collection;

import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;

/**
 * trip/{@code Leg} can be painted as an arrow, route will not painted by this
 * class
 * 
 * @author yu
 * 
 */
public class Trip2PolygonGraph extends X2GraphImpl {

	@Override
	public Collection<Feature> getFeatures() throws SchemaException,
			NumberFormatException, IllegalAttributeException {
		for (int i = 0; i < attrTypes.size(); i++) {
			defaultFeatureTypeFactory.addType(attrTypes.get(i));
		}
		FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
//		for (Link link : this.links2paint == null ? this.network.getLinks()
//				.values() : this.links2paint) {
//			LinearRing lr = getLinearRing(link);
//			Polygon p = new Polygon(lr, null, this.geofac);
//			MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);
//			int size = 9 + parameters.size();
//			Object[] o = new Object[size];
//			o[0] = mp;
//			o[1] = link.getId().toString();
//			o[2] = link.getFromNode().getId().toString();
//			o[3] = link.getToNode().getId().toString();
//			o[4] = link.getLength();
//			o[5] = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
//			o[6] = ((LinkImpl) link).getType() != null ? Integer
//					.parseInt(((LinkImpl) link).getType()) : 0;
//			o[7] = link.getFreespeed();
//			o[8] = link.getAllowedModes() != null ? link.getAllowedModes().toString() : TransportMode.car;
//			for (int i = 0; i < parameters.size(); i++) {
//				o[i + 9] = parameters.get(i).get(link.getId());
//			}
//			// parameters.get(link.getId().toString()) }
//			Feature ft = ftRoad.create(o, link.getId().toString());
//			features.add(ft);
//		}
		return features;
	}

}
