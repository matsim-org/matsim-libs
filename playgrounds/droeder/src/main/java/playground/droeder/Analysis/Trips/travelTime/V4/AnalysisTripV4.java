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
package playground.droeder.Analysis.Trips.travelTime.V4;

import java.util.ArrayList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.droeder.Analysis.Trips.travelTime.AbstractAnalysisTrip;

/**
 * @author droeder
 *
 */
public class AnalysisTripV4 extends AbstractAnalysisTrip {
	
//	private static final Logger log = Logger.getLogger(AnalysisTripV3.class);
	
	private Integer nrOfExpEvents = null;
	private Integer nrOfElements = 0;
	private PtTimeHandler handler;
	
	public AnalysisTripV4(){
		
	}
	
	@Override
	public void addElements(ArrayList<PlanElement> elements){
		this.nrOfElements = elements.size();
		this.nrOfExpEvents = this.findExpectedNumberOfEvents(elements);
		super.addElements(elements);
		
		//handler is only needed for pt
		if(this.mode.equals(TransportMode.pt)){
			this.handler = new PtTimeHandler();
		}
	}
	
	public Integer getNrOfElements(){
		return this.nrOfElements;
	}
	
	private int findExpectedNumberOfEvents(ArrayList<PlanElement> elements){
		int temp = 0;
		for(PlanElement pe: elements){
			if( pe instanceof Leg){
				// +4 for every pt-leg
				if(((Leg) pe).getMode().equals(TransportMode.pt)){
					temp +=4;
				}
				// +2 for every other leg
				else{
					temp +=2;
				}
			}
		}
		return temp;
	}

	private int handledEvents = 0;
	private Double first = null;
	private Double last = 0.0;
	/**
	 * returns true if enough events are handled and the trip is finished
	 * @param e
	 * @return
	 */
	public boolean handleEvent(PersonEvent e){
		this.handledEvents++;
		if(this.mode.equals(TransportMode.pt)){
			handler.handleEvent(e);
			if(this.handledEvents == this.nrOfExpEvents){
				handler.finish(this);
				return true;
			}else{
				return false;
			}
		}else{
			if(first == null){
				first = e.getTime();
			}else{
				last = e.getTime();
			}
			
			if(this.handledEvents == nrOfExpEvents){
				this.tripTTime = last - first;
				return true;
			}else{
				return false;
			}
		}
		
	}
	
}
