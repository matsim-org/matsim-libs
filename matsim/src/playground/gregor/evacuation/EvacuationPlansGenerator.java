/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPlansGenerator.java
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

package playground.gregor.evacuation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

public class EvacuationPlansGenerator {
	
	
	//evacuation Nodes an Link
	private final static String saveLinkId = "el1";
	private final static String saveNodeAId = "en1"; 
	private final static String saveNodeBId = "en2";
	
	
	//	the positions of the evacuation nodes - for now hardcoded
	private final static String saveAX = "0";
	private final static String saveAY = "0";
	private final static String saveBX = "0";
	private final static String saveBY = "0";	
	
	
	private HashMap<Id,EvacuationAreaLink> desasterAreaLinks = new HashMap<Id,EvacuationAreaLink>();
	
	public EvacuationPlansGenerator(){
		
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	//this method generates a evacuation plan for all agents
	//////////////////////////////////////////////////////////////////////	
	public void createEvacuationPlans(Plans plans, NetworkLayer network) {
		
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		PlansCalcRoute router = new PlansCalcRoute(network, timeCostCalc, timeCostCalc, false);
		//PlansCalcRoute router = new PlansCalcRoute(network,new TravelDistanceCost(),false);
		
		Iterator it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = (Person)it.next();
			
			if (pers.getPlans().size() != 1 )
				Gbl.errorMsg("For each agent only one initial evacuation plan is allowed!");
			
			Plan old_plan = pers.getPlans().get(0);
			Plan new_plan = new Plan(pers);
			
			
			if (old_plan.getActsLegs().size() != 1 )
				Gbl.errorMsg("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			
			Act old_act = (Act)old_plan.getActsLegs().get(0);
			
			//only persons within the desaster area should get new plans
			//TODO may be it is better to eliminate such persons from the simulation
			if (!desasterAreaLinks.containsKey(old_act.getLink().getId())) continue;
			
			
			Act actA = new Act("h",old_act.getCoord().getX(),old_act.getCoord().getX(),old_act.getLink(),0.0,old_act.getEndTime(),0.0,true);
			new_plan.addAct(actA);
			
			Leg leg = new Leg(1,"car",0.0,0.0,0.0);
			new_plan.addLeg(leg);
			
			Act actB = new Act("h",12000.0, -12000.0,network.getLink(saveLinkId),0.0,0.0,0.0,true);
			new_plan.addAct(actB);
			
			
			router.run(new_plan);
			
			pers.addPlan(new_plan);
			pers.getPlans().remove(old_plan);
			pers.setSelectedPlan(new_plan);

		}
	}
	
	
	
	
	
	//////////////////////////////////////////////////////////////////////
	//this method create a link from all save nodes to the evacuation
	//node A and creates the save nodes itself
	//////////////////////////////////////////////////////////////////////
	private void createEvacuationLinks(NetworkLayer network) {
		
		Collection<QueueNode> nodes = network.getNodes();
		
		network.createNode(saveNodeAId,saveAX, saveAY,"QueueNode");
		network.createNode(saveNodeBId,saveBX, saveBY,"QueueNode");
		String capacity = (new Double (Double.MAX_VALUE)).toString();
		network.createLink(saveLinkId,saveNodeAId,saveNodeBId,"0", "100000",capacity,"1",null,null);
		
		int linkId = 1;
		for (QueueNode node : nodes){
			String nodeId =  node.getId().toString();
			if (isSaveNode(node) && !nodeId.equals(saveNodeAId) && !nodeId.equals(saveNodeBId)){
				linkId++;
				String sLinkID = "el" + (new Integer(linkId)).toString();
				network.createLink(sLinkID,nodeId,saveNodeAId,"0","100000",capacity,"1",null,null);
			}
			
		}
	}
	
	
	//////////////////////////////////////////////////////////////////////
	//returns true if a node is outside the desaster area
	//////////////////////////////////////////////////////////////////////	
	private boolean isSaveNode(QueueNode node) {
		
		Iterator it = node.getInLinks().iterator();
		while (it.hasNext()){
			QueueLink link = (QueueLink)it.next();
			if (desasterAreaLinks.containsKey(link.getId())){
				return false;
			}
		}
	
		return true;
	}

	public void generatePlans(Plans plans, NetworkLayer network, HashMap<Id,EvacuationAreaLink> desasterAreaLinks) {
		this.desasterAreaLinks = desasterAreaLinks;
		createEvacuationLinks(network);
		createEvacuationPlans(plans,network);
		
	}
	
}
