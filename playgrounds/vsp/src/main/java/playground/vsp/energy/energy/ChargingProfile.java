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
package playground.vsp.energy.energy;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class ChargingProfile{
	
	private static final Logger log = Logger
		.getLogger(ChargingProfile.class);
	private Id id;
	private SortedMap<Double, Duration2NewState> currState2out;
	
	public ChargingProfile(Id id){
		this.id = id;
		this.currState2out = new TreeMap<Double, ChargingProfile.Duration2NewState>();
	}
	
	public void addNewEntry(Double currentState, Double duration, Double newState){
		if(!this.currState2out.containsKey(currentState)){
			this.currState2out.put(currentState, new Duration2NewState());
		}
		// log an error if for a profile/currentState and a certain duration 2 values are given
		if(!this.currState2out.get(currentState).addNewEntry(duration, newState)){
			log.error("try to add the duration " + duration + " twice for the chargingProfile " + this.id.toString() + " and the currentState " +  currentState);
		}
	}
	
	public Double getNewState(Double duration, Double currentState){
	
		Duration2NewState loader = null;
		Double temp = Double.MAX_VALUE;
		Double diff;
		
		//find the next currentState
		for(Entry<Double, Duration2NewState> d: this.currState2out.entrySet()){
			diff = Math.abs(currentState - d.getKey());
			if(diff < temp){
				temp = diff;
				loader = d.getValue();
			}
		}
		return loader.getNewState(duration);
	}
	
	public Id getId(){
		return this.id;
	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		for(Entry<Double, Duration2NewState> e: this.currState2out.entrySet()){
			b.append("ID: " + this.id + "\t" + "currState: " + e.getKey() + "\n");
			b.append(e.getValue().toString() + "\n");
		}
		return b.toString();
	}
	
	//intern class
	private class Duration2NewState{
		private SortedMap<Double, Double> duration2NewState;
		
		public Duration2NewState(){
			this.duration2NewState = new TreeMap<Double, Double>();
		}
		
		public boolean addNewEntry(Double duration, Double newState){
			if(this.duration2NewState.containsKey(duration)){
				return false;
			}else{
				this.duration2NewState.put(duration, newState);
				return true;
			}
		}
		
		public Double getNewState(Double duration){
			// explicit value for this duration
			if(this.duration2NewState.containsKey(duration)){
				return this.duration2NewState.get(duration);
			}
			// duration > maxDuration given in this profile
			else if(duration > this.duration2NewState.lastKey()){
				//TODO Maximalwert?!?!
				return this.duration2NewState.get(this.duration2NewState.lastKey());
			}
			// no explicit value is given
			else{
				double low = 0;
				double tLow = 0;
				double high = 0;
				double tHigh = 0;
				for(Entry<Double, Double> e: this.duration2NewState.entrySet()){
					//find the next smallest Value
					if(e.getKey() < duration){
						low = e.getValue();
						tLow = e.getKey();
					}
					// find the next larger value
					if(e.getKey() > duration){
						high = e.getValue();
						tHigh = e.getKey();
						break;
					}
				}
				Double value = (low + ((duration - tLow)* (high-low)/(tHigh - tLow)));
				//TODO store the new value???
//				this.duration2NewState.put(duration, value);
				return value;
			}
			
		}
		
		@Override
		public String toString(){
			StringBuffer b =  new StringBuffer();
			for(Entry<Double, Double> e: this.duration2NewState.entrySet()){
				b.append(e.getKey() + "\t" + e.getValue() + "\n");
			}
			return b.toString();
		}
	}

}
