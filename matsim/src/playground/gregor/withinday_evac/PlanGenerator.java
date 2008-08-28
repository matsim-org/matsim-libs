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

package playground.gregor.withinday_evac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

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
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;

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
		int count = 0;
		for (Entry<String,ArrayList<String>> e : this.traces.entrySet()) {
			Person pers = this.population.getPerson(e.getKey());
			Plan plan = pers.getSelectedPlan();
			Link[] links = plan.getNextLeg(plan.getFirstActivity()).getRoute().getLinkRoute();
			ArrayList<String> strLinks = e.getValue();
			if (strLinks.size() < links.length) {
				if (addNewPlan(pers,strLinks)) count++;
				continue;
			}
			
			for (int i = 0; i < links.length; i++) {
				if (!links[i].getId().toString().equals(strLinks.get(i))) {
					if (addNewPlan(pers,strLinks)) count++;
					break;
				}
			}
		}
System.out.println(count);
	}


	private boolean addNewPlan(final Person pers, final ArrayList<String> strLinks) {
		
//		HashSet<String> added = new HashSet<String>();
//		for (String linkId : strLinks) {
//			if (added.contains(linkId)){
//				return false;
//			}
//			added.add(linkId);
//		}
//		
		
		
		ArrayList<Node> nodes = new ArrayList<Node>();
//		HashSet<Node> test = new HashSet<Node>();

		for (String linkId : strLinks) {
//			if (test.contains(this.network.getLink(linkId).getFromNode())) {
//				return false;
//			}
			nodes.add(this.network.getLink(linkId).getFromNode());
		}

//		nodes.add(this.network.getLink(strLinks.get(strLinks.size()-1)).getToNode());
		pers.removeWorstPlans(this.maxPlans-1);
		Plan plan = pers.copySelectedPlan();
		Act actA = plan.getFirstActivity();
		Leg leg = plan.getNextLeg(actA);
		Route route = new Route();
		route.setRoute(nodes);
		route.getDist();
		leg.setRoute(route);
		plan.setScore(Plan.UNDEF_SCORE);
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
