/* *********************************************************************** *
 * project: org.matsim.*
 * GreedyShelterAllocator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sims.shelters.allocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;

import com.vividsolutions.jts.geom.Envelope;

public class GreedyShelterAllocator extends EvacuationPopulationFromShapeFileLoader {

	private static final Logger log = Logger.getLogger(GreedyShelterAllocator.class);
	
	private final Scenario scenario;
	private Dijkstra router;
	private final List<Building> buildings;

	private final List<SinkNodeInfo> sinks = new ArrayList<SinkNodeInfo>();
	
	private final Map<Node, SourceNodeInfo> sources = new HashMap<Node, SourceNodeInfo>();
	
	private Queue<NodeCostInfo> sourceQueue;
	
	private final EvacuationShelterNetLoaderForShelterAllocation esnl;
	
	private int count = 0;

	private final Population pop;

	private PopulationFactory popB;

	private EvacuationStartTimeCalculator time;

	private int numOfPers = 0;

	private List<FloodingReader> readers;
	
	private QuadTree<FloodingInfo> fis = null;
	
	public GreedyShelterAllocator(Population pop, List<Building> buildings, Scenario scenario, EvacuationShelterNetLoaderForShelterAllocation esnl, List<FloodingReader> netcdfReaders) {
		super(pop, buildings, scenario);
		//TODO the corresponding fields in 
		//the super class should be accessed instead of creating these fields 
		this.scenario = scenario;
		this.buildings = buildings;
		this.esnl = esnl;
		this.pop = pop;
		this.readers = netcdfReaders;
	}
	
	@Override
	public Population getPopulation() {
		log.info("doing intialization...");
		if (this.readers != null) {
			buildFiQuadTree();
		}
		initRouter();
		initNodeInfos();
		createShelterLinks();
		
		gatherSinkNodes();
		log.info("done.");
		
		log.info("calculating distance from each sink node to each source node");
		calcSinkSourceDist();
		log.info("done.");
		
		log.info("creating plans");
		this.popB = this.pop.getFactory();
		this.time = getEndCalculatorTime();
		createPlans();
		log.info("done.");
		
		return null;
	}
	
	private void buildFiQuadTree() {

		Envelope envelope = new Envelope(0,0,0,0);

		for (FloodingReader fr : this.readers) {
			envelope.expandToInclude(fr.getEnvelope());
		}
		this.fis = new QuadTree<FloodingInfo>(envelope.getMinX(),envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
		for (FloodingReader fr : this.readers) {
			for (FloodingInfo fi : fr.getFloodingInfos()) {
					this.fis.put(fi.getCoordinate().x, fi.getCoordinate().y, fi);
			}
		}

	}

	private void createShelterLinks() {
		this.esnl.generateShelterLinks(this.numOfPers);

		
	}

	private void createPlans() {
		int pop = 0;
		while (this.sourceQueue.size() > 0) {
			NodeCostInfo nci = this.sourceQueue.poll();
			if (nci.ni.numPers == 0) {
				continue;
			}
			int d = nci.ni.numPers;
			for (int i = 0; i < d; i++) {
				if (nci.sni.capacity > 0) {
					pop++;
					nci.sni.capacity--;
					nci.ni.numPers--;
					createPlan(nci.ni.node,nci.sni.node);
				} else {
					break;
				}
			}
			
		}
		System.out.println(pop);
		
	}

	private void createPlan(Node orig, Node dest) {
		if (orig.getId().toString().contains("n")){
			throw new RuntimeException("this should never happen!");
		}
		Person pers = this.popB.createPerson(this.scenario.createId(Integer.toString(this.count++)));
		Plan plan = this.popB.createPlan();
		plan.setPerson(pers);
		ActivityImpl act = (ActivityImpl) this.popB.createActivityFromLinkId("h", orig.getInLinks().values().iterator().next().getId());
		act.setEndTime(this.time.getEvacuationStartTime(act));
		plan.addActivity(act);
		Leg leg = this.popB.createLeg(TransportMode.car);
		plan.addLeg(leg);
		ActivityImpl act2 = (ActivityImpl) this.popB.createActivityFromLinkId("h", dest.getInLinks().values().iterator().next().getId());
		plan.addActivity(act2);
		pers.addPlan(plan);
		this.pop.addPerson(pers);
		
	}

	private void calcSinkSourceDist() {
		
		Comparator<? super NodeCostInfo> comp = new NodeCostComparator();
		this.sourceQueue = new PriorityQueue<NodeCostInfo>(this.sinks.size() * this.sources.size(),comp);
		
		for (SinkNodeInfo sni : this.sinks) {
			for (Entry<Node, SourceNodeInfo> e : this.sources.entrySet()) {
				Path p = this.router.calcLeastCostPath(e.getKey(), sni.node, 0);
				double cost = p.travelCost;
				NodeCostInfo nci = new NodeCostInfo();
				nci.ni = e.getValue();
				nci.sni = sni;
				nci.cost = cost;
				this.sourceQueue.add(nci);
			}
		}
	}

	private void gatherSinkNodes() {
		Map<Node,Building> shelterNodeMapping = new HashMap<Node, Building>();
		for (Entry<Id, Building> e : this.esnl.getShelterLinkMapping().entrySet()) {
			Node n = this.scenario.getNetwork().getLinks().get(e.getKey()).getToNode();
			shelterNodeMapping.put(n, e.getValue());
		}
		
		
		for (Node node : this.scenario.getNetwork().getNodes().values()){
			if (node.getId().toString().contains("n")) {
				if (node.getId().toString().contains("b")) { 
					SinkNodeInfo sni = new SinkNodeInfo();
					Building b = shelterNodeMapping.get(node);
					sni.node = node;
					sni.capacity = b.getShelterSpace();
					this.sinks.add(sni);
				} else if (node.getId().toString().equals("en2") ) {
					SinkNodeInfo sni = new SinkNodeInfo();
					sni.node = node;
					sni.capacity = this.esnl.getShelterLinkMapping().get(new IdImpl("el1")).getShelterSpace();
					this.sinks.add(sni);
				}
			}
		}
		
	}

	private void initNodeInfos() {
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		for (Building building : this.buildings) {
			
			
			if (building.getGeo() != null) {
				Coord coord = MGC.point2Coord(building.getGeo().getCentroid());
				if (this.fis != null) {
					FloodingInfo fi = this.fis.get(coord.getX(), coord.getY());
					if (fi.getCoordinate().distance(building.getGeo().getCentroid().getCoordinate()) > this.scenario.getConfig().evacuation().getBufferSize()) {
						continue;
					}

				}
			}
			
			int numOfPers = getNumOfPersons(building);
			LinkImpl l = network.getNearestLink(MGC.coordinate2Coord(building.getGeo().getCentroid().getCoordinate()));
			Node n = l.getToNode();
			
			//TODO could may be removed if only building within the inundation area are considered
			//FIXME this persons occupy space in shelters anyway, so they have to be considered some how
			if (n.getId().toString().contains("n")) {
				continue;
			}
			this.numOfPers  += numOfPers;
			SourceNodeInfo ni = this.sources.get(n);
			if (ni == null) {
				ni = new SourceNodeInfo();
				ni.node = n;
				this.sources.put(n, ni);
			}
			ni.numPers += numOfPers;
		}
	}

	private void initRouter() {
		FreespeedTravelTimeCost tt = new FreespeedTravelTimeCost(-6,0,0);
		PluggableTravelCostCalculator tc = new PluggableTravelCostCalculator(tt);
    	this.router = new Dijkstra(this.scenario.getNetwork(),tc,tt);
	}

	private static class NodeCostInfo {
		public SinkNodeInfo sni;
		SourceNodeInfo ni;
		double cost;
	}
	
	private static class SourceNodeInfo {
		int numPers = 0;
		Node node;
		
	}
	private static class SinkNodeInfo {
		Node node;
		int capacity;
	}
	
	private static class NodeCostComparator implements Comparator<NodeCostInfo> {

		@Override
		public int compare(NodeCostInfo o1, NodeCostInfo o2) {
			if (o1.cost < o2.cost) {
				return -1;
			}
			
			if (o2.cost < o1.cost) {
				return 1;
			}
			return 0;
		}
		
	}
	
}
