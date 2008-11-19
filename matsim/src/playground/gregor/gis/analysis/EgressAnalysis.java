/* *********************************************************************** *
 * project: org.matsim.*
 * EgressAnalysis.java
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.ShapeFileWriter;
import org.matsim.world.World;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;


public class EgressAnalysis {
	private static final Logger log = Logger.getLogger(DistanceAnalysis.class);
	private FeatureSource featureSourcePolygon;
	private ArrayList<Polygon> polygons;

	private Population population;
	private Envelope envelope = null;
	private QuadTree<Person> personTree;
	private NetworkLayer network;
	private PlansCalcRoute router;
	private FeatureType ftDistrictShape;
	private ArrayList<Feature> features;
	private GeometryFactory geofac;
	private HashMap<Polygon,Double> catchRadi = new HashMap<Polygon,Double>();
	private static double CATCH_RADIUS;
	playground.gregor.collections.gnuclasspath.TreeMap<Double, Feature> ft_tree;
	private GTH gth;
	private Map<Id, EgressNode> egressNodes;


	public EgressAnalysis(FeatureSource features, Population population,
			NetworkLayer network) throws Exception {
		this.featureSourcePolygon = features;
		this.population = population;
		this.network = network;
		this.geofac = new GeometryFactory();
		this.gth = new GTH(this.geofac);
		this.egressNodes = new HashMap<Id,EgressNode>();
		this.router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());
		initFeatureCollection();
		initEgressNodes();
		handlePlans();
		createFeatures();
//		createGridFeatures();
		writePolygons();

	}

	private void createGridFeatures() throws Exception {
		double length = 250;
		Envelope e = this.featureSourcePolygon.getBounds();
		for (double x = e.getMinX(); x < e.getMaxX(); x += length) {
			for (double y = e.getMinY(); y < e.getMaxY(); y+= length) {
				this.features.add(getPolyFeature(new Coordinate(x,y),0,0,length));


			}

		}


	}

	private void writePolygons() {
		try {
			ShapeFileWriter.writeGeometries(this.features, "./padang/egress.shp");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private Collection<Feature> createFeatures() {
		double length = 1000;

		for (EgressNode e : this.egressNodes.values()) {

				if (e.num_current == 0) {
					continue;
				}
				try {
					length = Math.max(100,e.num_current / 50);
					this.features.add(getPolyFeature(MGC.coord2Coordinate(e.node.getCoord()),e.num_current,e.num_shortest,length));
				} catch (IllegalAttributeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		}

		return null;
	}

	private void initFeatureCollection() throws FactoryRegistryException, SchemaException {

		this.features = new ArrayList<Feature>();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType shortest = AttributeTypeFactory.newAttributeType("shortest", Integer.class);
		AttributeType current = AttributeTypeFactory.newAttributeType("current", Integer.class);
		AttributeType deviance = AttributeTypeFactory.newAttributeType("diffsc", Integer.class);
		this.ftDistrictShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id,   current, shortest, deviance}, "egressShape");
	}

	private Feature getPolyFeature(Coordinate coord2Coordinate,
			int num_current, int num_shortest, double length) throws IllegalAttributeException {
		Polygon p = this.gth.getSquare(coord2Coordinate, length);
		int diff = num_current - num_shortest;
		Feature ft = this.ftDistrictShape.create(new Object [] {new MultiPolygon(new Polygon []{p},this.geofac),0,num_current, num_shortest, diff},"egress");
		return ft;
	}




	private void handlePlans() {
			log.info("handle plans");
			for (Person person : this.population) {
				Leg leg = person.getSelectedPlan().getNextLeg(person.getSelectedPlan().getFirstActivity());
				List<Node> route = leg.getRoute().getRoute();
				Node node = route.get(route.size()-2);
				this.egressNodes.get(node.getId()).num_current++;
				Plan plan = new Plan(person);
				plan.addAct(person.getSelectedPlan().getFirstActivity());
				Leg l = new Leg(BasicLeg.Mode.car);
				l.setNum(1);
				l.setDepartureTime(0.0);
				l.setTravelTime(0.0);
				l.setArrivalTime(0.0);
				plan.addLeg(l);
				plan.addAct(person.getSelectedPlan().getNextActivity(leg));
				router.run(plan);
				Leg leg2 = plan.getNextLeg(plan.getFirstActivity());
				List<Node> route2 = leg2.getRoute().getRoute();
				Node node2 = route2.get(route2.size()-2);
				this.egressNodes.get(node2.getId()).num_shortest++;

			}
			log.info("done.");





	}




	private void initEgressNodes() {
		for (Link link : this.network.getLinks().values()) {
			if (link.getId().toString().contains("el")) {
				Id id = link.getFromNode().getId();
				EgressNode  e = new EgressNode();
				e.node = link.getFromNode();
				e.num_shortest = 0;
				e.num_current = 0;
				this.egressNodes.put(id, e);
			}
		}

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
		Population population = new Population();
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		log.info("done.");


		try {
			new EgressAnalysis(features,population,network);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private class EgressNode {
		Node node;
		int num_shortest;
		int num_current;

	}

}
