/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.helper;

import java.util.Collection;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class GISBasedPopulationGenerator {

	private final Scenario sc;
	private final Collection<SimpleFeature> fts;
	private int id = 0;
	private final Id safeLinkId;
	private final NetworkLegRouter router;
	private final Collection<SimpleFeature> ftsEnv;

	private final GeometryFactory geofac = new GeometryFactory();
	//	private final Id safeLinkId1 = new IdImpl(501);
	//	private final Id safeLinkId2 = new IdImpl(581);
	//	private final Id safeLinkId3 = new IdImpl(0);
	private final Id safeLinkId1 = new IdImpl(871);
	private final Id safeLinkId2 = new IdImpl(0);
	private final Id safeLinkId3 = new IdImpl(905);

	public GISBasedPopulationGenerator(Scenario sc, Collection<SimpleFeature> population, Collection<SimpleFeature> environment, Id destination) {
		this.sc = sc;
		this.fts = population;
		this.ftsEnv = environment;
		this.safeLinkId = destination;
		DijkstraFactory fac = new DijkstraFactory();
		FreespeedTravelTimeAndDisutility travelTimeCosts = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
		LeastCostPathCalculator dijkstra = fac.createPathCalculator(sc.getNetwork(), travelTimeCosts, travelTimeCosts);
		this.router = new NetworkLegRouter(sc.getNetwork(), dijkstra, ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getModeRouteFactory());
	}

	private void run() {


		for (SimpleFeature ft : this.fts) {
			createPersons(ft);
		}
		UTurnRemover utr = new UTurnRemover(this.sc);
		utr.notifyIterationStarts(new IterationStartsEvent(null, 0));
	}


	private void createPersons(SimpleFeature ft) {
		Population pop = this.sc.getPopulation();
		PopulationFactory pb = pop.getFactory();
		Coordinate cc = ((Geometry) ft.getDefaultGeometry()).getCoordinate();
		long number = (Long)ft.getAttribute("persons");
		for (; number > 0; number--) {
			Person pers = pb.createPerson(this.sc.createId("g"+Integer.toString(this.id++)));
			pop.addPerson(pers);
			double rnd = MatsimRandom.getRandom().nextDouble();
			if (rnd <= .33){
				createPlan(pers,this.safeLinkId1,cc, pb,true);
			} else {
				createPlan(pers,this.safeLinkId1,cc, pb,true);
			}
			if (rnd > .33 && rnd <= .66) {
				createPlan(pers,this.safeLinkId2,cc, pb, true);
			} else {
				createPlan(pers,this.safeLinkId2,cc, pb, false);
			}
			if (rnd > .66) {
				createPlan(pers,this.safeLinkId3,cc, pb, true);
			} else {
				createPlan(pers,this.safeLinkId3,cc, pb, false);
			}

		}
	}

	private void createPlan(Person pers, Id safeLinkId2, Coordinate cc, PopulationFactory pb, boolean select) {
		Plan plan = pb.createPlan();
		Coord c = MGC.coordinate2Coord(cc);

		Link l = getNearestVisibleLink(c);
		ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", l.getId());
		act.setCoord(c);
		act.setEndTime(0);
		plan.addActivity(act);
		Leg leg = pb.createLeg("walk2d");
		plan.addLeg(leg);
		Activity act2 = pb.createActivityFromLinkId("h", safeLinkId2);
		act2.setEndTime(0);
		plan.addActivity(act2);
		//		plan.setScore(0.);

		pers.addPlan(plan);
		if (select){
			((PersonImpl)pers).setSelectedPlan(plan);
		}
		this.router.routeLeg(pers, leg, act, act2, 0);

	}

	private Link getNearestVisibleLink(Coord c) {
		Link l = ((NetworkImpl)this.sc.getNetwork()).getNearestLink(c);
		if (!intersectsEnvironment(l,c)) {
			return l;
		}

		Link ret = null;
		double dist = Double.POSITIVE_INFINITY;
		for (Link ll : this.sc.getNetwork().getLinks().values()) {
			if (((CoordImpl)c).calcDistance(ll.getCoord()) < dist ) {
				if (!intersectsEnvironment(ll, c)) {
					ret = ll;
					dist =((CoordImpl)c).calcDistance(ll.getCoord());
				}
			}
		}
		return ret;
	}

	private boolean intersectsEnvironment(Link l, Coord c) {
		Coordinate c1 = MGC.coord2Coordinate(l.getFromNode().getCoord());
		Coordinate c2 = MGC.coord2Coordinate(l.getToNode().getCoord());
		Coordinate c3 = MGC.coord2Coordinate(c);
		LinearRing shell = this.geofac.createLinearRing(new Coordinate[] {c1, c2, c3, c1});
		Polygon p = this.geofac.createPolygon(shell, null);
		for (SimpleFeature ft : this.ftsEnv) {
			if (((Geometry) ft.getDefaultGeometry()).intersects(p)) {
				return true;
			}
		}

		return false;
	}

	public static void main(String [] args) {
		String networkFile = "/Users/laemmel/devel/sim2dDemoII/input/network.xml";
		String popFile = "/Users/laemmel/devel/sim2dDemoII/input/plans.xml";
		String popShape = "/Users/laemmel/devel/sim2dDemoII/raw_input/population.shp";
		String floorShape = "/Users/laemmel/devel/sim2dDemoII/raw_input/floorplan.shp";

		Config c = ConfigUtils.createConfig();
		c.network().setInputFile(networkFile);
		Scenario sc = ScenarioUtils.loadScenario(c);

		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(popShape);
		Collection<SimpleFeature> features = reader.getFeatureSet();

		ShapeFileReader readerII = new ShapeFileReader();
		readerII.readFileAndInitialize(floorShape);
		Collection<SimpleFeature> featuresII = readerII.getFeatureSet();

		new GISBasedPopulationGenerator(sc,features, featuresII,new IdImpl(501)).run();

		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(popFile);
	}



}
