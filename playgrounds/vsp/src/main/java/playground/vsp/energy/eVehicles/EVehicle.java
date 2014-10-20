/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.energy.eVehicles;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.energy.ePlans.EVehiclePlan;
import playground.vsp.energy.poi.PoiList;

/**
 * @author droeder
 *
 */
public class EVehicle {
	private StateStore store;
	private Double currentSoC;
	private EVehiclePlan vPlan;
	private Id<Person> id;
	private double lengthOfLastLink;
	private double linkEnterTime;

	public EVehicle(Id<Person> id, EVehiclePlan vPlan, Double initialSoC){
		this.id = id;
		this.vPlan = vPlan;
		this.currentSoC = initialSoC;
		this.lengthOfLastLink = 0.;
		this.linkEnterTime = 1.;
		this.store = new StateStore();
	}

	public Id<Person> getId() {
		return this.id;
	}

	public boolean rightPerson(Id<Person> personId) {
		return this.vPlan.expectedPerson(personId);
	}

	/**
	 * @param time
	 */
	public void discharge(double time, double discharge ) {
		this.currentSoC = this.currentSoC - (discharge * this.lengthOfLastLink / 1000. * 2.778 * Math.pow(10, -7));
		this.storeSoC(time, this.currentSoC, this.lengthOfLastLink);
	}

	/**
	 * @param time
	 * @param initialSoC2
	 */
	private void storeSoC(double time, double soc, double dist) {
		this.store.addValue(soc, time, dist);
	}

	/**
	 * @param time
	 */
	public void charge(double time, double newSoC) {
		this.storeSoC(this.getChargingStart(), this.currentSoC, 0.);
		this.currentSoC = newSoC;
		this.storeSoC(time, this.currentSoC, this.lengthOfLastLink);
	}
	
	public double getLinkEnterTime(){
		return this.linkEnterTime;
	}
	
	public double getChargingEnd(double time){
		double plannedDuration = this.vPlan.getEnd() - this.vPlan.getStart();
		if(this.getChargingStart() + plannedDuration > time){
			return time;
		}else{
			return this.getChargingStart() + plannedDuration;
		}
//		double maxDuration = time - this.getChargingStart();
//		if(maxDuration < plannedDuration){
//			return this.getChargingStart() + maxDuration;
//		}else{
//			return this.vPlan.getEnd();
////			return this.getChargingStart() + plannedDuration;
//		}
//		if(vPlan.getEnd() < time){
//			return this.vPlan.getEnd();
//		}else{
//			return time;
//		}
	}
	
	public double getChargingStart(){
		if(this.vPlan.getStart() < this.linkEnterTime){
			return this.linkEnterTime;
		}else{
			return this.vPlan.getStart();
		}
	}
	
	public double getLastLinkLength(){
		return this.lengthOfLastLink;
	}

	/**
	 * @param length
	 * @param time
	 */
	public void setLinkEnter(double length, double time) {
		this.linkEnterTime = time;
		this.lengthOfLastLink = length;
	}
	
	private class StateStore{
		
		List<Double> soc, dist, time;
		
		public StateStore(){
			this.soc = new ArrayList<Double>();
			this.dist = new ArrayList<Double>();
			this.time = new ArrayList<Double>();
		}
		
		public void addValue(double soc, double time, double additionalDistance) {
			this.soc.add(soc);
			this.time.add(time);
			if(this.dist.size() > 0){
				this.dist.add(this.dist.get(this.dist.size() -1 ) + additionalDistance);
			}else{
				this.dist.add(additionalDistance);
			}
		}
		
		public Double[] getSoC(){
			return this.soc.toArray(new Double[this.soc.size()]);
		}
		
		public Double[] getDist(){
			return this.dist.toArray(new Double[this.soc.size()]);
		}
		
		public Double[] getTime(){
			return this.time.toArray(new Double[this.soc.size()]);
		}
		
	}

	/**
	 * 
	 */
	public void increase() {
		this.vPlan.increase();
	}

	/**
	 * @return
	 */
	public Id getProfileId() {
		return this.vPlan.getProfileId();
	}

	/**
	 * @return
	 */
	public Double getSoC() {
		return this.currentSoC;
	}

	/**
	 * @return
	 */
	public Double[] getSoCChangeTimes() {
		return this.store.getTime();
	}
	
	public Double[] getTravelledDistances(){
		return this.store.getDist();
	}
	
	public Double[] getSoCs(){
		return this.store.getSoC();
	}

	private boolean plugged = false;
	/**
	 * @param poilist
	 */
	public void plug(PoiList poilist, Double time) {
		this.plugged = poilist.plug(this.vPlan.getPoiId(), time);
	}

	/**
	 * @param poilist
	 * @return
	 */
	public boolean unplug(PoiList poilist, Double time) {
		if(this.plugged){
			this.plugged = false;
			poilist.unplug(this.vPlan.getPoiId(), time);
			return true;
		}else{
			return false;
		}
	}

}
