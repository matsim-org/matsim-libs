/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPlansGeneratorAndNetworkTrimmer.java
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

package playground.gregor.sims.evacbase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.population.BasicLeg;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCost;
import org.matsim.utils.geometry.CoordImpl;

/**
 *@author glaemmel
 */
public class EvacuationPlansGeneratorAndNetworkTrimmer {

	private final static Logger log = Logger.getLogger(EvacuationPlansGeneratorAndNetworkTrimmer.class);

	//evacuation Nodes an Link
	private final static String saveLinkId = "el1";
	private final static String saveNodeAId = "en1";
	private final static String saveNodeBId = "en2";

	//	the positions of the evacuation nodes - for now hard coded
	// Since the real positions of this nodes not really matters
	// and for the moment we are going to evacuate Padang only,
	// the save nodes are located east of the city.
	// Doing so, the visualization of the resulting evacuation network is much clearer in respect of coinciding links.
	private final static String saveAX = "662433";
	private final static String saveAY = "9898853";
	private final static String saveBX = "662433";
	private final static String saveBY = "9898853";
	
	private HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
	private final HashSet<Node> saveNodes = new HashSet<Node>();
	private final HashSet<Node> redundantNodes = new HashSet<Node>();

	private TravelCost tc = null;

	/**
	 * Generates an evacuation plan for all agents inside the evacuation area.
	 * Agents outside the evacuation are will be removed from the plans.
	 *
	 * @param plans
	 * @param network
	 */
	public void createEvacuationPlans(final Population plans, final NetworkLayer network) {
		PlansCalcRoute router;
		if (this.tc != null) {
			router = new PlansCalcRoute(network, this.tc, new FreespeedTravelTimeCost());
		} else {
			router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());	
		}

		/* all persons that want to start on an already deleted link will be excluded from the
		 *simulation.     */
		log.info("  - removing all persons outside the evacuation area");
		Iterator<Person> it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((Activity)pers.getPlans().get(0).getPlanElements().get(0)).getLink().getId();

