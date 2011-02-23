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
package playground.droeder.Journal2MATSim2Journal;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author droeder
 *
 */
public class TripDurationHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler,
											ActivityEndEventHandler, ActivityStartEventHandler{

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void handleEvent(AgentArrivalEvent e) {
		// TODO Auto-generated method stub
		if(e.getPersonId().equals(new IdImpl("10_car")) || e.getPersonId().equals(new IdImpl("10_pt"))){
			System.out.println(e.toString());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent e) {
		// TODO Auto-generated method stub
		if(e.getPersonId().equals(new IdImpl("10_car")) || e.getPersonId().equals(new IdImpl("10_pt"))){
			System.out.println(e.toString());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub
		
	}

}

class AnalysisPopulation{
	private Map<Id, Plan> agentId2Plan;
	private Map<Id, PlanElement> agentId2actualPlanElement;
	
	private PopulationFactory fac;
	
	public AnalysisPopulation(){
		this.agentId2Plan = new HashMap<Id, Plan>();
		this.agentId2actualPlanElement = new HashMap<Id, PlanElement>();
		
		this.fac = new ScenarioImpl().getPopulation().getFactory();
	}
	
	public void processAgentEvent(AgentEvent e){
		if(!this.agentId2Plan.containsKey(e.getPersonId())){
			this.agentId2Plan.put(e.getPersonId(), fac.createPlan());
		}
		
		if(e instanceof AgentDepartureEvent){
		
		}else{
			
		}
	}
	
	public void processActivityEvent(ActivityEvent e){
		if(!this.agentId2Plan.containsKey(e.getPersonId())){
			this.agentId2Plan.put(e.getPersonId(), fac.createPlan());
		}
		
		if(e instanceof ActivityEndEvent){
			
		}else{
			
		}
	}
	
}


class Trip{
	
	private static final Logger log = Logger.getLogger(Trip.class);
	
	private boolean finished = false;
	TransportMode mode;
	Integer changesPt = 0;
	Double accesTime = 0.0;
	Double travelTime = 0.0;
	Double startTime = null;
	Double endTime = null;
	
	public void setLegMode(TransportMode mode){
		this.mode = mode;
	}
	
	public TransportMode getTranportMode(){
		return this.mode;
	}
	
	public void ChangesPt(){
		this.changesPt++;
	}
	public Integer getPtChanges(){
		return this.changesPt;
	}
	
	public void setFinished(){
		this.finished= true;
	}
	public boolean finished(){
		return this.finished;
	}
	
	public void tripStartTime(Double startTime){
		this.startTime = startTime;
	}
	
	public void tripEndTime(Double endTime){
		this.endTime = endTime;
	}
	
	public double getTravelTime(){
		if(this.endTime == null){
			log.error("at least no TripEndTime defined!!!");
			return this.travelTime;
		}
		
		this.travelTime = this.endTime - this.startTime; 
		return this.travelTime;
	}
	
}