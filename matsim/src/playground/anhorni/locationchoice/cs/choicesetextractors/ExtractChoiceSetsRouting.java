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

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.router.PlansCalcRoute;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.SpanningTree;
import playground.anhorni.locationchoice.cs.helper.ZHFacilities;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

/**
 * @author anhorni
 */
public class ExtractChoiceSetsRouting extends ChoiceSetExtractor implements AfterMobsimListener  {
	
	private final static Logger log = Logger.getLogger(ExtractChoiceSetsRouting.class);
	private String mode;

	public ExtractChoiceSetsRouting(Controler controler, ZHFacilities facilities, 
			List<ChoiceSet> choiceSets, String mode) {
		
		super(controler, choiceSets);
		super.facilities = facilities;
		this.mode = mode;	
	}
	
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		
		if (event.getIteration() < Gbl.getConfig().controler().getLastIteration()) {
			return;
		}
		log.info("Number of ZH facilities " + this.facilities.getNumberOfFacilities());
		log.info("computing " + this.mode + " choice sets...:");
		super.computeChoiceSets();
	}
				
	protected void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type, 
			Controler controler) {
			
		NetworkLayer network = controler.getNetwork();
				
		Iterator<ZHFacility> facilities_it = this.facilities.getZhFacilities().values().iterator();
		while (facilities_it.hasNext()) {
			ZHFacility facility = facilities_it.next();
			
			Id linkId = facility.getLinkId();
			
			//--------------------------------------------------
			/*
			 * this is NOT working: 
			 * Link linkBefore = choiceSet.getTrip().getBeforeShoppingAct().getLink(); ...
			 */
			//Link linkBefore = network.getNearestLink(choiceSet.getTrip().getBeforeShoppingAct().getLink().getCenter());
			Link linkBefore = network.getLink(choiceSet.getTrip().getBeforeShoppingAct().getLink().getId());
			Act fromAct0 = new Act("beforeShop", linkBefore);
			fromAct0.setEndTime(choiceSet.getTrip().getBeforeShoppingAct().getEndTime());
			fromAct0.setCoord(linkBefore.getCenter());
						
			Link link = network.getLink(linkId);
			Act toAct0 = new Act("shop", link);
			toAct0.setCoord(link.getCenter());
						
			Leg legBefore = computeLeg(fromAct0, toAct0, controler);				
			double travelTimeBeforeShopping = legBefore.getTravelTime();
			
			//--------------------------------------------------			
			Act fromAct1 = new Act(toAct0.getType(), toAct0.getLink());
			double endTime = choiceSet.getTrip().getBeforeShoppingAct().getEndTime() + 
			travelTimeBeforeShopping +
			choiceSet.getTrip().getShoppingAct().calculateDuration();			
			fromAct1.setEndTime(endTime);
			fromAct1.setCoord(toAct0.getCoord());
						
			//Link linkAfter = network.getNearestLink(choiceSet.getTrip().getAfterShoppingAct().getLink().getCenter());
			Link linkAfter = network.getLink(choiceSet.getTrip().getAfterShoppingAct().getLink().getId());
			Act toAct1 = new Act("afterShop", linkAfter);
			toAct1.setCoord(linkAfter.getCenter());
						
			Leg legAfter = computeLeg(fromAct1, toAct1, controler);	
			double travelTimeAfterShopping = legAfter.getTravelTime();
			//--------------------------------------------------
			
			double totalTravelTime = travelTimeBeforeShopping + travelTimeAfterShopping;	
			
			/*
			 * This is NOT working: legBefore.getRoute().getDist() + legAfter.getRoute().getDist()
			 */
			double travelDist = 0.0;
			
			Iterator<Id> routeLinkBefore_it = legBefore.getRoute().getLinkIds().iterator();
			while (routeLinkBefore_it.hasNext()) {		
				Id lId = routeLinkBefore_it.next();
				travelDist += network.getLink(lId).getLength();
			}
			
			Iterator<Id> routeLinkAfter_it = legAfter.getRoute().getLinkIds().iterator();
			while (routeLinkAfter_it.hasNext()) {		
				Id lId = routeLinkAfter_it.next();
				travelDist += network.getLink(lId).getLength();
			}
			if (totalTravelTime <= choiceSet.getTravelTimeBudget() || facility.getId().compareTo(choiceSet.getChosenFacilityId()) == 0) {			
				choiceSet.addFacility(facility, totalTravelTime, travelDist);
			}
		}	
	}
		
	private Leg computeLeg(Act fromAct, Act toAct, Controler controler) {	
		Leg leg = new Leg(BasicLeg.Mode.car);
		PlansCalcRoute router = (PlansCalcRoute)controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());
		
		return leg;
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
	
	
	
	
		private double handleWalkLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// make simple assumption about distance and walking speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 5.0 / 3.6; // 4.0 km/h --> m/s
		// create an empty route, but with realistic travel time
		CarRoute route = new NodeCarRoute(fromAct.getLink(), toAct.getLink());
		int travTime = (int)(dist / speed);
		route.setTravelTime(travTime);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}
	
	
 */
