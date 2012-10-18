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
public class DisChargingProfile{
	

	private static final Logger log = Logger
		.getLogger(DisChargingProfile.class);
	private Id id;
	private SortedMap<Double, Speed2usage> slope2out;
	
	public DisChargingProfile(Id id){
		this.id = id;
		this.slope2out = new TreeMap<Double, Speed2usage>();
	}
	
	public void addNewEntry(Double slope, Double speed, Double newState){
		if(!this.slope2out.containsKey(slope)){
			this.slope2out.put(slope, new Speed2usage());
		}
		// log an error if for a certain speed in one profile/slope two values are given
		if(!this.slope2out.get(slope).addNewEntry(speed, newState)){
			log.error("try to add the duration " + speed + " twice for the disChargingProfile " + this.id.toString() + " and the slope " +  slope);
		}
	}
	
	public Double getNewState(Double speed, Double slope){
	
		Speed2usage usage = null;
		Double temp = Double.MAX_VALUE;
		Double diff;
		
		//find the next slope
		for(Entry<Double, Speed2usage> d: this.slope2out.entrySet()){
			diff = Math.abs(slope - d.getKey());
			if(diff < temp){
				temp = diff;
				usage = d.getValue();
			}
		}
		return usage.getUsage(speed);
	}
	
	public Id getId(){
		return this.id;
	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		for(Entry<Double, Speed2usage> e: this.slope2out.entrySet()){
			b.append("ID: " + this.id + "\t" + "slope: " + e.getKey() + "\n");
			b.append(e.getValue().toString() + "\n");
		}
		return b.toString();
	}
	
	//intern class
	private class Speed2usage{
		private SortedMap<Double, Double> speed2usage;
		
		public Speed2usage(){
			this.speed2usage = new TreeMap<Double, Double>();
		}
		
		public boolean addNewEntry(Double duration, Double newState){
			if(this.speed2usage.containsKey(duration)){
				return false;
			}else{
				this.speed2usage.put(duration, newState);
				return true;
			}
		}
		
		public Double getUsage(Double speed){
			// speed is given in table, take the usage
			if(this.speed2usage.containsKey(speed)){
				return this.speed2usage.get(speed);
			}
			// speed > maxSpeed for given profile/slope
			else if(speed > this.speed2usage.lastKey()){
				//TODO Maximalwert, neu berechnen oder max aus Tabelle für entspr. Profil/Steigung?!?!
				return this.speed2usage.get(this.speed2usage.lastKey());
			}
			// linear interpolation, because no exact value is given
			else{
				double low = 0;
				double vLow = 0;
				double high = 0;
				double vHigh = 0;
				for(Entry<Double, Double> e: this.speed2usage.entrySet()){
					//find the next smaller value
					if(e.getKey() < speed){
						low = e.getValue();
						vLow = e.getKey();
					}
					// find the next larger value (and break, because we got everything we need)
					if(e.getKey() > speed){
						high = e.getValue();
						vHigh = e.getKey();
						break;
					}
				}
				//calculate the usage
				Double value = (low + ((speed - vLow)* (high-low)/(vHigh - vLow)));
				// store the value ???
//				this.speed2usage.put(speed, value);
				return value;
			}
			
		}
		
		@Override
		public String toString(){
			StringBuffer b =  new StringBuffer();
			for(Entry<Double, Double> e: this.speed2usage.entrySet()){
				b.append(e.getKey() + "\t" + e.getValue() + "\n");
			}
			return b.toString();
		}
	}
	

}
