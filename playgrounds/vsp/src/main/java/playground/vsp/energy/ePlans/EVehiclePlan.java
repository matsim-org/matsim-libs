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
package playground.vsp.energy.ePlans;

import java.util.List;
import java.util.ListIterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.energy.energy.ChargingProfile;
import playground.vsp.energy.energy.ChargingProfiles;
import playground.vsp.energy.poi.Poi;

/**
 * @author droeder
 *
 */
public class EVehiclePlan {
	
	private ListIterator<EVehiclePlanElement> elements;
	private EVehiclePlanElement currentElement;
	
	public EVehiclePlan(List<EVehiclePlanElement> elements){
		this.elements = elements.listIterator();
		this.currentElement = this.elements.next();
	}

	public void increase() {
		if(this.elements.hasNext()){
			this.currentElement = this.elements.next();
		}else{
			this.currentElement = new EChargingAct(0., 0., ChargingProfiles.NONE, Id.create("NONE", Person.class), Id.create("NONE", Poi.class));
		}
	}

	public boolean expectedPerson(Id<Person> personId) {
//		System.out.println(this.currentElement.getDriverId());
		return this.currentElement.getPersonId().equals(personId);
	}

	public double getStart() {
		if(this.currentElement instanceof EChargingAct){
			return ((EChargingAct)this.currentElement).getStart();
		}else{
			return Double.MAX_VALUE;
		}
	}

	public double getEnd() {
		if(this.currentElement instanceof EChargingAct){
			return ((EChargingAct)this.currentElement).getEnd();
		}else{
			return Double.MIN_VALUE;
		}
	}
	
	public Id<Poi> getPoiId(){
		if(this.currentElement instanceof EChargingAct){
			return ((EChargingAct)this.currentElement).getPoiId();
		}else{
			return Id.create("NONE", Poi.class);
		}
	}

	public Id<ChargingProfile> getProfileId() {
		return this.currentElement.getProfileId();
	}

}
