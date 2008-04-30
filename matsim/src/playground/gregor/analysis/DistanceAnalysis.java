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

package playground.gregor.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.world.World;
import org.opengis.referencing.FactoryException;

import playground.gregor.shapeFileToMATSim.ShapeFileReader;
import playground.gregor.shapeFileToMATSim.ShapeFileWriter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
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
	private int num_of_classes;
	
	
	
	
	public DistanceAnalysis(FeatureSource features, Plans population, NetworkLayer network, int num_of_classes) {
		this.featureSourcePolygon = features;
		this.population = population;
		this.network = network;
		this.num_of_classes = num_of_classes;
		this.router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());
		this.geofac = new GeometryFactory();
		try {
			initFeatureCollection();
		} catch (FactoryRegistryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SchemaException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			parsePolygons();
		} catch (Exception e) {
				e.printStackTrace();
		}
		handlePlans();
		iteratePolygons();
		writePolygons();
		
		


	}
	private void writePolygons() {
		double from = this.ft_tree.firstKey();
		double to = this.ft_tree.lastKey();
		double stepsize = (to - from) / this.num_of_classes;
		
		
		
		for (double key = from+stepsize; key < (to+stepsize); key += stepsize) {
			this.features.clear();
			while (this.ft_tree.lowerEntry(key) != null) {
				double tmp_key = this.ft_tree.lowerKey(key);
				Feature ft = this.ft_tree.get(tmp_key);
				this.features.add(ft);
				this.ft_tree.remove(tmp_key);
			}
			try {
				ShapeFileWriter.writeGeometries(this.features, "./padang/test_ft" + (int)key  + ".shp");
			} catch (Exception e) {
					e.printStackTrace();
			}			
			
			
		}
		
		
		
		
		

		
	}
	private void iteratePolygons() {
		
		this.ft_tree =  new org.matsim.utils.collections.gnuclasspath.TreeMap<Double, Feature>();

		
		
		for (Polygon polygon : this.polygons) {
			int num_pers = 0;
			double length_shortest = 0;
			double length_selected = 0;
			
			
			
			Collection<Person> persons = this.personTree.get(polygon.getCentroid().getX(), polygon.getCentroid().getY(),this.catchRadi.get(polygon));
			System.out.println("11111111111111111" + persons.size());
			persons = rmAliens(persons,polygon);
			System.out.println("22222222222222222" + persons.size());
			num_pers = persons.size();
			if (num_pers == 0) continue;
			double meanDeviance = handlePersons(persons) / num_pers;
			
			try {
				ft_tree.put(meanDeviance, getFeature(polygon,meanDeviance,length_shortest,length_selected, num_pers));
			} catch (IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			try {
//				this.features.add(getFeature(polygon,meanDeviance,length_shortest,length_selected, num_pers));
//			} catch (IllegalAttributeException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
		}
		
	}
	private Feature getFeature(Polygon polygon, double meanDeviance,
			double length_shortest, double length_selected, int num_pers) throws IllegalAttributeException {

		return this.ftDistrictShape.create(new Object [] {new MultiPolygon(new Polygon []{polygon },this.geofac), -1, 0.0, polygon.getArea(), " ", num_pers, meanDeviance },"network");

	}
	private double handlePersons(Collection<Person> persons) {
		double length_selected = 0;
		double length_shortest = 0;
		for (Person person : persons) {
			Leg leg = person.getSelectedPlan().getNextLeg(person.getSelectedPlan().getFirstActivity());
			length_selected += leg.getRoute().getDist();
			Plan plan = new Plan(person);
			plan.addAct(person.getSelectedPlan().getFirstActivity());
			plan.addLeg(new Leg(1,"car",0.0,0.0,0.0));
			plan.addAct(person.getSelectedPlan().getNextActivity(leg));
			router.run(plan);
			Leg leg2 = plan.getNextLeg(plan.getFirstActivity());
			length_shortest += leg2.getRoute().getDist();
		}
		
		return length_selected  - length_shortest;
		
	}
	private Collection<Person>  rmAliens(Collection<Person> persons, Polygon polygon) {
//		ConcurrentLinkedQueue<Person> ret = new ConcurrentLinkedQueue<Person>(persons);
		ArrayList<Person> ret = new ArrayList<Person>();
		for (Person person : persons) {
			Point p = MGC.coord2Point(person.getSelectedPlan().getFirstActivity().getCoord());
			if (polygon.contains(p)) {
//				ret.remove(person);
				ret.add(person);
			}
		}
		return ret;
	}
	private void handlePlans() {
		
		this.personTree = new QuadTree<Person>(this.envelope.getMinX(),this.envelope.getMinY(),this.envelope.getMaxX(),this.envelope.getMaxY());
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
//			 CATCH_RADIUS = Math.max(CATCH_RADIUS,catch_radius);
			
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
				this.catchRadi .put(polygon,catch_radius);
				this.polygons.add(polygon);
			
			}
		}
		
		log.info("done.");
		
	}
	
	private void initFeatureCollection() throws FactoryRegistryException, SchemaException {
		this.features = new ArrayList<Feature>();
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType width = AttributeTypeFactory.newAttributeType("width", Double.class);
		AttributeType area = AttributeTypeFactory.newAttributeType("area", Double.class);
		AttributeType info = AttributeTypeFactory.newAttributeType("info", String.class);
		AttributeType persons = AttributeTypeFactory.newAttributeType("persons", Integer.class);
		AttributeType deviance = AttributeTypeFactory.newAttributeType("deviance", Double.class);
		this.ftDistrictShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id, width, area, info, persons, deviance }, "linkShape");
		
		
	}
	
	public static void main(String [] args) {

		
		String district_shape_file;
		int num_of_classes;


		if (args.length != 3) {
			throw new RuntimeException("wrong number of arguments! Pleas run DistanceAnalysis config.xml shapefile.shp num_classes" );
		} else {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});
			district_shape_file = args[1];
			num_of_classes = Integer.parseInt(args[2]);
		}

		World world = Gbl.createWorld();

		log.info("loading network from " + Gbl.getConfig().network().getInputFile());
		NetworkLayer network = new NetworkLayer();
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
		log.info("done.");
		
		
		DistanceAnalysis da = new DistanceAnalysis(features,population,network, num_of_classes);
		
	}
}