			if (network.getLink(id) == null) {
				it.remove();
			}
		}

		// the remaining persons plans will be routed
		log.info("  - generating evacuation plans for the remaining persons");
		final Coord saveCoord = new CoordImpl(12000.0, -12000.0);
		final Link saveLink = network.getLink(saveLinkId);
		for (Person person : plans.getPersons().values()) {
			if (person.getPlans().size() != 1 ) {
				throw new RuntimeException("For each agent only one initial evacuation plan is allowed!");
			}

			Plan plan = person.getPlans().get(0);

			if (plan.getPlanElements().size() != 1 ) {
				throw new RuntimeException("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			}
			
			Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			plan.addLeg(leg);

			plan.addAct(new org.matsim.population.ActivityImpl("h", saveCoord, saveLink));

			router.run(plan);
		}

	}

	/**
	 * Creates links from all save nodes to the evacuation node A
	 *
	 * @param network
	 */
	private void createEvacuationLinks(final NetworkLayer network) {

		network.createNode(new IdImpl(saveNodeAId), new CoordImpl(saveAX, saveAY));
		network.createNode(new IdImpl(saveNodeBId), new CoordImpl(saveBX, saveBY));

		/* TODO [GL] the capacity of the evacuation links should be a very high value but for unknown reason Double.MAX_VALUE
		 * does not work, may be the better solution will be to implement a method in QueueLink to set the spaceCap to infinity
		 *anyway, this solution is just a workaround the spaceCap problem should be solved in an other way - gl */
		double capacity = Double.parseDouble("99999999999999999999"); // (new Double (Double.MAX_VALUE)).toString();
		network.createLink(new IdImpl(saveLinkId), network.getNode(saveNodeAId), network.getNode(saveNodeBId), 10, 100000, capacity, 1);

		int linkId = 1;
		for (Node node : network.getNodes().values()) {
			String nodeId =  node.getId().toString();
			if (isSaveNode(node) && !nodeId.equals(saveNodeAId) && !nodeId.equals(saveNodeBId)){
				linkId++;
				String sLinkID = "el" + Integer.toString(linkId);
				network.createLink(new IdImpl(sLinkID), network.getNode(nodeId), network.getNode(saveNodeAId), 10, 100000, capacity, 1);
			}
		}
	}

	/**
	 * @param node
	 * @return true if <code>node</node> is outside the evacuation area
	 */
	private boolean isSaveNode(final Node node) {
		return this.saveNodes.contains(node);
	}

	/**
	 * Returns true if <code>node</code> is redundant. A node is
	 * redundant if it is not next to the evacuation area.
	 *
	 * @param node
	 * @return true if <code>node</code> is redundant.
	 */
	private boolean isRedundantNode(final Node node) {
		return this.redundantNodes.contains(node);
	}


	public void generatePlans(final Population plans, final NetworkLayer network, final HashMap<Id, EvacuationAreaLink> evacuationAreaLinks) {
		this.evacuationAreaLinks = evacuationAreaLinks;
		log.info("generating evacuation plans ...");
		log.info(" * classifing nodes");
		classifyNodes(network);
		log.info(" * cleaning up the network");
		cleanUpNetwork(network);
		log.info(" * creating evacuation links");
		createEvacuationLinks(network);
		log.info(" * creating evacuation plans");
		createEvacuationPlans(plans,network);
		log.info("done");
	}

	/**
	 * Classifies the nodes. Nodes that are next to the evacuation area and
	 * reachable from inside the evacuation area will be classified as save
	 * nodes. Other nodes outside the evacuation area will be classified
	 * as redundant nodes.
	 *
	 * @param network
	 */
	private void classifyNodes(final NetworkLayer network) {
		/* classes:
		 * 0: default, assume redundant
		 * 1: redundant node
		 * 2: save nodes, can be reached from evacuation area
		 * 3: "normal" nodes within the evacuation area
		 */
		for (Node node : network.getNodes().values()) {
			int inCat = 0;
			for (Link link : node.getInLinks().values()) {
				if (this.evacuationAreaLinks.containsKey(link.getId())) {
					if ((inCat == 0) || (inCat == 3)) {
						inCat = 3;
					}	else {
						inCat = 2;
						break;
					}
				} else {
					if (inCat <= 1) {
						inCat = 1;
					} else {
						inCat = 2;
						break;
					}
				}
			}
			switch (inCat) {
				case 2:
					this.saveNodes.add(node);
					break;
				case 3:
					break;
				case 1:
				default:
					this.redundantNodes.add(node);
			}
		}

	}

	/**
	 * Removes all links and nodes outside the evacuation area except the
	 * nodes next to the evacuation area that are reachable from inside the
	 * evacuation area ("save nodes").
	 *
	 * @param network
	 */
	private void cleanUpNetwork(final NetworkLayer network) {

		ConcurrentLinkedQueue<Link> l = new ConcurrentLinkedQueue<Link>();
		for (Link link : network.getLinks().values()) {
			if (!this.evacuationAreaLinks.containsKey(link.getId())) {
				l.add(link);
			}
		}

		Link link = l.poll();
		while (link != null){
			network.removeLink(link);
			link = l.poll();
		}

		ConcurrentLinkedQueue<Node> n = new ConcurrentLinkedQueue<Node>();
		for (Node node : network.getNodes().values()) {
			if (isRedundantNode(node)) {
				n.add(node);
			}
		}

		Node node = n.poll();
		while (node != null) {
			network.removeNode(node);
			node = n.poll();
		}
		new NetworkCleaner().run(network);
	}

	/**
	 * This method allows to set a travel cost calculator. If not set a free speed travel cost calculator
	 * will be instantiated automatically  
	 * @param tc
	 */
	public void setTravelCostCalculator(final TravelCost tc) {
		this.tc  = tc;
	}

}
