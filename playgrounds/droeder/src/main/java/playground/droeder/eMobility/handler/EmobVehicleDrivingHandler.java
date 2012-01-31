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
package playground.droeder.eMobility.handler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.subjects.EmobVehicle;

/**
 * @author droeder
 *
 */
public class EmobVehicleDrivingHandler implements LinkEnterEventHandler, LinkLeaveEventHandler{
	
	private DisChargingProfiles dCharge;
	private Network net;
	private Map<Id, EmobVehicle> veh;
	private HashMap<Id, Id> vehDisChargeType;
	private HashMap<Id, LinkEnterEvent> linkEnter;
	private EventsManager events;

	public EmobVehicleDrivingHandler(DisChargingProfiles disChargingProfiles, Network net, EventsManager events){
		this.dCharge = disChargingProfiles;
		this.net = net;
		this.veh = new HashMap<Id, EmobVehicle>();
		this.vehDisChargeType = new HashMap<Id, Id>();
		this.linkEnter = new HashMap<Id, LinkEnterEvent>();
		this.events = events ;
	}
	
	public void startRide(EmobVehicle v, Id disChargingType){
		this.veh.put(v.getId(),v);
		this.vehDisChargeType.put(v.getId(), disChargingType);
	}
	
	public void finishRide(Id vehId, double arrival){
		if(this.linkEnter.containsKey(vehId)){
			LinkEnterEvent e = this.linkEnter.remove(vehId);
			Double disCharge = this.calcDischargeInkWh(e.getTime(), arrival, vehId, e.getLinkId());
			this.veh.get(vehId).disCharge(
					disCharge, 
					arrival, 
					this.net.getLinks().get(e.getLinkId()).getLength());
		}
		this.veh.remove(vehId);
		this.vehDisChargeType.remove(vehId);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(this.linkEnter.containsKey(event.getVehicleId())){
			Double disCharge = this.calcDischargeInkWh(
					this.linkEnter.get(event.getVehicleId()).getTime(), 
					event.getTime(), 
					event.getVehicleId(), 
					event.getLinkId());
			this.veh.get(event.getVehicleId()).disCharge(
					disCharge, 
					event.getTime(), 
					this.net.getLinks().get(event.getLinkId()).getLength());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.veh.containsKey(event.getVehicleId())){
			this.linkEnter.put(event.getVehicleId(), event);
		}
	}

	private Double calcDischargeInkWh(Double enter, Double leave, Id vehId, Id linkId ) {
		Link l = this.net.getLinks().get(linkId);
		Id disChType = this.vehDisChargeType.get(vehId);
		Double disChargePerKmInJoule = this.dCharge.getJoulePerKm(disChType, 
				this.calculateAvSpeed(enter, leave, l), 
				this.calculateSlopeInPercent(l));
		return (disChargePerKmInJoule * l.getLength() / 1000. * 2.778 * Math.pow(10, -7));
	}

	private double calculateAvSpeed(double start, double end, Link l){
		return (l.getLength()/(end-start));
	}
	
	private double calculateSlopeInPercent(Link l){
		// TODO z-coordinate?!
//		Coord v = CoordUtils.minus(l.getToNode().getCoord(), l.getFromNode().getCoord());
//		double xy = Math.sqrt(Math.pow(v.getX(), 2) + Math.pow(v.getY(), 2));
//		double z = v.getZ();
//		double slope = z/xy;
		double slope = 0;
		return slope*100;
	}
}
