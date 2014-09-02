/* *********************************************************************** *
 * project: org.matsim.*
 * Counts2QGIS.java
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
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * write a QGIS-.shp-file, in which only links with counts stations can be
 * written.
 * 
 * @author yu
 * 
 */
public class Counts2QGIS extends MATSimNet2QGIS {
	public Counts2QGIS(String netFilename, String coordRefSys) {
		super(netFilename, coordRefSys);
	}

	public static class Counts2PolygonGraph extends Network2PolygonGraph {
		private Set<Id<Link>> linkIds = null;
		private final PolygonFeatureFactory.Builder factoryBuilder;

		public Counts2PolygonGraph(final Network network,
				final CoordinateReferenceSystem crs, final Set<Id<Link>> linkIds) {
			super(network, crs);
			this.linkIds = linkIds;
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
		public Collection<SimpleFeature> getFeatures()  {
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
				int size = 6 + parameters.size();
				Object[] o = new Object[size];
				o[0] = link.getId().toString();
				o[1] = link.getFromNode().getId().toString();
				o[2] = link.getToNode().getId().toString();
				o[3] = link.getLength();
				o[4] = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
				o[5] = link.getFreespeed();
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 6] = parameters.get(i).get(link.getId());
				}
				// parameters.get(link.getId().toString()) }
				SimpleFeature ft = factory.createPolygon(mp, o, null);
				features.add(ft);
			}
			return features;
		}

		@Override
		protected double getLinkWidth(final Link link) {
			return super.getLinkWidth(link) * 2.0;
		}

	}

	protected Counts counts;

	protected Set<Id<Link>> readCounts(final String countsFilename) {
		counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);
		System.out.println("size :\t" + counts.getCounts().keySet().size());
		return counts.getCounts().keySet();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		String netFilename = "../berlin/network/bb_5_hermannstr.xml.gz";
		String countsFilename = "../berlin/counts/counts4bb_5_hermannstr_counts4Kantstr.xml";

		Counts2QGIS c2q = new Counts2QGIS(netFilename, gk4);
		c2q.setN2g(new Counts2PolygonGraph(c2q.getNetwork(), c2q.crs, c2q
				.readCounts(countsFilename)));
		c2q.writeShapeFile("../matsimTests/berlinQGIS/counts4bb_5_hermannstr_counts4Kantstr.shp");

		Gbl.printElapsedTime();
	}
}
