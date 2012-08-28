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

package org.matsim.contrib.transEnergySim.controllers;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author wrashid
 * 
 */
public class InductiveChargingController extends AddHandlerAtStartupControler {

	private InductiveStreetCharger inductiveCharger;
	private ChargingUponArrival chargingUponArrival;
	private EnergyConsumptionTracker energyConsumptionTracker;

	public InductiveChargingController(Config config, HashMap<Id, Vehicle> vehicles) {
		super(config);
		
		EventHandlerGroupForRaceConditionAvoidance handlerGroup=new EventHandlerGroupForRaceConditionAvoidance();
		
		setEnergyConsumptionTracker(new EnergyConsumptionTracker(vehicles, network));
		setInductiveCharger(new InductiveStreetCharger(vehicles, network, this));
		setChargingUponArrival(new ChargingUponArrival(vehicles, this));
		
		handlerGroup.addHandler(getEnergyConsumptionTracker());
		handlerGroup.addHandler(getInductiveCharger());
		handlerGroup.addHandler(getChargingUponArrival());
		
		addHandler(handlerGroup);
	}
	
	public void printToConsole(){
		System.out.println("energyConsumptionTracker");
		energyConsumptionTracker.getLog().printToConsole();
		System.out.println("===");
		inductiveCharger.getLog().printToConsole();
		System.out.println("===");
		chargingUponArrival.getLog().printToConsole();
		System.out.println("===");
	}

	public InductiveStreetCharger getInductiveCharger() {
		return inductiveCharger;
	}

	private void setInductiveCharger(InductiveStreetCharger inductiveCharger) {
		this.inductiveCharger = inductiveCharger;
	}

	public ChargingUponArrival getChargingUponArrival() {
		return chargingUponArrival;
	}

	private void setChargingUponArrival(ChargingUponArrival chargingUponArrival) {
		this.chargingUponArrival = chargingUponArrival;
	}

	public EnergyConsumptionTracker getEnergyConsumptionTracker() {
		return energyConsumptionTracker;
	}

	private void setEnergyConsumptionTracker(EnergyConsumptionTracker energyConsumptionTracker) {
		this.energyConsumptionTracker = energyConsumptionTracker;
	}

	private class EventHandlerGroupForRaceConditionAvoidance implements ActivityStartEventHandler, AgentArrivalEventHandler,
			AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
		
		protected LinkedList<EventHandler> handler = new LinkedList<EventHandler>();

		public void addHandler(EventHandler handler){
			this.handler.add(handler);
		}
		
		@Override
		public void reset(int iteration) {
			for (EventHandler h:handler){
				h.reset(iteration);
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			for (EventHandler h:handler){
				if (h instanceof LinkLeaveEventHandler){
					((LinkLeaveEventHandler) h).handleEvent(event);
				}
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			for (EventHandler h:handler){
				if (h instanceof LinkEnterEventHandler){
					((LinkEnterEventHandler) h).handleEvent(event);
				}
			}
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			for (EventHandler h:handler){
				if (h instanceof AgentDepartureEventHandler){
					((AgentDepartureEventHandler) h).handleEvent(event);
				}
			}
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			for (EventHandler h:handler){
				if (h instanceof AgentArrivalEventHandler){
					((AgentArrivalEventHandler) h).handleEvent(event);
				}
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			for (EventHandler h:handler){
				if (h instanceof ActivityStartEventHandler){
					((ActivityStartEventHandler) h).handleEvent(event);
				}
			}
		}

	}

}
