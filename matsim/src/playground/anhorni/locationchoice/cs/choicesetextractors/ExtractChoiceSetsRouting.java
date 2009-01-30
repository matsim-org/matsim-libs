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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.router.PlansCalcRoute;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.SpanningTree;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

/**
 * @author anhorni
 */
public class ExtractChoiceSetsRouting extends ChoiceSetExtractor implements AfterMobsimListener  {
	
	private final static Logger log = Logger.getLogger(ExtractChoiceSetsRouting.class);
	private String mode;

	public ExtractChoiceSetsRouting(Controler controler, TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink, 
			List<ChoiceSet> choiceSets, String mode) {
		
		super(controler, choiceSets);
		super.zhFacilitiesByLink = zhFacilitiesByLink;
		this.mode = mode;
		
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
		log.info("computing choice sets...:");
		super.computeChoiceSets();
	}
			
	protected void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type, 
			Controler controler) {
			
		NetworkLayer network = controler.getNetwork();
		
		Iterator<Id> link_it = this.zhFacilitiesByLink.keySet().iterator();
		while (link_it.hasNext()) {		
			Id linkId = link_it.next();
			
			Link link = network.getLink(linkId);
			
			Act fromAct = choiceSet.getTrip().getBeforeShoppingAct();
			Act toAct = new Act("shop", link);	
			
			double travelTimeBeforeShopping = computeTravelTime(fromAct, toAct, controler);
			
			fromAct = toAct;
			fromAct.setEndTime(choiceSet.getTrip().getBeforeShoppingAct().getEndTime() + travelTimeBeforeShopping +
					choiceSet.getTrip().getShoppingAct().getDuration());
			toAct = choiceSet.getTrip().getAfterShoppingAct();	
			
			double travelTimeAfterShopping = computeTravelTime(fromAct, toAct, controler);			
			double totalTravelTime = travelTimeBeforeShopping + travelTimeAfterShopping;
			
			if (totalTravelTime <= choiceSet.getTravelTimeBudget()) {			
				choiceSet.addFacilities(this.zhFacilitiesByLink.get(linkId), totalTravelTime);
			}
			
		}		
	}
	
	
	private double computeTravelTime(Act fromAct, Act toAct, Controler controler) {	
		Leg leg = null;
		if (this.mode.equals("car")) {
			leg = new Leg(BasicLeg.Mode.car);
		}
		else if (this.mode.equals("walk")) {
			leg = new Leg(BasicLeg.Mode.walk);
		}
		/*
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);
		*/
		
		PlansCalcRoute router = (PlansCalcRoute)controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravelTime();
	}	
}


/* not using spanning tree at the moment: 
 * 
 * protected void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type, 
			Controler controler) {
	
		NetworkLayer network = controler.getNetwork();
				
		spanningTree.setOrigin(network.getNearestNode(choiceSet.getTrip().getBeforeShoppingAct().getCoord()));
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
 */
