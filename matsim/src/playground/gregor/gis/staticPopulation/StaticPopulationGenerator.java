/* *********************************************************************** *
 * project: org.matsim.*
 * StaticPopulationGenerator.java
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

package playground.gregor.gis.staticPopulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.PersonImpl;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class StaticPopulationGenerator {
	private static final Logger log = Logger.getLogger(StaticPopulationGenerator.class);
	private final NetworkLayer network;
	private final Collection<Feature> zones;
	private final QuadTree<Link> linksTree;
	private final GeometryFactory geofac;
	
	public StaticPopulationGenerator(final NetworkLayer network,
			final Collection<Feature> zones) {
		this.network = network;
		this.zones = zones;
		this.geofac  = new GeometryFactory();
		this.linksTree = new QuadTree<Link>(646000,9800000,674000,9999000);
		for (final Link link : network.getLinks().values()) {
			this.linksTree.put(link.getCenter().getX(), link.getCenter().getY(), link);
		}
	}

	public void createPopulation() {
		int id = 0;
		final Population population = new Population();
		int inhabitants_all = 0;
		int lost = 0;
		for (final Feature zone : this.zones) {
			final Polygon p = (Polygon) zone.getDefaultGeometry().getGeometryN(0);
			final Envelope e = zone.getBounds();
			final long inhabitants = (Long)zone.getAttribute(7);
			inhabitants_all += inhabitants;
			Collection<Link> links = new ArrayList<Link>();
			this.linksTree.get(e.getMinX(), e.getMinY(),e.getMaxX(), e.getMaxY(),links);
			final ArrayList<Link> tmp = new ArrayList<Link>();
			for (final Link link : links) {
				final Point point = this.geofac.createPoint(new Coordinate(link.getCenter().getX(),link.getCenter().getY()));
				if (p.contains(point)) {
					tmp.add(link);
				}
			}
			if (tmp.size() == 0) {
				lost += inhabitants;
				continue;
			}
			if (tmp.size() >= links.size() ) {
				log.error("something went wrong!!");
			}
			links = tmp;
			final double overalllength = getOALength(links);
			int all = 0;
			for (final Link link : links) {
				final double fraction = link.getLength() / overalllength;
				final int li = (int) Math.round(inhabitants * fraction);
				all += li;
				for (int i = 0; i < li ; i++) {
					final Person pers = new PersonImpl(new IdImpl(id++));
					final Plan plan = new org.matsim.population.PlanImpl(pers);
					final Act act = new org.matsim.population.ActImpl("h",link.getCenter(),link);
					act.setStartTime(3 * 3600.0); 
					// (I still think it would make more sense to leave the starting time of the first activity undefined. kai)
					act.setEndTime(3 *3600.0);
					act.setDuration(0);
					plan.addAct(act);
					pers.addPlan(plan);
					try {
						population.addPerson(pers);
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
					
				}
			}
			System.out.println("Diff: " +  (inhabitants - all));
			
		}
		System.err.println("inh:" + inhabitants_all + " agents:" + id + " lost:" + lost);
		new PopulationWriter(population,"padang_plans_v200800820.xml.gz", "v4").write();
		
	}
	
	private double getOALength(final Collection<Link> links) {
		double l = 0;
		for (final Link link : links) {
			l += link.getLength();
		}
		return l;
	}

	public static void main(final String [] args) {
		final String netFile = "./networks/padang_net_v20080618.xml";
		final String zonesFile = "./padang/podes.shp";
		
		Gbl.createWorld();
		Gbl.createConfig(null);
		Collection<Feature> zones = null;
		try {
			zones=  getPolygons(ShapeFileReader.readDataFile(zonesFile));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFile);
		
		new StaticPopulationGenerator(network,zones).createPopulation();
	}
	
	private static Collection<Feature> getPolygons(final FeatureSource n) {
		final Collection<Feature> polygons = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
			final int id = (Integer) feature.getAttribute(1);
			final MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
			}
			final Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			polygons.add(feature);
	}
	
		return polygons;
	}
	
}
