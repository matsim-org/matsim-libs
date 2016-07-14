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
 */
public class DensityVsFractionOfStoppedVehiclesHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private final Id<Link> linkId ;
	private Map<Double, Double> densityVsFractionOfStoppedVehicles;
//	private final Map<String, Double> legMode2PCU;
	private Map<Id<Person>, String> personId2LegMode;
	private double localDensity = 0.;
	private Map<Id<Person>, Double> personId2LinkEnterTime;
	private Map<String, Double> legMode2FreeSpeddTravelTime;
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
	public DensityVsFractionOfStoppedVehiclesHandler(Id<Link> linkId, double linkLength) {
		this.linkId = linkId;

		this.densityVsFractionOfStoppedVehicles = new HashMap<Double, Double>();
		this.personId2LegMode = new HashMap<Id<Person>, String>();
		this.personId2LinkEnterTime = new HashMap<Id<Person>, Double>();
//		this.legMode2PCU = new HashMap<String, Double>();
		this.legMode2FreeSpeddTravelTime = new HashMap<String, Double>();

//		this.legMode2PCU.put("cars", Double.valueOf(1));
//		this.legMode2PCU.put("motorbikes", Double.valueOf(0.25));
//		this.legMode2PCU.put("bicycles", Double.valueOf(0.25));

		double carFreeSpeedTravelTime = Math.floor(linkLength/MixedTrafficVehiclesUtils.getSpeed(TransportMode.car))+1;
		double bicycleFreeSpeedTravelTime = Math.floor(linkLength/MixedTrafficVehiclesUtils.getSpeed(TransportMode.bike))+1;

		this.legMode2FreeSpeddTravelTime.put(TransportMode.car, carFreeSpeedTravelTime);
//		this.legMode2FreeSpeddTravelTime.put("motorbikes", carFreeSpeedTravelTime);
		this.legMode2FreeSpeddTravelTime.put(TransportMode.bike, bicycleFreeSpeedTravelTime);

	}

	@Override
	public void reset(int iteration) {
		this.densityVsFractionOfStoppedVehicles.clear();
		this.personId2LegMode.clear();
//		this.legMode2PCU.clear();
		this.personId2LinkEnterTime.clear();
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		if(event.getLinkId().equals(this.linkId)){
			double nowPCU = MixedTrafficVehiclesUtils.getPCU(this.personId2LegMode.get(personId));
			this.localDensity -= nowPCU;

			double freeSpeedVehiclesCounter = 0.;
			double stopeedVehiclesCounter =0.;

			double linkTravelTime = event.getTime() - this.personId2LinkEnterTime.get(personId);

			if(linkTravelTime>this.legMode2FreeSpeddTravelTime.get(this.personId2LegMode.get(personId))) {
				stopeedVehiclesCounter++;
			}
			freeSpeedVehiclesCounter++;

			double fractionOfStoppedVehicle = stopeedVehiclesCounter/freeSpeedVehiclesCounter;
			this.densityVsFractionOfStoppedVehicles.put(this.localDensity, fractionOfStoppedVehicle);

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
	
	public Map<Double, Double> getDensityVsFractionOfStoppedVehicles(){
		return this.densityVsFractionOfStoppedVehicles;
	}

}
