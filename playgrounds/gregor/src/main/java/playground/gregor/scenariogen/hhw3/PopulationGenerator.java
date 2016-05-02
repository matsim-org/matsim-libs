/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.scenariogen.hhw3;

import java.util.Collection;
import java.util.LinkedList;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class PopulationGenerator {


//	private final static Id tt0 = Id.create("sim2d_0_rev_-941555");
//	private final static Id tt1 = Id.create("sim2d_0_-941546");
	private final static double CUTOFF_DIST = 1000;
	
	static Polygon t1;
	static {
		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(1113948.78,7041312.03);
		Coordinate c1 = new Coordinate(1113993.25,7041575.30);
		Coordinate c3 = new Coordinate(1114088.36,7041568.01);
		Coordinate c2 = new Coordinate(1114061.1,7041280.1);
		Coordinate c4 = new Coordinate(1113948.78,7041312.03);
		LinearRing lr = geofac.createLinearRing(new Coordinate[] {c0,c1,c2,c3,c4} );
		t1 = geofac.createPolygon(lr, null);
	}
	static Polygon t2;
	static{
		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(1113931.94,7041297.11);
		Coordinate c1 = new Coordinate(1113980.24,7041575.65);
		Coordinate c2 = new Coordinate(1113925.97,7041554.83);
		Coordinate c3 = new Coordinate(1113893.5,7041307.3);
		Coordinate c4 = new Coordinate(1113931.94,7041297.11);
		LinearRing lr = geofac.createLinearRing(new Coordinate[] {c0,c1,c2,c3,c4} );
		t2 = geofac.createPolygon(lr, null);
	}
	
	private static final Coord to1 = new Coord(1113996.6644551097, 7041531.017688233);
	private static final Coord to2 = new Coord(1113968.4303997215, 7041535.944105351);

	private static String inputDir = "/Users/laemmel/devel/hhw3/input/";
	public static void main (String [] args) {
		String config = inputDir + "config.xml";
		String s2config = "/Users/laemmel/devel/hhw3/input/s2d_config_v0.3.xml";
		String demand = "/Users/laemmel/Downloads/Hamburg_Shapefiles_mit_Bevölkerungsdichte/HH_buildings_polygons_rev.shp";
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(demand);

		Config conf = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(conf);

//		Sim2DConfig s2conf = Sim2DConfigUtils.loadConfig(s2config);
//		Sim2DScenario s2sc = Sim2DScenarioUtils.loadSim2DScenario(s2conf);
//		s2sc.connect(sc);

		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		String crs = conf.global().getCoordinateSystem();
		transformCRS(r, crs);
		
		int id = 0;
		
		long tp = 0;
		long carss = 0;
		for (SimpleFeature ft : r.getFeatureSet()) {
			MultiPolygon p = (MultiPolygon) ft.getDefaultGeometry();
			Coordinate c = p.getCentroid().getCoordinate();

			Link l = net.getNearestLinkExactly(new Coord(c.x, c.y));
			double dist = c.distance(new Coordinate(l.getCoord().getX(),l.getCoord().getY()));
			if (dist > CUTOFF_DIST) {
				continue;
			}
			
			

			long persons = (Long) ft.getAttribute("persons");
			
			tp+=persons;
			double cars = (Double) ft.getAttribute("Privat_PKW");
			carss += cars;
			persons -= 1.81 * cars;
			for (int i = 0; i < persons; i++) {


				Person pers = fac.createPerson(Id.create(id++, Person.class));
				pop.addPerson(pers);

				Plan plan = fac.createPlan();
				pers.addPlan(plan);


				Activity act0 = fac.createActivityFromLinkId("origin", l.getId());


				double baseTime = MatsimRandom.getRandom().nextGaussian()*3600+3600;
				while (baseTime < 0 || baseTime > 2*3600) {
					baseTime = MatsimRandom.getRandom().nextGaussian()*3600+3600;
				}

				double time = dist * Math.sqrt(2)/1.34 + baseTime;
				act0.setEndTime(time);
				plan.addActivity(act0);

				Leg leg0 = fac.createLeg("car");
				plan.addLeg(leg0);



//				Id d = MatsimRandom.getRandom().nextBoolean() ? tt0 : tt1; 
				Id<Link> d = Id.create("1622", Link.class);
				Activity act1 = fac.createActivityFromLinkId("destination", d);
				plan.addActivity(act1);
			}
		}
		System.out.println(tp);
		new PopulationWriter(pop, sc.getNetwork()).write(conf.plans().getInputFile());
		createNetworkChangeEvents(sc);
		new ConfigWriter(conf).write(config);
	}


	private static void createNetworkChangeEvents(Scenario sc) {
		Collection<NetworkChangeEvent> events = new LinkedList<NetworkChangeEvent>();
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();


		double time = 0;
		while (time < 4 * 3600) {

			int ii = 0;
			{
				NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
				for (Link l : sc.getNetwork().getLinks().values()) {
					if (CoordUtils.calcEuclideanDistance(to1, l.getToNode().getCoord()) < 0.1 || CoordUtils.calcEuclideanDistance(to1, l.getFromNode().getCoord()) < 0.1) {
						l.setCapacity(4*3600);
						l.setNumberOfLanes(4/0.71);
						l.setFreespeed(1.34);
					
						continue;
					}
					for (Link ll : l.getToNode().getOutLinks().values()){
						if (CoordUtils.calcEuclideanDistance(to1, ll.getToNode().getCoord()) < 0.1){
							l.setCapacity(0.5);
							l.setNumberOfLanes(1);
							l.setLength(.26);
							l.setFreespeed(0.5);
							e.addLink(l);
							ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0);
							e.setFlowCapacityChange(cv);
//							ChangeValue cxv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.01);
//							e.setFreespeedChange(cxv);
							ii++;
							break;
						}
					}
//					if (t1.contains(MGC.coord2Point(l.getToNode().getCoord()))) {
//						l.setCapacity(0.5);
//						l.setNumberOfLanes(1);
//						l.setLength(.26);
//						l.setFreespeed(0.5);
//						e.addLink(l);
//						ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0);
//						e.setFlowCapacityChange(cv);
////						ChangeValue cxv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.1);
////						e.setLanesChange(cxv);
//						ii++;
//					}
				}
				events.add(e);
			}
			int jj = 0;
			{
				NetworkChangeEvent e = fac.createNetworkChangeEvent(time+ 2*60+30);
				for (Link l : sc.getNetwork().getLinks().values()) {
					if (CoordUtils.calcEuclideanDistance(to2, l.getToNode().getCoord()) < 0.1 || CoordUtils.calcEuclideanDistance(to2, l.getFromNode().getCoord()) < 0.1) {
						l.setCapacity(4*3600);
						l.setNumberOfLanes(4/0.71);
					
						continue;
					}
					if (t2.contains(MGC.coord2Point(l.getToNode().getCoord()))) {
						l.setCapacity(0.5);
						l.setNumberOfLanes(1);
						l.setLength(.26);
						l.setFreespeed(0.5);
						e.addLink(l);
						ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0);
						e.setFlowCapacityChange(cv);
//						ChangeValue cxv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.01);
//						e.setFreespeedChange(cxv);
						jj++;
					}
				}
				events.add(e);
			}
			System.out.println(ii + " " + jj);
			time += 4*60 + 30; 
//			if (time < 20*60) {
//				continue;
//			}
			{
				NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
				for (Link l : sc.getNetwork().getLinks().values()) {
					if (CoordUtils.calcEuclideanDistance(to1, l.getToNode().getCoord()) < 0.1 || CoordUtils.calcEuclideanDistance(to1, l.getFromNode().getCoord()) < 0.1) {
						l.setCapacity(4*3600);
						l.setNumberOfLanes(4/0.71);
						continue;
					}
					for (Link ll : l.getToNode().getOutLinks().values()){
						if (CoordUtils.calcEuclideanDistance(to1, ll.getToNode().getCoord()) < 0.1){
							ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.5);
							e.setFlowCapacityChange(cv);
//							ChangeValue cxv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 1.34);
//							e.setFreespeedChange(cxv);
							e.addLink(l);	
							break;
						}
					}
					
//					if (t1.contains(MGC.coord2Point(l.getToNode().getCoord()))) {
//						ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.5);
//						e.setFlowCapacityChange(cv);
//						e.addLink(l);
//					}
				}
				events.add(e);
			}
			{
				NetworkChangeEvent e = fac.createNetworkChangeEvent(time + 2*60+30);
				for (Link l : sc.getNetwork().getLinks().values()) {
					if (CoordUtils.calcEuclideanDistance(to2, l.getToNode().getCoord()) < 0.1 || CoordUtils.calcEuclideanDistance(to2, l.getFromNode().getCoord()) < 0.1) {
						l.setCapacity(4*3600);
						l.setNumberOfLanes(4/0.71);
					
						continue;
					}
					if (t2.contains(MGC.coord2Point(l.getToNode().getCoord()))) {
						e.addLink(l);
						ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.5);
						e.setFlowCapacityChange(cv);
//						ChangeValue cxv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 1.34);
//						e.setFreespeedChange(cxv);
					}
				}
				events.add(e);
			}
			time += 60;
		}

		new NetworkChangeEventsWriter().write(inputDir +"/networkChangeEvents.xml.gz", events);
		sc.getConfig().network().setTimeVariantNetwork(true);
		sc.getConfig().network().setChangeEventsInputFile(inputDir+"/networkChangeEvents.xml.gz");
		new NetworkWriter(sc.getNetwork()).write(sc.getConfig().network().getInputFile());

	}

	private static void transformCRS(ShapeFileReader r1, String crs) {
		CoordinateReferenceSystem target = MGC.getCRS(crs);
		try {
			MathTransform t = CRS.findMathTransform(r1.getCoordinateSystem(), target);
			for (SimpleFeature f : r1.getFeatureSet()) {
				Geometry geo = (Geometry) f.getDefaultGeometry();
				Geometry gg = JTS.transform(geo, t);
				f.setDefaultGeometry(gg);

			}
			//			r1.getFeatureSource().getC
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
