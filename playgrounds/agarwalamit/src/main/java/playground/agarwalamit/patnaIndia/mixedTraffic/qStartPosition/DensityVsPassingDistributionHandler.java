/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.patnaIndia.mixedTraffic.qStartPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 * Density vs number of bicycles overtaken by cars on link id"1" in test network.
 */
public class DensityVsPassingDistributionHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private Map<Id<Person>, String> personId2LegMode;
	private final Map<String, Double> legMode2PCU;
	private Map<Double, Double> density2TotalOvertakenBicycles;
	private Map<Double, Double> density2AverageOvertakenBicycles;
	private double localDensity = 0.;
	private Map<Id<Person>, Double> personId2LinkEnterTime;
	private final Id<Link> linkId;

	public DensityVsPassingDistributionHandler(Id<Link> linkId) {
		this.personId2LegMode = new HashMap<Id<Person>, String>();
		this.density2TotalOvertakenBicycles = new HashMap<Double, Double>();
		this.density2AverageOvertakenBicycles=new HashMap<Double, Double>();
		this.personId2LinkEnterTime = new HashMap<Id<Person>, Double>();
		this.legMode2PCU = new HashMap<String, Double>();
		this.linkId = linkId;

		this.legMode2PCU.put("cars", Double.valueOf(1));
		this.legMode2PCU.put("motorbikes", Double.valueOf(0.25));
		this.legMode2PCU.put("bicycles", Double.valueOf(0.25));
		this.legMode2PCU.put("fast", Double.valueOf(1));
		this.legMode2PCU.put("med", Double.valueOf(0.25));
		this.legMode2PCU.put("truck", Double.valueOf(0.25));

	}

	@Override
	public void reset(int iteration) {
		this.personId2LegMode.clear();
		this.legMode2PCU.clear();
		this.density2TotalOvertakenBicycles.clear();
		this.density2AverageOvertakenBicycles.clear();
		this.personId2LinkEnterTime.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = event.getPersonId();
		if(event.getLinkId().equals(this.linkId)){

			this.localDensity -=this.legMode2PCU.get(this.personId2LegMode.get(personId));
			if(!this.personId2LegMode.get(personId).equals("bicycles")){
				updateDensity2OvertakenBicycleCount(this.localDensity,event);
			}

			this.personId2LinkEnterTime.remove(personId);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = event.getPersonId();

		if(event.getLinkId().equals(this.linkId)){
			this.personId2LinkEnterTime.put(personId, event.getTime());
			String legMode = this.personId2LegMode.get(personId);
			this.localDensity +=this.legMode2PCU.get(legMode);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2LegMode.put(event.getPersonId(),event.getLegMode());
	}

	private void updateDensity2OvertakenBicycleCount(double localDensity, LinkLeaveEvent event){
		double numberOfBicyclesOvertaken = getNumberOfBicycleOvertaken(event);
		double noOfVehiclesOnLink = this.personId2LinkEnterTime.size();
		this.density2TotalOvertakenBicycles.put(localDensity, numberOfBicyclesOvertaken);
		this.density2AverageOvertakenBicycles.put(localDensity, numberOfBicyclesOvertaken/noOfVehiclesOnLink);
	}

	private double getNumberOfBicycleOvertaken(LinkLeaveEvent event) {
		double overtakenBicycles =0;

		for(Id<Person> personId:this.personId2LinkEnterTime.keySet()){
			if(this.personId2LinkEnterTime.get(event.getPersonId())>this.personId2LinkEnterTime.get(personId)){
				overtakenBicycles++;
			}
		}
		return overtakenBicycles;
	}

	public Map<Double, Double> getDensity2TotalOvertakenBicycleCount(){
		return this.density2TotalOvertakenBicycles;
	}

	public Map<Double, Double> getDensity2AverageOvertakenBicycleCount(){
		return this.density2AverageOvertakenBicycles;
	}
}
