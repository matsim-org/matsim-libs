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
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.PlansWriterHandlerImplV4;
import org.matsim.utils.collections.QuadTree;

import playground.gregor.gis.networkProcessing.NetworkGenerator;
import playground.gregor.gis.utils.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class StaticPopulationGenerator {
	private static final Logger log = Logger.getLogger(StaticPopulationGenerator.class);
	private NetworkLayer network;
	private Collection<Feature> zones;
	private QuadTree<Link> linksTree;
	private GeometryFactory geofac;
	
	public StaticPopulationGenerator(NetworkLayer network,
			Collection<Feature> zones) {
		this.network = network;
		this.zones = zones;
		this.geofac  = new GeometryFactory();
		this.linksTree = new QuadTree<Link>(646000,9800000,674000,9999000);
		for (Link link : network.getLinks().values()) {
			this.linksTree.put(link.getCenter().getX(), link.getCenter().getY(), link);
		}
	}

	public void createPopulation() {
		int id = 0;
		Plans population = new Plans();
		for (Feature zone : zones) {
			Polygon p = (Polygon) zone.getDefaultGeometry().getGeometryN(0);
			Envelope e = zone.getBounds();
			long inhabitants = (Long)zone.getAttribute(7);
			Collection<Link> links = new ArrayList<Link>();
			this.linksTree.get(e.getMinX(), e.getMinY(),e.getMaxX(), e.getMaxY(),links);
			ArrayList<Link> tmp = new ArrayList<Link>();
			for (Link link : links) {
				Point point = this.geofac.createPoint(new Coordinate(link.getCenter().getX(),link.getCenter().getY()));
				if (p.contains(point)) {
					tmp.add(link);
				}
			}
			if (tmp.size() == 0) {
				continue;
			}
			if (tmp.size() >= links.size() ) {
				log.error("something went wrong!!");
			}
			links = tmp;
			double overalllength = getOALength(links);
			int all = 0;
			for (Link link : links) {
				double fraction = link.getLength() / overalllength;
				int li = (int) Math.round(inhabitants * fraction);
				all += li;
				for (int i = 0; i < li ; i++) {
					Person pers = new Person(new IdImpl(id++));
					Plan plan = new Plan(pers);
					Act act = new Act("h",link.getCenter().getX(),link.getCenter().getY(),link,3 * 3600.0,3 *3600.0,0,true);
					plan.addAct(act);
					pers.addPlan(plan);
					try {
						population.addPerson(pers);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
				}
			}
			System.out.println("Diff: " +  (inhabitants - all));
			
		}
		
		new PlansWriter(population,"pagan_plans_v20080608.xml.gz", "v4").write();
		
	}
	
	private double getOALength(Collection<Link> links) {
		double l = 0;
		for (Link link : links) {
			l += link.getLength();
		}
		return l;
	}

	public static void main(String [] args) {
		String netFile = "./networks/padang_net_v20080608.xml";
		String zonesFile = "./padang/zones.shp";
		
		Gbl.createWorld();
		Gbl.createConfig(null);
		Collection<Feature> zones = null;
		try {
			zones=  getPolygons(ShapeFileReader.readDataFile(zonesFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFile);
		
		new StaticPopulationGenerator(network,zones).createPopulation();
	}
	
	private static Collection<Feature> getPolygons(FeatureSource n) {
		Collection<Feature> polygons = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			Feature feature = it.next();
			int id = (Integer) feature.getAttribute(1);
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
				continue;
			}
			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			polygons.add(feature);
	}
	
		return polygons;
	}
	
}
