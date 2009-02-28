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

import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.population.routes.NodeCarRoute;

import playground.gregor.withindayevac.debug.DebugDecisionTree;
import playground.gregor.withindayevac.debug.DebugFollowFastestAgent;

public class PlanGenerator implements StartupListener, BeforeMobsimListener, AfterMobsimListener,
AgentStuckEventHandler, LinkEnterEventHandler{

	private Population population;
	HashMap<String,ArrayList<String>> traces = new HashMap<String, ArrayList<String>>();
	private int maxPlans;
	private NetworkLayer network;

	public void notifyStartup(final StartupEvent event) {
		event.getControler().getEvents().addHandler(this);
		this.maxPlans = event.getControler().getConfig().strategy().getMaxAgentPlanMemorySize();
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
		
			List<Link> links = ((CarRoute) plan.getNextLeg(plan.getFirstActivity()).getRoute()).getLinks();
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
		Plan plan = new org.matsim.population.PlanImpl(pers);
		Act oldA = pers.getSelectedPlan().getFirstActivity();
		Act a = new org.matsim.population.ActImpl(oldA);
		a.setType("h");
		Leg oldLeg = pers.getSelectedPlan().getNextLeg(oldA);
		Leg l = new org.matsim.population.LegImpl(oldLeg.getMode());
		l.setNum(oldLeg.getNum());
		l.setDepartureTime(oldLeg.getDepartureTime());
		l.setTravelTime(oldLeg.getTravelTime());
		l.setArrivalTime(oldLeg.getArrivalTime());
		
		Act oldB = pers.getSelectedPlan().getNextActivity(oldLeg);
		Act b = new org.matsim.population.ActImpl(oldB);
		plan.addAct(a);
		CarRoute route = new NodeCarRoute();
		route.setNodes(nodes);
		route.getDist();
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
		this.traces.remove(event.agentId);	
	}


	public void handleEvent(final LinkEnterEvent event) {
		ArrayList<String> links = this.traces.get(event.agentId);
		if (links == null) {
			links = new ArrayList<String>();
			this.traces.put(event.agentId, links);
		}
		links.add(event.linkId);
	}

}
