/* *********************************************************************** *
 * project: org.matsim.*
 * PadangPrimActGeneration.java
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

package playground.gregor.demandmodeling;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.router.PlansCalcRouteDijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.StringUtils;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PadangPrimActGeneration {

	private static double BETA = 0.0013;

	private final NetworkLayer network;
	private final String zonesFilename;
	private final String demandFilename;
	private final String districts;
	private QuadTree<Link> linksTree;

	private int sumOpportunities;

	private long inhabitants = 0;

	private final GeometryFactory geofac;

	private int id = 0;

	private static final Logger log = Logger.getLogger(PadangPrimActGeneration.class);
	
	public PadangPrimActGeneration(final String zonesFile, final String demandFilename, final String districts) {
		this.network = (NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		this.zonesFilename = zonesFile;
		this.demandFilename = demandFilename;
		this.districts = districts;
		this.geofac = new GeometryFactory();
		init();
	}

	public void init() {

		this.linksTree = new QuadTree<Link>(646000,9800000,674000,9999000);
		for (final Link link : this.network.getLinks().values()) {
			this.linksTree.put(link.getCenter().getX(), link.getCenter().getY(), link);
		}

	}

	public void run() {
		HashMap<String,Feature> ftDist = getFeatures(this.districts);
		ArrayList<Zone> zones = getZones();
		Population pop = getPopulation(ftDist,zones);
		PlansCalcRouteDijkstra router = new PlansCalcRouteDijkstra(this.network,new FreespeedTravelTimeCost(),new FreespeedTravelTimeCost());
		router.run(pop);
		new PopulationWriter(pop,this.demandFilename, "v4").write();
	}


	private ArrayList<Person> getPersons(final Feature zone) {
		
		ArrayList<Person> ret = new ArrayList<Person>();
		
		final Polygon p = (Polygon) zone.getDefaultGeometry().getGeometryN(0);
		final Envelope e = zone.getBounds();
		final long inhabitants = (Long)zone.getAttribute(6);
		this.inhabitants  += inhabitants;
		Collection<Link> links = new ArrayList<Link>();
		this.linksTree.get(e.getMinX()-300, e.getMinY()-300,e.getMaxX()+300, e.getMaxY()+300,links);
		if (links.size() == 0) {
			log.warn("no link found!");
		}
		
		final ArrayList<Link> tmp = new ArrayList<Link>();
		for (final Link link : links) {
			final Point point = this.geofac.createPoint(new Coordinate(link.getCenter().getX(),link.getCenter().getY()));
			if (p.contains(point)) {
				tmp.add(link);
			}
		}
		
		if (tmp.size() == 0) {
			for (final Link link : links) {
				LineString ls = this.geofac.createLineString(new Coordinate[] {new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY()),
						new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY())});
				
				if (ls.touches(p)) {
					tmp.add(link);
				} 					
				
			}			
		}
		
		if (tmp.size() == 0) {
			for (final Link link : links) {
				tmp.add(link);
			}
		}
		
//		if (tmp.size() == 0) {
//			log.warn("could not find any link for zone: " + zone.getAttribute(1) + "just taking the nearest link to centroid!");
//		}
//		
		Coord c = MGC.point2Coord(p.getCentroid());
		tmp.add(this.network.getNearestLink(c));
		
		links = tmp;
		final double overalllength = getOALength(links);
		int all = 0;
		for (final Link link : links) {
			final double fraction = link.getLength() / overalllength;
			final int li = (int) Math.round(inhabitants * fraction);
			all += li;
			for (int i = 0; i < li ; i++) {
				final Person pers = new Person(new IdImpl(this.id++));
				final Plan plan = new Plan(pers);
				final Act act = new Act("h",link.getCenter().getX(),link.getCenter().getY(),link,Time.MIDNIGHT,6*3600,Time.UNDEFINED_TIME,false);
				plan.addAct(act);
				pers.addPlan(plan);
				try {
					ret.add(pers);
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
				
			}
		}
		return ret;
	}

	private double getOALength(final Collection<Link> links) {
		double l = 0;
		for (final Link link : links) {
			l += link.getLength();
		}
		return l;
	}
	
	private Population getPopulation(final HashMap<String, Feature> ftDist,
			final ArrayList<Zone> zones) {
		
	
		
		Population population = new Population();
		int count = 0;
		for (Entry<String, Feature> e : ftDist.entrySet()) {
			Feature ft = e.getValue();
			Long inhabitants = (Long) ft.getAttribute(6);
			ArrayList<Person> persons = getPersons(ft);
			
			for (Person pers : persons) {
				
				Zone primActZone = null;
				while(true) {
					// draw random zone based on numOpportunities
					int r = MatsimRandom.random.nextInt(this.sumOpportunities);
					int tmpSum = 0;
					Zone tmpZone = null;
					for (Zone zone : zones) {
						tmpSum += zone.numOpportunities;
						if (r <= tmpSum) {
							tmpZone = zone;
							break;
						}
					}
					
					double distance = tmpZone.coord.calcDistance(pers.getRandomPlan().getFirstActivity().getCoord());
					double p = Math.exp(-BETA * distance);
					if (p >= MatsimRandom.random.nextDouble()) {
						primActZone = tmpZone;
						break;
					}					
					
				}
				Feature ftW = ftDist.get(primActZone.zoneId);
				if (ftW == null) {
//					System.out.println(primActZone.zoneId);
					continue;
				}
				
				Link link = getRandomLinkWithin(ftW);
				Leg leg = new Leg(BasicLeg.Mode.car);
				leg.setNum(0);
				leg.setDepTime(Time.UNDEFINED_TIME);
				leg.setTravTime(Time.UNDEFINED_TIME);
				leg.setArrTime(Time.UNDEFINED_TIME);
				Act act = new Act("w",link.getCenter().getX(),link.getCenter().getY(),link,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME,true);
				pers.getSelectedPlan().addLeg(leg);
				pers.getSelectedPlan().addAct(act);
				try {
					population.addPerson(pers);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				count++;
				if (count % 1000 == 0) {
					System.out.println(count);
				}
			}
		}
		System.out.println("inhabitants" + count);
		return population;
	}

	private Link getRandomLinkWithin(final Feature ft) {

		Envelope e = ft.getBounds();
		double maxShift = new CoordImpl(e.getMinX(),e.getMinY()).calcDistance(new CoordImpl(e.getMaxX(),e.getMaxY()));
		Coordinate centroid = ft.getDefaultGeometry().getCentroid().getCoordinate();
		int count = 0;
		while (count < 100) {
			Coordinate rnd = new Coordinate(centroid.x + MatsimRandom.random.nextDouble() * maxShift, centroid.y + MatsimRandom.random.nextDouble() * maxShift);
			Point p = this.geofac.createPoint(rnd);
			try {
				if (!p.disjoint(ft.getDefaultGeometry())) {
					return this.network.getNearestLink(MGC.point2Coord(p));
				}
			} catch (Exception e1) {

			}
			count++;
			
		}
//		log.warn("somthing went wrong with the geotools - just taking the link next to the centroid");
		Coord rnd = new CoordImpl(centroid.x + MatsimRandom.random.nextDouble() * maxShift, centroid.y + MatsimRandom.random.nextDouble() * maxShift);
		return this.network.getNearestLink(rnd);

	}

	private ArrayList<Zone> getZones() {

		ArrayList<Zone> ret = new ArrayList<Zone>(20);
		this.sumOpportunities = 0;

		// read zones
		try {
			final BufferedReader zonesReader = IOUtils.getBufferedReader(this.zonesFilename);
			String header = zonesReader.readLine();
			String line = zonesReader.readLine();
			while (line != null) {
				String[] parts = StringUtils.explode(line, ';');
				int numOpportunities = Integer.parseInt(parts[3]);
				Zone zone = new Zone(parts[0], new CoordImpl(parts[1], parts[2]), numOpportunities);
				ret.add(zone);
				this.sumOpportunities += numOpportunities;
				// --------
				line = zonesReader.readLine();
			}
			zonesReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return ret;
	}

	private HashMap<String, Feature> getFeatures(final String districts) {

		HashMap<String,Feature> ret = new HashMap<String, Feature>();
		FeatureSource fts = null;
		try {
			fts = ShapeFileReader.readDataFile(districts);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			for (Object ft : fts.getFeatures()) {
				String id = ((Long) ((Feature)ft).getAttribute(1)).toString();
				ret.put(id,(Feature)ft);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

	public static void main(final String [] args) throws IOException {
		final String zonesFilename = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/demand_generation/USABLE DATA/Marcel/Zones.csv";

		//		final String plansFilename = "./networks/padang_plans_v2000618.xml.gz";
		final String demandFilename = "./networks/padang_plans_v20080618_demand.xml.gz";
		final String networkFilename = "./networks/padang_net_v20080618.xml";
		final String districts = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/data/GIS/keluraha_population.shp";



		Gbl.createConfig(null);
		Gbl.createWorld();
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);
		Gbl.getWorld().setNetworkLayer(network);		



		new PadangPrimActGeneration(zonesFilename,demandFilename,districts).run();


		
	}




	private static class Zone {
		public final String zoneId;
		public final Coord coord;
		public final int numOpportunities;

		public Zone(final String zoneId, final Coord coord, final int numOpportunities) {
			this.zoneId = zoneId;
			this.coord = coord;
			this.numOpportunities = numOpportunities;
		}
	}

}
