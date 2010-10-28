/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionPlugin.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.pluggable.energyConsumption;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class EnergyConsumptionPlugin implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler, AgentArrivalEventHandler {
	
	private EnergyConsumptionModel energyConsumptionModel;
	
	//Id: person
	HashMap<Id, Double> timeOfEnteringOrWaitingToEnterCurrentLink;
	
	//Id: person
	DoubleValueHashMap<Id> energyConsumptionOfCurrentLeg;

	//Id: person
	LinkedListValueHashMap<Id,Double> energyConsumptionOfLegs;
	
	
	private LinkedListValueHashMap<Id, Vehicle> vehicles;

	private Controler controler;
	

	public EnergyConsumptionPlugin(EnergyConsumptionModel energyConsumptionModel, LinkedListValueHashMap<Id, Vehicle> vehicles, Controler controler) {
		this.energyConsumptionModel=energyConsumptionModel;
		this.vehicles=vehicles;
		this.controler=controler;
	}
	
	public void reset(int iteration) {
		timeOfEnteringOrWaitingToEnterCurrentLink=new HashMap<Id, Double>();
		energyConsumptionOfCurrentLeg=new DoubleValueHashMap<Id>();
		energyConsumptionOfLegs=new LinkedListValueHashMap<Id, Double>();
	}

	public void handleEvent(LinkEnterEvent event) {
		logLinkEnteranceTime(event.getPersonId(), event.getTime());
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		logLinkEnteranceTime(event.getPersonId(), event.getTime());
	}
	
	private void logLinkEnteranceTime(Id personId, double timeOfEnteranceOfLinkOrWaitingToEnterLink){
		timeOfEnteringOrWaitingToEnterCurrentLink.put(personId, timeOfEnteranceOfLinkOrWaitingToEnterLink);
	}

	
	public void handleEvent(LinkLeaveEvent event) {
		updateEnergyConsumptionOfLeg(event.getPersonId(),event.getTime(),event.getLinkId());
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		updateEnergyConsumptionOfLeg(event.getPersonId(),event.getTime(),event.getLinkId());
		
		handleLegCompletion(event.getPersonId());
	}

	private void updateEnergyConsumptionOfLeg(Id personId,double linkLeaveTime, Id linkId){
		Double linkEnteranceTime=timeOfEnteringOrWaitingToEnterCurrentLink.get(personId);
		
		if (linkEnteranceTime==null){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		double timeSpendOnLink= GeneralLib.getIntervalDuration(linkEnteranceTime, linkLeaveTime);
		Link link = controler.getNetwork().getLinks().get(linkId);
		Vehicle vehicle=vehicles.getValue(personId);
		Double energyConsumptionOnLink=energyConsumptionModel.getEnergyConsumptionForLink(vehicle, timeSpendOnLink, link);
		
		energyConsumptionOfCurrentLeg.incrementBy(personId, energyConsumptionOnLink);
		
		resetLinkEnteranceTime(personId);
	}
	
	private void resetLinkEnteranceTime(Id personId){
		timeOfEnteringOrWaitingToEnterCurrentLink.remove(personId);
	}
	
	private void handleLegCompletion(Id personId) {
		energyConsumptionOfLegs.put(personId, energyConsumptionOfCurrentLeg.get(personId));
		energyConsumptionOfCurrentLeg.put(personId, 0.0);
	}
}
