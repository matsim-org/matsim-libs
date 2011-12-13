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
package playground.droeder.eMobility;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;

/**
 * @author droeder
 *
 */
public class ElectroVehicleHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
												ActivityStartEventHandler, ActivityEndEventHandler,
												AgentArrivalEventHandler{

	private Map<Id, ElectroVehicle> vehicles;
	private HashMap<Id, LinkEnterEvent> disCharging;
	private HashMap<Id, ActivityStartEvent> charging;
	private ChargingProfiles chargingProfiles;
	private Network net;
	private DisChargingProfiles disChargingProfiles;
	private Map<Id, String> lastMode;

	public ElectroVehicleHandler(Map<Id, ElectroVehicle> vehicles, ChargingProfiles charging, 
			DisChargingProfiles discharging, Network net){
		this.vehicles = vehicles;
		this.disCharging = new HashMap<Id, LinkEnterEvent>();
		this.charging = new HashMap<Id, ActivityStartEvent>();
		this.chargingProfiles = charging;
		this.disChargingProfiles = discharging;
		this.net = net;
		this.lastMode = new HashMap<Id, String>();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.processEvent(event);
	}
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.lastMode.put(event.getPersonId(), event.getLegMode()); 
	}
	
	private void processEvent(PersonEvent e){
		//TODO change to vehId
		if(!this.vehicles.containsKey(e.getPersonId())){
			// no eletroVehicle
			return;
		}else{
			//TODO change to vehId
			ElectroVehicle v = this.vehicles.get(e.getPersonId());
			if(e instanceof ActivityEndEvent){
				if(this.charging.containsKey(v.getId())){
					// it is not the first activity, so charge the vehicle depending on the given data
					double charge = this.chargingProfiles.getNewState(v.getChargingType(), e.getTime() - this.charging.get(v.getId()).getTime(), 
							v.getChargeState());
					v.setNewCharge(charge, e.getTime());
				}else{
					/* it is the first activity of an agent, we don't know anything about the duration of it's activity,
					 * so we can not charge here
					 */
				}
			}else if(e instanceof LinkLeaveEvent){
				if(this.disCharging.containsKey(v.getId())){
					Link l = net.getLinks().get(((LinkLeaveEvent)e).getLinkId());
					v.disCharge(this.calcDischargeInkWh(v, this.disCharging.remove(v.getId()), e, l), e.getTime(), l.getLength());
				}else{
					/*  this must be a LinkLeaveEvent after an activity. 
					 *  The activity is located at the end of the link, so the discharging should happen with the ActStartEvent
					 */
				}
			}else if(e instanceof LinkEnterEvent){
				//store the event to process it later
				this.disCharging.put(v.getId(), (LinkEnterEvent) e);
			}else if(e instanceof ActivityStartEvent){
				// store the event to process it later
				if(this.lastMode.containsKey(e.getPersonId()) && this.lastMode.get(e.getPersonId()).equals(TransportMode.car)){
					this.charging.put(v.getId(), (ActivityStartEvent) e);
				}
				/* discharge here for the last passed link otherwise the avSpeedCalculation will be wrong,
				 * because the TT then includes the activityDuration 
				 * we don't need to check if a LinkEnterEvent is stored, because it _must_ be stored at this time
				 */
				if(this.disCharging.containsKey(v.getId())){
					Link l = net.getLinks().get(((ActivityEvent) e).getLinkId());
					v.disCharge(this.calcDischargeInkWh(v, this.disCharging.remove(v.getId()), e, l), e.getTime(), l.getLength());
				}
			}
		}
	}
	
	private Double calcDischargeInkWh(ElectroVehicle v, LinkEnterEvent enter, PersonEvent e, Link l) {
		Double disChargePerKmInJoule = this.disChargingProfiles.getJoulePerKm(v.getDisChargingType(), 
				this.calculateAvSpeed(enter.getTime(), e.getTime(), l), 
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
