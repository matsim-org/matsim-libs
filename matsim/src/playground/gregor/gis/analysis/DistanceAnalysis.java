/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisDistance.java
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

package playground.gregor.gis.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.network.algorithms.NetworkSegmentDoubleLinks;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.Route;
import org.matsim.router.Dijkstra;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.world.World;
import org.opengis.referencing.FactoryException;

import playground.gregor.gis.shapeFileProcessing.ShapeFileReader;
import playground.gregor.gis.shapeFileProcessing.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class DistanceAnalysis {
	private static final Logger log = Logger.getLogger(DistanceAnalysis.class);
	private FeatureSource featureSourcePolygon;
	private ArrayList<Polygon> polygons;

	private Plans population;
	private Envelope envelope = null;
	private QuadTree<Person> personTree;
	private NetworkLayer network;
	private PlansCalcRoute router;
	private FeatureType ftDistrictShape;
	private ArrayList<Feature> features;
	private GeometryFactory geofac;
	private HashMap<Polygon,Double> catchRadi = new HashMap<Polygon,Double>();
	private static double CATCH_RADIUS;
	org.matsim.utils.collections.gnuclasspath.TreeMap<Double, Feature> ft_tree;
	





	public DistanceAnalysis(FeatureSource features, Plans population, NetworkLayer network) throws Exception {
		this.featureSourcePolygon = features;
		this.population = population;
		this.network = network;
		this.envelope  = this.featureSourcePolygon.getBounds();
		
		this.router = new PlansCalcRoute(network, new TravelTimeDistanceCostCalculator(new FreespeedTravelTimeCost()), new FreespeedTravelTimeCost());
		this.geofac = new GeometryFactory();
		
		initFeatureCollection();
		parsePolygons();
		createPolygons();
		handlePlans();
		iteratePolygons();
		writePolygons();

		
		





	}
	private void writePolygons() {
		try {
			ShapeFileWriter.writeGeometries(this.features, "./padang/evac_classification.shp");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void createPolygons() throws Exception {
		this.polygons = new ArrayList<Polygon>();
		double length = 375;
		GTH gth = new GTH(this.geofac);
		Envelope e = this.featureSourcePolygon.getBounds();
		for (double x = e.getMinX(); x < e.getMaxX(); x += length) {
			for (double y = e.getMinY(); y < e.getMaxY(); y+= length) {
				Polygon p = gth.getSquare(new Coordinate(x,y), length);
				this.polygons.add(p);
			}
			
		}
		
		
	}





	private void iteratePolygons(){

		this.ft_tree =  new org.matsim.utils.collections.gnuclasspath.TreeMap<Double, Feature>();
		int id = 0;

		int toGo = this.polygons.size();
		for (Polygon polygon : this.polygons) {
			int num_pers = 0;

			Collection<Person> persons = this.personTree.get(polygon.getCentroid().getX(), polygon.getCentroid().getY(),400);

			persons = rmAliens(persons,polygon);

			num_pers = persons.size();
			if (num_pers == 0) continue;

			double [] dists = handlePersons(persons);
			double meanDeviance = (dists[0] - dists[1]) / num_pers;
			double length_shortest = dists[1] / num_pers;
			double length_selected = dists[0] / num_pers;
			double evac_time = (-dists[2] * 10) / num_pers;  
			double varK = dists[3];
			
			try {
				this.features.add(getFeature(polygon, meanDeviance, length_shortest, length_selected, num_pers, id++, evac_time,varK));
			} catch (IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			toGo--;
			if (toGo % 100 == 0)
				log.info("ToGo:" + toGo);

		}

	}

	private double[] handlePersons(Collection<Person> persons) {

		double [] dist = {0., 0.,0., 0.}; 
		double diff = 0;
		double [] diffAll = new double [persons.size()];
		int i = 0;
		

		
		for (Person person : persons) {
			Leg leg = person.getSelectedPlan().getNextLeg(person.getSelectedPlan().getFirstActivity());
			double l1 = leg.getRoute().getDist();
			
			dist[0] +=  l1;
			dist[2] += person.getSelectedPlan().getScore(); 
			Plan plan = new Plan(person);
			plan.addAct(person.getSelectedPlan().getFirstActivity());
			plan.addLeg(new Leg(1,"car",0.0,0.0,0.0));
			plan.addAct(person.getSelectedPlan().getNextActivity(leg));
			router.run(plan);
			Leg leg2 = plan.getNextLeg(plan.getFirstActivity());
			double l2 = leg2.getRoute().getDist();
			dist[1] = l2;
		
			diff += dist[0] - dist[1];
			diffAll[i++] = dist[0] - dist[1];
		}
		double mean = diff / i;
		double var = 0;
		for (i = 0; i < persons.size(); i++) {
			var += (diffAll[i] - mean) * (diffAll[i] - mean);
			
		}
		double sd = Math.sqrt(var);
		double varK = sd / mean;
		dist[3] = varK;
		return dist;

	}
	private Collection<Person>  rmAliens(Collection<Person> persons, Polygon polygon) {

		ArrayList<Person> ret = new ArrayList<Person>();
		for (Person person : persons) {
			Point p = MGC.coord2Point(person.getSelectedPlan().getFirstActivity().getCoord());
			if (polygon.contains(p)) {
				ret.add(person);
			}
		}
		return ret;
	}
	private void handlePlans() {

		this.personTree = new QuadTree<Person>(0,0,3*this.envelope.getMaxX(),3*this.envelope.getMaxY());
		for (Person person : this.population.getPersons().values()){
			CoordI c = person.getSelectedPlan().getFirstActivity().getCoord();
			this.personTree.put(c.getX(), c.getY(), person);
		}

	}
	private void parsePolygons()throws Exception{

		log.info("parseing features ...");

		FeatureCollection collectionPolygon = this.featureSourcePolygon.getFeatures();
		this.envelope  = this.featureSourcePolygon.getBounds();


		this.polygons = new ArrayList<Polygon>();


		FeatureIterator it = collectionPolygon.features();
		while (it.hasNext()) {
			Feature feature = it.next();
			double catch_radius = Math.max(feature.getBounds().getHeight(),feature.getBounds().getWidth())/2;
//			CATCH_RADIUS = Math.max(CATCH_RADIUS,catch_radius);

			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
				this.catchRadi .put(polygon,catch_radius);
				this.polygons.add(polygon);

			}
		}

		log.info("done.");

	}

	private Feature getFeature(Polygon polygon, double meanDeviance,
			double length_shortest, double length_selected, int num_pers, int id, double evac_time, double varK ) throws IllegalAttributeException {

		return this.ftDistrictShape.create(new Object [] {new MultiPolygon(new Polygon []{polygon },this.geofac),id,num_pers, length_shortest, length_selected, meanDeviance, meanDeviance*meanDeviance,evac_time, varK},"network");

	}

	private void initFeatureCollection() throws FactoryRegistryException, SchemaException {
		this.features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType inhabitants = AttributeTypeFactory.newAttributeType("inhabitants", Integer.class);
		AttributeType shortest = AttributeTypeFactory.newAttributeType("shortest_path", Double.class);
		AttributeType current = AttributeTypeFactory.newAttributeType("current_path", Double.class);
		AttributeType deviance = AttributeTypeFactory.newAttributeType("diff_shortest_current", Double.class);
		AttributeType devianceSqr = AttributeTypeFactory.newAttributeType("square_diff_shortest_current", Double.class);
		AttributeType evac_time = AttributeTypeFactory.newAttributeType("evac_time", Double.class);
		AttributeType varK = AttributeTypeFactory.newAttributeType("varK", Double.class);
		this.ftDistrictShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id, inhabitants, shortest, current, deviance, devianceSqr, evac_time, varK }, "gridShape");


	}

	public static void main(String [] args) {


		String district_shape_file;
		


		if (args.length != 2) {
			throw new RuntimeException("wrong number of arguments! Pleas run DistanceAnalysis config.xml shapefile.shp" );
		} else {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});
			district_shape_file = args[1];
			
		}

		World world = Gbl.createWorld();

		log.info("loading network from " + Gbl.getConfig().network().getInputFile());
		NetworkFactory fc = new NetworkFactory();
		fc.setLinkPrototype(TimeVariantLinkImpl.class);
		
		NetworkLayer network = new NetworkLayer(fc);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		world.setNetworkLayer(network);
		world.complete();
		log.info("done.");
		

		

		log.info("loading shape file from " + district_shape_file);
		FeatureSource features = null;
		try {
			features = ShapeFileReader.readDataFile(district_shape_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done");


		log.info("loading population from " + Gbl.getConfig().plans().getInputFile());
		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
//		plansReader.readFile("./badPersons.xml");
		log.info("done.");


		try {
			new DistanceAnalysis(features,population,network);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			new EgressAnalysis(features,population,network);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

