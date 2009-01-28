/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractChoiceSets.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.cs.choicesetextractors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.router.PlansCalcRoute;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.SpanningTree;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

/**
 * @author anhorni
 */
public class ExtractCarChoiceSets extends ChoiceSetExtractor implements AfterMobsimListener  {
	
	private final static Logger log = Logger.getLogger(ExtractCarChoiceSets.class);

	public ExtractCarChoiceSets(Controler controler, TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink, 
			List<ChoiceSet> choiceSets) {
		
		super(controler, choiceSets);
		super.zhFacilitiesByLink = zhFacilitiesByLink;
		
	}
	
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		
		if (event.getIteration() < Gbl.getConfig().controler().getLastIteration()) {
			return;
		}
		
		int numberOfFacilities = 0;
		Iterator<ArrayList<ZHFacility>> it = super.zhFacilitiesByLink.values().iterator();
		while (it.hasNext()) {
			numberOfFacilities += it.next().size();
		}
		log.info("Number of ZH facilities " + numberOfFacilities);
		log.info("computing car choice sets...:");
		super.computeChoiceSets();
	}
	
		
	protected void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type, 
			Controler controler) {
	
		NetworkLayer network = controler.getNetwork();
				
		Node referenceNode = network.getNearestNode(choiceSet.getReferencePoint());
		spanningTree.setOrigin(referenceNode);
		spanningTree.setDepartureTime(choiceSet.getTrip().getBeforeShoppingAct().getEndTime());
		spanningTree.run(network);
		List<Node> nodesList = new Vector<Node>();
		List<Double> nodesTravelTimesList = new Vector<Double>();					
		spanningTree.getNodesByTravelTimeBudget(choiceSet.getTravelTimeBudget(), nodesList, nodesTravelTimesList);
		
		int index = 0;
		Iterator<Node> nodes_it = nodesList.iterator();
		while (nodes_it.hasNext()) {		
			Node node = nodes_it.next();
			Map<Id, ? extends Link> linksList = node.getIncidentLinks();
			Iterator<? extends Link> links_it = linksList.values().iterator();
			while (links_it.hasNext()) {
				nodesTravelTimesList.get(index);
				Link link = links_it.next();
				// only one link per facility
				
				ArrayList<ZHFacility> facilities = 
					(ArrayList<ZHFacility>)this.zhFacilitiesByLink.get(link.getId());
				if (facilities != null) {
					
					// calculate travel time to after shopping location
					double travelTime2AfterShopingAct = this.getTravelTime(link, nodesTravelTimesList.get(index), 
							controler, choiceSet);
					
					double totalTravelTime = nodesTravelTimesList.get(index) + travelTime2AfterShopingAct;
					
					if (totalTravelTime <= choiceSet.getTravelTimeBudget()) {
						choiceSet.addFacilities(facilities, totalTravelTime);
					}
				}
			}
			index++;
		}		
	}
	
	double getTravelTime(Link link, double usedTravelTime, 
			Controler controler, ChoiceSet choiceSet) {
		
		Act fromAct = new Act("shop", link);
		fromAct.setCoord(link.getCenter());		
		double startTime = choiceSet.getTrip().getBeforeShoppingAct().getEndTime() + usedTravelTime;
		fromAct.setStartTime(startTime);	
		double shopDuration = choiceSet.getTrip().getShoppingAct().getDuration();
		fromAct.setEndTime(startTime + shopDuration);
		fromAct.setDuration(shopDuration);
		fromAct.setLinkId(link.getId());
		
		Act toAct = new Act("aftershop", controler.getNetwork().getNearestLink(choiceSet.getTrip().getAfterShoppingAct().getCoord()));
		toAct.setCoord(choiceSet.getTrip().getAfterShoppingAct().getCoord());		
		toAct.setStartTime(choiceSet.getTrip().getAfterShoppingAct().getStartTime());	
		toAct.setEndTime(choiceSet.getTrip().getAfterShoppingAct().getEndTime());
		toAct.setDuration(choiceSet.getTrip().getAfterShoppingAct().getDuration());
		toAct.setLinkId(controler.getNetwork().getNearestLink(choiceSet.getTrip().getAfterShoppingAct().getCoord()).getId());
		
		double travelTimeShop2AfterShopAct = this.computeTravelTime(fromAct, choiceSet.getTrip().getAfterShoppingAct(), controler);	
		
				
		return travelTimeShop2AfterShopAct; 		
	}
	
	private double computeTravelTime(Act fromAct, Act toAct, Controler controler) {	
		Leg leg = new Leg(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);
		
		PlansCalcRoute router = (PlansCalcRoute)controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravelTime();
	}	
}
