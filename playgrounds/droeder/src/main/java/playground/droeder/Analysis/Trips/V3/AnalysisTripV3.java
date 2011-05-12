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
package playground.droeder.Analysis.Trips.V3;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;

/**
 * @author droeder
 *
 */
public class AnalysisTripV3 extends AbstractAnalysisTrip {
	
//	private static final Logger log = Logger.getLogger(AnalysisTripV3.class);
	
	private Integer nrOfExpEvents = null;
	private Integer nrOfElements = 0;
	
	public AnalysisTripV3(){
		
	}
	
	@Override
	public void addElements(ArrayList<PlanElement> elements){
		this.nrOfElements = elements.size();
		this.nrOfExpEvents = this.findExpectedNumberOfEvents(elements);
		super.addElements(elements);
	}
	
	public Integer getNrOfElements(){
		return this.nrOfElements;
	}
	
	private int findExpectedNumberOfEvents(ArrayList<PlanElement> elements){
		int temp = 0;
		for(PlanElement pe: elements){
			if( pe instanceof Leg){
				
				if(((Leg) pe).getMode().equals(TransportMode.pt)){
					temp +=4;
				}else{
					temp +=2;
				}
			}
		}
		return temp;
	}
	
	public int getNumberOfExpectedEvents(){
		return this.nrOfExpEvents;
	}

	public void addEvents(ArrayList<PersonEvent> events) {
		super.tripTTime = events.get(events.size()-1).getTime() - events.get(0).getTime();
		if(super.getMode().equals(TransportMode.pt)){
			TravelTimeHandler handler = new TravelTimeHandler(this);
			for(PersonEvent pe: events){
				handler.processEvent(pe);
			}
		}
	}
	
}

class TravelTimeHandler{
	private static final Logger log = Logger.getLogger(TravelTimeHandler.class);
	
	private AnalysisTripV3 trip;
	private String expEvent = AgentDepartureEventImpl.class.toString();
	private boolean warn = true;
	
	public TravelTimeHandler(AnalysisTripV3 trip){
		this.trip = trip;
	}

	public void processEvent(PersonEvent e) {
		if(warn && !e.getClass().toString().equals(this.expEvent)){
			log.warn("got unexpected Event! Check results! Message thrown only once!");
			this.warn = false;
		}
		
		this.expEvent = this.getNextExpEvent(e);
		
		if(e instanceof AgentDepartureEvent){
			this.process((AgentDepartureEvent) e);
		}else if(e instanceof AgentArrivalEvent){
			this.process((AgentArrivalEvent) e);
		}else if(e instanceof PersonEntersVehicleEvent){
			this.process((PersonEntersVehicleEvent)e);
		}else if(e instanceof PersonLeavesVehicleEvent){
			this.process((PersonLeavesVehicleEvent)e);
		}
	}
	
	
	private boolean accesWalk = true;
	private boolean accesWait = true;
//	private boolean switchWait = false;
	//TODO switchWait
	private void process(AgentDepartureEvent e){
		if(accesWalk){
			this.trip.accesWalkTTime -= e.getTime();
		}else if(e.getLegMode().equals(TransportMode.transit_walk)){
			if(this.trip.egressWalkTTime > 0){
				this.trip.switchWalkTTime += this.trip.egressWalkTTime;
				this.trip.switchWalkCnt++;
				this.trip.egressWalkCnt--;
			}
			this.trip.egressWalkTTime = -e.getTime();
		}
	}
	
	private void process(AgentArrivalEvent e){
		if(accesWalk){
			this.trip.accesWalkTTime += e.getTime();
			this.trip.accesWalkCnt++;
			this.accesWalk = false;
		}else if(e.getLegMode().equals(TransportMode.transit_walk)){
			this.trip.egressWalkTTime += e.getTime();
			this.trip.egressWalkCnt++;
		}
		if(accesWait){
			this.trip.accesWaitTime -= e.getTime();
		}
	}
	
	private void process(PersonEntersVehicleEvent e){
		if(accesWait){
			accesWait = false;
			this.trip.accesWaitTime += e.getTime();
			if(this.trip.getAccesWaitTime() > 0){
				this.trip.accesWaitCnt++;
			}
		}
		this.trip.lineTTime -=e.getTime();
	}
	
	private void process(PersonLeavesVehicleEvent e){
	
		this.trip.lineTTime +=e.getTime();
		this.trip.lineCnt++;
		
	}
	
	private String getNextExpEvent(PersonEvent e){
		if(e instanceof AgentDepartureEvent){
			if(((AgentDepartureEvent) e).getLegMode().equals(TransportMode.pt)){
				return PersonEntersVehicleEventImpl.class.toString();
			}else{
				return AgentArrivalEventImpl.class.toString();
			}
		}else if(e instanceof AgentArrivalEvent){
			return AgentDepartureEventImpl.class.toString();
		}else if(e instanceof PersonEntersVehicleEvent){
			return PersonLeavesVehicleEventImpl.class.toString();
		}else if(e instanceof PersonLeavesVehicleEvent){
			return AgentArrivalEventImpl.class.toString();
		}else{
			return "false";
		}
	}
}

