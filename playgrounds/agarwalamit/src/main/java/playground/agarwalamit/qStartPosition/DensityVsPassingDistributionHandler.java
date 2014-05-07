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
package playground.agarwalamit.qStartPosition;

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

/**
 * @author amit
 * Density vs number of bicycles overtaken by cars on link id"1" in test network.
 */
public class DensityVsPassingDistributionHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private Map<Id, String> personId2LegMode;
	private final Map<String, Double> legMode2PCU;
	private Map<Double, Double> density2OvertakenBicycles;
	private double localDensity = 0.;
	private Map<Id, Double> legMode2LinkEnterTime;
	private Map<Id, Double> legMode2LinkLeaveTime;
	private final Id linkId;

	public DensityVsPassingDistributionHandler(Id linkId) {
		this.personId2LegMode = new HashMap<Id, String>();
		this.density2OvertakenBicycles = new HashMap<Double, Double>();
		this.legMode2LinkEnterTime = new HashMap<Id, Double>();
		this.legMode2LinkLeaveTime = new HashMap<Id, Double>();
		this.legMode2PCU = new HashMap<String, Double>();
		this.linkId = linkId;
		reset(0);

		this.legMode2PCU.put("cars", Double.valueOf(1));
		this.legMode2PCU.put("motorbikes", Double.valueOf(0.25));
		this.legMode2PCU.put("bicycles", Double.valueOf(0.25));

	}

	@Override
	public void reset(int iteration) {
		this.personId2LegMode.clear();
		this.legMode2PCU.clear();
		this.density2OvertakenBicycles.clear();
		this.legMode2LinkEnterTime.clear();
		this.legMode2LinkLeaveTime.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();
		if(event.getLinkId().equals(this.linkId)){
			this.legMode2LinkLeaveTime.put(personId, event.getTime());

			this.localDensity -=this.legMode2PCU.get(this.personId2LegMode.get(personId));
			updateDensity2OvertakenBicycleCount(this.localDensity);
			this.legMode2LinkEnterTime.remove(personId);
			this.legMode2LinkLeaveTime.remove(personId);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();

		if(event.getLinkId().equals(this.linkId)){
			this.legMode2LinkEnterTime.put(personId, event.getTime());
			String legMode = this.personId2LegMode.get(personId);
			this.localDensity +=this.legMode2PCU.get(legMode);
			//			updateDensity2OvertakenBicycleCount(this.localDensity);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		personId2LegMode.put(event.getPersonId(),event.getLegMode());
	}

	private void updateDensity2OvertakenBicycleCount(double localDensity){
		double numberOfBicyclesOvertaken = getNumberOfBicycleOvertaken();
		this.density2OvertakenBicycles.put(localDensity, numberOfBicyclesOvertaken);
	}

	private double getNumberOfBicycleOvertaken() {
		double overtakenBicycles =0;
		double noOfBicyclesOnLink = getNumberOfVehiclesOnLink("bicycles",this.legMode2LinkEnterTime);
		double noOfMotorbikesOnLink = getNumberOfVehiclesOnLink("motorbikes",this.legMode2LinkEnterTime);
		double noOfCarsOnLink = getNumberOfVehiclesOnLink("cars",this.legMode2LinkEnterTime);
		for(Id personId:this.legMode2LinkEnterTime.keySet()){
			if(!this.personId2LegMode.get(personId).equals("bicycles")){
				if(this.legMode2LinkLeaveTime.containsKey(personId)){
					//					overtakenBicycles += noOfBicyclesOnLink;
					double avgNoOfBicyclesOvertaken = noOfBicyclesOnLink/(noOfCarsOnLink+noOfMotorbikesOnLink);
					overtakenBicycles +=avgNoOfBicyclesOvertaken;
				}
			}
		}
		return overtakenBicycles;
	}

	public Map<Double, Double> getDensity2OvertakenBicycleCount(){
		return this.density2OvertakenBicycles;
	}

	private double getNumberOfVehiclesOnLink(String vehicleType, Map<Id, Double> mapInWhichLookFor){
		double noOfVehicles=0;
		for(Entry<Id, Double> e : mapInWhichLookFor.entrySet()){
			if(this.personId2LegMode.get(e.getKey()).equals(vehicleType)){
				noOfVehicles++;
			}
		}
		return noOfVehicles;
	}
}
