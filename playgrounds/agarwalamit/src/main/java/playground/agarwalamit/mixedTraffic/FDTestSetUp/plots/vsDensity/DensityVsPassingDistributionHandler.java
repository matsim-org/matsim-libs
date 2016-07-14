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
package playground.agarwalamit.mixedTraffic.FDTestSetUp.plots.vsDensity;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 * Density vs number of bicycles overtaken by cars on link id"1" in test network.
 */
public class DensityVsPassingDistributionHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private Map<Id<Person>, String> personId2LegMode;
	private Map<Double, Double> density2TotalOvertakenBicycles;
	private Map<Double, Double> density2AverageOvertakenBicycles;
	private double localDensity = 0.;
	private Map<Id<Person>, Double> personId2LinkEnterTime;
	private final Id<Link> linkId;
	private BufferedWriter writer;
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public DensityVsPassingDistributionHandler(Id<Link> linkId, BufferedWriter writer) {
		this.personId2LegMode = new HashMap<Id<Person>, String>();
		this.density2TotalOvertakenBicycles = new HashMap<Double, Double>();
		this.density2AverageOvertakenBicycles=new HashMap<Double, Double>();
		this.personId2LinkEnterTime = new HashMap<Id<Person>, Double>();
		this.linkId = linkId;
		this.writer = writer;
	}

	@Override
	public void reset(int iteration) {
		this.personId2LegMode.clear();
		this.density2TotalOvertakenBicycles.clear();
		this.density2AverageOvertakenBicycles.clear();
		this.personId2LinkEnterTime.clear();
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		if(event.getLinkId().equals(this.linkId)){
			double nowPCU = MixedTrafficVehiclesUtils.getPCU(this.personId2LegMode.get(personId));
			this.localDensity -= nowPCU;
			if(this.personId2LegMode.get(personId).equals(TransportMode.car)){ // when a car leaves; check if it have overtaken anything ?
				updateDensity2OvertakenBicycleCount(this.localDensity,event);
			}
			this.personId2LinkEnterTime.remove(personId);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());

		if(event.getLinkId().equals(this.linkId)){
			this.personId2LinkEnterTime.put(personId, event.getTime());
			String legMode = this.personId2LegMode.get(personId);
			this.localDensity +=MixedTrafficVehiclesUtils.getPCU(legMode);
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
		
		try {
			this.writer.write(localDensity+"\t"+numberOfBicyclesOvertaken+"\n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
		
		this.density2AverageOvertakenBicycles.put(localDensity, numberOfBicyclesOvertaken/noOfVehiclesOnLink);
	}

	private double getNumberOfBicycleOvertaken(LinkLeaveEvent event) {
		double overtakenBicycles =0;
		// Simply, link leave event of car is passed one at a time and thus for e.g. for car link leave event
		//enter time of car is more than bike enter time and leave time of bike is not reached yet
		// if end time of bike will reach, it won't be in linkEnterList 
		for(Id<Person> personId:this.personId2LinkEnterTime.keySet()){
			if(this.personId2LinkEnterTime.get(this.delegate.getDriverOfVehicle(event.getVehicleId())) > this.personId2LinkEnterTime.get(personId)){
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
