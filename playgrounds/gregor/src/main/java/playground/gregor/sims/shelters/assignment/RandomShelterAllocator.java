/* *********************************************************************** *
 * project: org.matsim.*
 * RandomShelterAllocator.java
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
package playground.gregor.sims.shelters.assignment;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author laemmel
 * 
 */
public class RandomShelterAllocator extends EvacuationPopulationFromShapeFileLoader {

	private static final Logger log = Logger.getLogger(RandomShelterAllocator.class);

	private final ScenarioImpl scenario;
	private final List<Building> buildings;
	private final EvacuationShelterNetLoaderForShelterAllocation esnl;
	private final Population pop;
	private final List<FloodingReader> readers;

	private Dijkstra router;

	private QuadTree<FloodingInfo> fis;

	private PopulationFactory popB;

	private EvacuationStartTimeCalculator time;

	private int count = 0;

	/**
	 * @param buildings
	 * @param scenarioData
	 * @param esnl
	 * @param netcdfReaders
	 */
	public RandomShelterAllocator(List<Building> buildings, ScenarioImpl scenarioData, EvacuationShelterNetLoaderForShelterAllocation esnl, List<FloodingReader> netcdfReaders) {
		super(scenarioData.getPopulation(), buildings, scenarioData, netcdfReaders);
		this.scenario = scenarioData;
		this.buildings = buildings;
		this.esnl = esnl;
		this.pop = scenarioData.getPopulation();
		this.readers = netcdfReaders;
	}

	@Override
	public Population getPopulation() {
		log.info("doing intialization...");
		if (this.readers != null) {
			buildFiQuadTree();
		}
		initRouter();
		createShelterLinks();

		log.info("creating plans");
		this.popB = this.pop.getFactory();
		this.time = getEndCalculatorTime();
		createPlans();
		log.info("done.");

		return null;
	}

	/**
	 * 
	 */
	private void createPlans() {
		List<Tuple<Id, Shelter>> shelters = new LinkedList<Tuple<Id, Shelter>>();
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			if ((link.getId().toString().contains("sl") && link.getId().toString().contains("b"))) {
				Building b = this.esnl.getShelterLinkMapping().get(link.getId());
				int cap = b.getShelterSpace();
				if (cap <= 0) {
					continue;
				}
				Shelter t = new Shelter(cap, 0);
				Tuple<Id, Shelter> tup = new Tuple<Id, Shelter>(link.getId(), t);
				shelters.add(tup);
			} else if (link.getId().toString().equals("el1")) {
				int cap = 500000; //
				Shelter t = new Shelter(cap, 0);
				Tuple<Id, Shelter> tup = new Tuple<Id, Shelter>(link.getId(), t);
				shelters.add(tup);
			}
		}

		int count = 0;
		for (Building building : this.buildings) {

			// Coordinate c = building.getGeo().getCoordinate();
			// Link link = this.quadTree.get(c.x,c.y);

			Coord coord = MGC.point2Coord(building.getGeo().getCentroid());
			if (this.fis != null) {
				FloodingInfo fi = this.fis.get(coord.getX(), coord.getY());
				if (fi.getCoordinate().distance(building.getGeo().getCentroid().getCoordinate()) > this.scenario.getConfig().evacuation().getBufferSize()) {
					continue;
				}

			}

			if (coord == null) {
				throw new RuntimeException();
			}

			int numOfPers = getNumOfPersons(building);

			Link orig = this.scenario.getNetwork().getNearestLink(coord);
			while (orig.getId().toString().contains("l")) {
				orig = orig.getFromNode().getInLinks().values().iterator().next();
			}

			Node origN = orig.getToNode();

			for (int i = 0; i < numOfPers; i++) {
				int idx = MatsimRandom.getRandom().nextInt(shelters.size());
				Tuple<Id, Shelter> tup = shelters.get(idx);
				Link dest = this.scenario.getNetwork().getLinks().get(tup.getFirst());
				Node destN = dest.getFromNode();
				tup.getSecond().count++;
				if (tup.getSecond().count >= tup.getSecond().cap) {
					shelters.remove(idx);
				}
				createPlan(origN, destN, orig, dest);

			}
		}

	}

	private void buildFiQuadTree() {

		Envelope envelope = new Envelope(0, 0, 0, 0);

		for (FloodingReader fr : this.readers) {
			envelope.expandToInclude(fr.getEnvelope());
		}
		this.fis = new QuadTree<FloodingInfo>(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
		for (FloodingReader fr : this.readers) {
			for (FloodingInfo fi : fr.getFloodingInfos()) {
				this.fis.put(fi.getCoordinate().x, fi.getCoordinate().y, fi);
			}
		}

	}

	private void createShelterLinks() {
		this.esnl.generateShelterLinks(0);

	}

	private void createPlan(Node orig, Node dest, Link orig2, Link dest2) {
		if (orig.getId().toString().contains("n")) {
			throw new RuntimeException("this should never happen!");
		}
		Person pers = this.popB.createPerson(this.scenario.createId(Integer.toString(this.count++)));
		Plan plan = this.popB.createPlan();
		plan.setPerson(pers);
		ActivityImpl act = (ActivityImpl) this.popB.createActivityFromLinkId("h", orig2.getId());
		act.setEndTime(this.time.getEvacuationStartTime(act));
		plan.addActivity(act);
		Leg leg = this.popB.createLeg(TransportMode.car);
		plan.addLeg(leg);
		ActivityImpl act2 = (ActivityImpl) this.popB.createActivityFromLinkId("h", dest2.getId());
		plan.addActivity(act2);
		pers.addPlan(plan);
		this.pop.addPerson(pers);

	}

	private void initRouter() {
		FreespeedTravelTimeCost tt = new FreespeedTravelTimeCost(-6, 0, 0);
		PluggableTravelCostCalculator tc = new PluggableTravelCostCalculator(tt);
		this.router = new Dijkstra(this.scenario.getNetwork(), tc, tt);
	}

	private static class Shelter {

		int cap = 0;
		int count = 0;

		/**
		 * @param cap2
		 *            capacity
		 * @param i
		 *            utilization
		 */
		public Shelter(int cap, int i) {
			this.cap = cap;
			this.count = i;
		}

	}

}
