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
package playground.andreas.aas.modules.ptTripAnalysis.traveltime.V4;

import java.util.ArrayList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.Event;

import playground.andreas.aas.modules.ptTripAnalysis.traveltime.AbstractTTAnalysisTrip;

/**
 * @author droeder
 *
 */
public class TTAnalysisTripV4 extends AbstractTTAnalysisTrip {
	
//	private static final Logger log = Logger.getLogger(AnalysisTripV3.class);
	
	private Integer nrOfExpEvents = null;
	private Integer nrOfElements = 0;
	private PtTimeHandler handler;
	
	public TTAnalysisTripV4(){
		
	}
	
	// TODO[dr] this need to be the elements of the executed plan, I think...
	@Override
	public void addElements(ArrayList<PlanElement> elements){
		this.nrOfElements = elements.size();
		this.nrOfExpEvents = this.findExpectedNumberOfEvents(elements);
		super.addElements(elements);
		
		//handler is only needed for pt
		if(super.getMode().equals(TransportMode.pt)){
			this.handler = new PtTimeHandler();
		}
	}
	
	public Integer getNrOfElements(){
		return this.nrOfElements;
	}
	
	// TODO[dr] adapt changes in MATSim-core. There are a lot more thrown than in the past
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
	public boolean handleEvent(Event e){
		this.handledEvents++;
		// TODO[dr] do this for all modes in pt
		if(super.getMode().equals(TransportMode.pt)){
			handler.handleEvent(e);
			if(this.handledEvents == this.nrOfExpEvents){
				handler.finish(this);
				return true;
			}else{
				return false;
			}
		}else{
			// TODO[dr] should still work, but check...
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
