/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.Analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author droeder
 *
 */
public class AgentWaitHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	AgentArrivalEventHandler, AgentDepartureEventHandler{
	
	Network net;
	Map<Id, Double> realTT = new HashMap<Id, Double>();
	double temp;
	
	public AgentWaitHandler(Network net) {
		this.net = net;
	}
	
	@Override
	public void reset(int iteration) {
		realTT.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(realTT.containsKey(event.getPersonId())){
			temp = realTT.get(event.getPersonId())-event.getTime();
			realTT.put(event.getPersonId(), temp);
		}else{
			realTT.put(event.getPersonId(), -event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		temp = realTT.get(event.getPersonId())+event.getTime();
		realTT.put(event.getPersonId(), temp);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		temp = realTT.get(event.getPersonId())+event.getTime();
		realTT.put(event.getPersonId(), temp);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(realTT.containsKey(event.getPersonId())){
			temp = realTT.get(event.getPersonId())-event.getTime();
			realTT.put(event.getPersonId(), temp);
		}else{
			realTT.put(event.getPersonId(), -event.getTime());
		}
	}
	
	private Double getFreePlanTT(Person p){
		Double freePlanTT = 0.0;
		NetworkRoute route = null;
		for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				route = (NetworkRoute) leg.getRoute();
				if (route != null) {
					for(Id id : route.getLinkIds()){
						freePlanTT += net.getLinks().get(id).getLength()/net.getLinks().get(id).getFreespeed();
					}
					if (route.getEndLinkId() != null && route.getStartLinkId() != route.getEndLinkId()) {
						freePlanTT += net.getLinks().get(route.getEndLinkId()).getLength()/net.getLinks().get(route.getEndLinkId()).getFreespeed();
					}
				}
			}
		}
		
		return freePlanTT;
	}
	
	private Double getPersonWaitingFactor(Person p){
		return realTT.get(p.getId())/this.getFreePlanTT(p);
	}
	
	public Double getAverageWaitingFactor(Population pop){
		Double average = 0.0;
		for (Person p : pop.getPersons().values()){
			average += this.getPersonWaitingFactor(p);
		}
		average = average/pop.getPersons().size();
		
		return average;
	}
	
	public Map<String, Double> getFactors(Population pop){
		double median = 0;
		Map<String, Double> stats = new HashMap<String, Double>();
		Double[] factors = new Double[pop.getPersons().size()];
		int i = 0;
		
		for (Person p : pop.getPersons().values()){
			factors[i] = this.getPersonWaitingFactor(p);
			i++;
		}
		Arrays.sort(factors);
		if (factors.length%2.0 == 0.0){
			median = 0.5 * (factors[(factors.length-1)/2] + factors[(factors.length)/2]);
		}else{
			median = factors[(factors.length)/2];
		}
		stats.put("median", median);
		stats.put("min", factors[0]);
		stats.put("max", factors[factors.length-1]);
		
		
		return stats;
	}
	
}
