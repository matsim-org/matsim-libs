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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.gis.helper.GTH;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * - man braucht eine shape file mit einem Raster
 *   DistanceAnalysis.createPolygons() kreiert eins
 *
 * - dann werden alle Personen mit den Koordinaten der home location in einen
 *   QuadTree gepackt. (DistanceAnalysis.handlePlans())
 *
 * - nun kann man ???ber die Polygons des Rasters iterieren
 *   (DistanceAnalysis.iteratePolygons()). Die Personen werden in zwei Schritten
 *   gefiltert (dieses Vorgehen ist wesentlich effizienter als immer ???ber alle
 *   Personen zu iterieren):
 *
 * - Aus dem QuadTree holt man sich zunaechst alle Personen im Umkreis von  min
 *   d/2 des Mittelpunkts eines Polygons.  (DistanceAnalysis.iteratePolygons -
 *   Zeile 158)
 *
 * - Dannach werden alle die Personen, deren home location sich ausserhalb des
 *   Polygons befindet mit der Methode DistanceAnalysis.rmAliens(...,...)
 *   entfernt.
 *
 * - zum Schluss wird basierend auf der Analyse dieser Personen noch ein neues
 *   Feature mit den entsprechenden Attributen angelegt
 *   (DistanceAnalysis.iteratePolygons - Zeile 174)
 *
 * - am Ende werden noch alle Features in einen neuen Shapefile geschrieben
 *   (DistanceAnalysis.writePolygons())
 *
 * @author laemmel
 *
 */
public class DistanceAnalysis {
	private static final Logger log = Logger.getLogger(DistanceAnalysis.class);
	private final FeatureSource featureSourcePolygon;
	private ArrayList<Polygon> polygons;

	private final Population population;
	private Envelope envelope = null;
	private QuadTree<Person> personTree;
	private final Network network;
	private final PlansCalcRoute router;
	private FeatureType ftDistrictShape;
	private ArrayList<Feature> features;
	private final GeometryFactory geofac;
	private final HashMap<Polygon,Double> catchRadi = new HashMap<Polygon,Double>();
	private static double CATCH_RADIUS;
//	private TreeMap<Double, Feature> ft_tree;

	public DistanceAnalysis(final FeatureSource features, final Population population, final Network network, final Config config) throws Exception {
		this.featureSourcePolygon = features;
		this.population = population;
		this.network = network;
		this.envelope  = this.featureSourcePolygon.getBounds();

		FreespeedTravelTimeCost ttCost = new FreespeedTravelTimeCost(config.planCalcScore());
		this.router = new PlansCalcRoute(config.plansCalcRoute(), network, new TravelTimeDistanceCostCalculator(ttCost, config.planCalcScore()), ttCost, new DijkstraFactory());
		this.geofac = new GeometryFactory();

		initFeatureCollection();
//		parsePolygons();
		createPolygons();
		handlePlans();
		iteratePolygons();
		writePolygons();
	}


	private void writePolygons() {
		try {
			ShapeFileWriter.writeGeometries(this.features, "./padang/evac_classification.shp");
		} catch (IOException e) {
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

//		this.ft_tree =  new TreeMap<Double, Feature>();
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
			int dest = (int) dists[4];

			try {
				this.features.add(getFeature(polygon, meanDeviance, length_shortest, length_selected, num_pers, id++, evac_time,varK, dest));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
			toGo--;
			if (toGo % 100 == 0)
				log.info("ToGo:" + toGo);

		}

	}

	private double[] handlePersons(final Collection<Person> persons) {

		double [] dist = {0., 0.,0., 0., 0.};
		double diff = 0;
		double [] diffAll = new double [persons.size()];
		int i = 0;
		int [] dests = new int [100];


		for (Person person : persons) {
			Leg leg = ((PlanImpl) person.getSelectedPlan()).getNextLeg(((PlanImpl) person.getSelectedPlan()).getFirstActivity());
			double l1 = leg.getRoute().getDistance();
			List<Id> ls = ((NetworkRoute) leg.getRoute()).getLinkIds();
			Id lId = ls.get(ls.size()-1);
			String destS  = lId.toString().replace("el", "");
			int dest = Integer.parseInt(destS);
			dests[dest]++;

			dist[0] +=  l1;
			dist[2] += person.getSelectedPlan().getScore().doubleValue();
			PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
			plan.addActivity(((PlanImpl) person.getSelectedPlan()).getFirstActivity());
			LegImpl ll = new org.matsim.core.population.LegImpl(TransportMode.car);
			ll.setArrivalTime(0.0);
			ll.setDepartureTime(0.0);
			ll.setTravelTime(0.0);
			plan.addLeg(ll);
			plan.addActivity(((PlanImpl) person.getSelectedPlan()).getNextActivity(leg));
			this.router.run(plan);
			Leg leg2 = plan.getNextLeg(plan.getFirstActivity());
			double l2 = leg2.getRoute().getDistance();
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

		int max = 0;
		int dest = 0;
		for (int ii = 0; ii < 100; ii++) {
			if (dests[ii] > max) {
				max = dests[ii];
				dest = ii;
			}

		}
		dist[4] = dest;
		return dist;

	}
	private Collection<Person>  rmAliens(final Collection<Person> persons, final Polygon polygon) {

		ArrayList<Person> ret = new ArrayList<Person>();
		for (Person person : persons) {
			Point p = MGC.coord2Point(((PlanImpl) person.getSelectedPlan()).getFirstActivity().getCoord());
			if (polygon.contains(p)) {
				ret.add(person);
			}
		}
		return ret;
	}
	private void handlePlans() {

		this.personTree = new QuadTree<Person>(0,0,3*this.envelope.getMaxX(),3*this.envelope.getMaxY());
		for (Person person : this.population.getPersons().values()){
			Coord c = ((PlanImpl) person.getSelectedPlan()).getFirstActivity().getCoord();
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

	private Feature getFeature(final Polygon polygon, final double meanDeviance,
			final double length_shortest, final double length_selected, final int num_pers, final int id, final double evac_time, final double varK, final int dest ) throws IllegalAttributeException {

		return this.ftDistrictShape.create(new Object [] {new MultiPolygon(new Polygon []{polygon },this.geofac),id,num_pers, length_shortest, length_selected, meanDeviance, meanDeviance*meanDeviance,evac_time, varK, dest},"network");

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
		AttributeType dest = AttributeTypeFactory.newAttributeType("dest", Integer.class);
		this.ftDistrictShape = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id, inhabitants, shortest, current, deviance, devianceSqr, evac_time, varK, dest }, "gridShape");


	}

	public static void main(final String [] args) throws IOException {


		String district_shape_file;

		if (args.length != 2) {
			throw new RuntimeException("wrong number of arguments! Pleas run DistanceAnalysis config.xml shapefile.shp" );
		}
		Config config = ConfigUtils.loadConfig(args[0]);
		district_shape_file = args[1];

		ScenarioImpl scenario = new ScenarioImpl(config);

		log.info("loading network from " + config.network().getInputFile());
		NetworkImpl network = scenario.getNetwork();
		network.getFactory().setLinkFactory(new TimeVariantLinkFactory());

		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		log.info("done.");


		log.info("loading shape file from " + district_shape_file);
		FeatureSource features = null;
		try {
			features = ShapeFileReader.readDataFile(district_shape_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done");


		log.info("loading population from " + config.plans().getInputFile());
		Population population = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());
//		plansReader.readFile("./badPersons.xml");
		log.info("done.");

		try {
			new DistanceAnalysis(features,population,network,config);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			new EgressAnalysis(features,population,network,config);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

