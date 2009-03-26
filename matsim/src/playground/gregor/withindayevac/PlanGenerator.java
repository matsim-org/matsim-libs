/* *********************************************************************** *
 * project: org.matsim.*
 * PlanGenerator.java
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

package playground.gregor.withindayevac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NodeNetworkRoute;

import playground.gregor.withindayevac.debug.DebugDecisionTree;
import playground.gregor.withindayevac.debug.DebugFollowFastestAgent;

public class PlanGenerator implements StartupListener, BeforeMobsimListener, AfterMobsimListener,
AgentStuckEventHandler, LinkEnterEventHandler{

	private Population population;
	HashMap<String,ArrayList<String>> traces = new HashMap<String, ArrayList<String>>();
	private NetworkLayer network;

	public void notifyStartup(final StartupEvent event) {
		event.getControler().getEvents().addHandler(this);
		this.population = event.getControler().getPopulation();
		this.network = event.getControler().getNetwork();
	}

	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		this.traces.clear();
	}

	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		//DEBUG
		DebugDecisionTree.print();
		DebugDecisionTree.reset();
		DebugFollowFastestAgent.print();
		DebugFollowFastestAgent.reset();
		
		int count = 0;
		for (Entry<String,ArrayList<String>> e : this.traces.entrySet()) {
			Person pers = this.population.getPerson(new IdImpl(e.getKey()));
			Plan plan = pers.getSelectedPlan();
		
			List<Link> links = ((NetworkRoute) plan.getNextLeg(plan.getFirstActivity()).getRoute()).getLinks();
			ArrayList<String> strLinks = e.getValue();
			if (strLinks.size() < links.size()) {
				if (addNewPlan(pers,strLinks)) count++;
				continue;
			}
			
			for (int i = 0; i < links.size(); i++) {
				if (!links.get(i).getId().toString().equals(strLinks.get(i))) {
					if (addNewPlan(pers,strLinks)) count++;
					break;
				}
			}
		}
		System.out.println(count);
	}


	private boolean addNewPlan(final Person pers, final ArrayList<String> strLinks) {
		

		
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		HashSet<Node> added = new HashSet<Node>();

		for (String linkId : strLinks) {
			Node node = this.network.getLink(linkId).getFromNode();
			if (added.contains(node)) {
				return false;
			}
			added.add(node);
			nodes.add(node);
		}

//		nodes.add(this.network.getLink(strLinks.get(strLinks.size()-1)).getToNode());
		
//		pers.removeWorstPlans(this.maxPlans-1);
		Plan plan = new org.matsim.core.population.PlanImpl(pers);
		Activity oldA = pers.getSelectedPlan().getFirstActivity();
		Activity a = new org.matsim.core.population.ActivityImpl(oldA);
		a.setType("h");
		Leg oldLeg = pers.getSelectedPlan().getNextLeg(oldA);
		Leg l = new org.matsim.core.population.LegImpl(oldLeg.getMode());
		l.setDepartureTime(oldLeg.getDepartureTime());
		l.setTravelTime(oldLeg.getTravelTime());
		l.setArrivalTime(oldLeg.getArrivalTime());
		
		Activity oldB = pers.getSelectedPlan().getNextActivity(oldLeg);
		Activity b = new org.matsim.core.population.ActivityImpl(oldB);
		plan.addAct(a);
		NetworkRoute route = new NodeNetworkRoute();
		route.setNodes(nodes);
		route.getDistance();
		l.setRoute(route);
		plan.addLeg(l);
		plan.addAct(b);
		plan.setScore(Plan.UNDEF_SCORE);
		
		pers.removeWorstPlans(Gbl.getConfig().strategy().getMaxAgentPlanMemorySize()-1);
		pers.exchangeSelectedPlan(plan, true);
		return true;
	}


	public void reset(final int iteration) {
		// TODO Auto-generated method stub

	}

	public void handleEvent(final AgentStuckEvent event) {
		this.traces.remove(event.getPersonId().toString());	
	}


	public void handleEvent(final LinkEnterEvent event) {
		ArrayList<String> links = this.traces.get(event.getPersonId().toString());
		if (links == null) {
			links = new ArrayList<String>();
			this.traces.put(event.getPersonId().toString(), links);
		}
		links.add(event.getLinkId().toString());
	}

}
