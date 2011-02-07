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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PrimaryLocationDrawing {

	private static double BETA = 0.0013;

	private final NetworkImpl network;
	private final String zonesFilename;
	private final String demandFilename;
	private final String districts;
	private QuadTree<Link> linksTree;

	private int sumOpportunities;

	private long inhabitants = 0;

	private final GeometryFactory geofac;

	private int id = 0;

	private static final Logger log = Logger.getLogger(PrimaryLocationDrawing.class);

	public PrimaryLocationDrawing(final NetworkImpl network, final String zonesFile, final String demandFilename, final String districts) {
		this.network = network;
		this.zonesFilename = zonesFile;
		this.demandFilename = demandFilename;
		this.districts = districts;
		this.geofac = new GeometryFactory();
		init();
	}

	public void init() {

		this.linksTree = new QuadTree<Link>(646000,9800000,674000,9999000);
		for (final Link link : this.network.getLinks().values()) {
			this.linksTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
		}

	}

	public void run(Config config) {
		HashMap<String,Feature> ftDist = getFeatures(this.districts);
		ArrayList<Zone> zones = getZones();
		Population pop = getPopulation(ftDist,zones);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.planCalcScore());
		PlansCalcRoute router = new PlansCalcRoute(config.plansCalcRoute(), this.network,timeCostCalc, timeCostCalc, new DijkstraFactory());
		router.run(pop);
		new PopulationWriter(pop, this.network).write(this.demandFilename);
	}


	private ArrayList<PersonImpl> getPersons(final Feature zone) {

		ArrayList<PersonImpl> ret = new ArrayList<PersonImpl>();

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
			final Point point = this.geofac.createPoint(new Coordinate(link.getCoord().getX(),link.getCoord().getY()));
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
				final PersonImpl pers = new PersonImpl(new IdImpl(this.id++));
				final PlanImpl plan = new org.matsim.core.population.PlanImpl(pers);
				final ActivityImpl act = new org.matsim.core.population.ActivityImpl("h",link.getCoord(),link.getId());
				act.setEndTime(6*3600);
				plan.addActivity(act);
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



		Population population = new ScenarioImpl().getPopulation();
		int count = 0;
		for (Entry<String, Feature> e : ftDist.entrySet()) {
			Feature ft = e.getValue();
			Long inhabitants = (Long) ft.getAttribute(6);
			ArrayList<PersonImpl> persons = getPersons(ft);

			for (PersonImpl pers : persons) {

				Zone primActZone = null;
				while(true) {
					// draw random zone based on numOpportunities
					int r = MatsimRandom.getRandom().nextInt(this.sumOpportunities);
					int tmpSum = 0;
					Zone tmpZone = null;
					for (Zone zone : zones) {
						tmpSum += zone.numOpportunities;
						if (r <= tmpSum) {
							tmpZone = zone;
							break;
						}
					}

					double distance = CoordUtils.calcDistance(tmpZone.coord, ((PlanImpl) pers.getRandomPlan()).getFirstActivity().getCoord());
					double p = Math.exp(-BETA * distance);
					if (p >= MatsimRandom.getRandom().nextDouble()) {
						primActZone = tmpZone;
						break;
					}

				}
				Feature ftW = ftDist.get(primActZone.zoneId);
				if (ftW == null) {
//					System.out.println(primActZone.zoneId);
					continue;
				}

				LinkImpl link = getRandomLinkWithin(ftW);
//				Leg leg = new org.matsim.population.LegImpl(0,"car",Time.UNDEFINED_TIME,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME);
				LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
				leg.setArrivalTime(Time.UNDEFINED_TIME);
				leg.setDepartureTime(Time.UNDEFINED_TIME);
				leg.setTravelTime(Time.UNDEFINED_TIME);
				ActivityImpl act = new org.matsim.core.population.ActivityImpl("w",link.getCoord(),link.getId());
				pers.getSelectedPlan().addLeg(leg);
				pers.getSelectedPlan().addActivity(act);
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

	private LinkImpl getRandomLinkWithin(final Feature ft) {

		Envelope e = ft.getBounds();
		double maxShift = CoordUtils.calcDistance(new CoordImpl(e.getMinX(),e.getMinY()), new CoordImpl(e.getMaxX(),e.getMaxY()));
		Coordinate centroid = ft.getDefaultGeometry().getCentroid().getCoordinate();
		int count = 0;
		while (count < 100) {
			Coordinate rnd = new Coordinate(centroid.x + MatsimRandom.getRandom().nextDouble() * maxShift, centroid.y + MatsimRandom.getRandom().nextDouble() * maxShift);
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
		Coord rnd = new CoordImpl(centroid.x + MatsimRandom.getRandom().nextDouble() * maxShift, centroid.y + MatsimRandom.getRandom().nextDouble() * maxShift);
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



		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		new PrimaryLocationDrawing(network,zonesFilename,demandFilename,districts).run(scenario.getConfig());
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
