/* *********************************************************************** *
 * project: org.matsim.*
 * Toll2QGIS.java
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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author yu
 * 
 */
public class Toll2QGIS extends MATSimNet2QGIS {
	public Toll2QGIS(String netFilename, String coordRefSys) {
		super(netFilename, coordRefSys);
	}

	public static class Toll2PolygonGraph extends Network2PolygonGraph {
		private final RoadPricingScheme toll;
		private PolygonFeatureFactory.Builder factoryBuilder;

		public Toll2PolygonGraph(Network network,
				CoordinateReferenceSystem crs, RoadPricingScheme toll) {
			super(network, crs);
			this.toll = toll;

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
		public Collection<SimpleFeature> getFeatures() {
			for (int i = 0; i < attrTypes.size(); i++) {
				Tuple<String, Class<?>> att = attrTypes.get(i);
				factoryBuilder.addAttribute(att.getFirst(), att.getSecond());
			}
			PolygonFeatureFactory factory = factoryBuilder.create();

			for (Id linkId : toll.getTolledLinkIds()) {
				Link link = network.getLinks().get(linkId);
				// if (link != null) {
				LinearRing lr = getLinearRing(link);
				Polygon p = new Polygon(lr, null, geofac);
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, geofac);
				int size = 7 + parameters.size();
				Object[] o = new Object[size];
				o[0] = link.getId().toString();
				o[1] = link.getFromNode().getId().toString();
				o[2] = link.getToNode().getId().toString();
				o[3] = link.getLength();
				o[4] = link.getCapacity() / network.getCapacityPeriod()
						* 3600.0;
				o[5] = ((LinkImpl) link).getType() != null ? Integer
						.parseInt(((LinkImpl) link).getType()) : 0;
				o[6] = link.getFreespeed();
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 7] = parameters.get(i).get(link.getId());
				}
				// parameters.get(link.getId().toString()) }
				SimpleFeature ft = factory.createPolygon(mp, o, null);
				features.add(ft);
				// }
			}
			return features;
		}

	}

	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// String netFilename = "../matsimTests/scoringTest/network.xml";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		Toll2QGIS t2q = new Toll2QGIS(netFilename, ch1903);

		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(scheme);
		tollReader.parse(tollFilename);

		t2q.setN2g(new Toll2PolygonGraph(t2q.getNetwork(), t2q.crs, scheme));
		t2q.writeShapeFile("../matsimTests/toll/ivtch-osm_toll.shp");
	}
}
