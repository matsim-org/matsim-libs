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

package playground.wrashid.artemis.lav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * This class assigns one energy consumption value to each trip of a vehicle.
 * 
 * note: this module is compatible with both the quesim and jdeqsim model. the maxAllowedSpeedInNetworkInKmPerHour parameter 
 * to the energyConsumptionModel filters out, if the first or last link have been driven by the vehicle.
 * 
 * @author wrashid
 *
 */
public class EnergyConsumptionPlugin implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler, AgentArrivalEventHandler {
	
	private EnergyConsumptionModelLAV_v1 energyConsumptionModel;
	
	//Id: person
	HashMap<Id, Double> timeOfEnteringOrWaitingToEnterCurrentLink;
	
	//Id: person
	DoubleValueHashMap<Id> energyConsumptionOfCurrentLeg;

	//Id: person
	LinkedListValueHashMap<Id,Double> energyConsumptionOfLegs;
	
	// agent Id, linkId
	HashMap<Id,Id> lastLinkEntered;
	
	
	public LinkedListValueHashMap<Id, Double> getEnergyConsumptionOfLegs() {
		return energyConsumptionOfLegs;
	}

	private NetworkImpl network;

	private HashMap<Id, VehicleTypeLAV> agentVehicleMapping;

	private final HashMap<Id, VehicleSOC> agentSocMapping;

	private ArrayList<String> outputLogEnergyConsumptionPerLink;

	private final boolean useLog;

	public EnergyConsumptionPlugin(EnergyConsumptionModelLAV_v1 energyConsumptionModel, HashMap<Id,VehicleTypeLAV> agentVehicleMapping, NetworkImpl network, HashMap<Id,VehicleSOC> agentSocMapping, boolean useLog) {
		this.energyConsumptionModel=energyConsumptionModel;
		this.agentVehicleMapping=agentVehicleMapping;
		this.network=network;
		this.agentSocMapping = agentSocMapping;
		this.useLog = useLog;
		outputLogEnergyConsumptionPerLink=new ArrayList<String>();
		
		outputLogEnergyConsumptionPerLink.add("averageSpeedDrivenInMetersPerSecond\tMaxSpeedInMetersPerSecond\tenergyConsumptionOnLinkInJoule\tPowerTrain\tFuel\tPower\tWeight\tagentId\tlinkEnterTime\tlinkLengthInMeters");
	
		reset(0);
	}
	
	

	public void reset(int iteration) {
		timeOfEnteringOrWaitingToEnterCurrentLink=new HashMap<Id, Double>();
		energyConsumptionOfCurrentLeg=new DoubleValueHashMap<Id>();
		energyConsumptionOfLegs=new LinkedListValueHashMap<Id, Double>();
		lastLinkEntered = new HashMap<Id, Id>();
	}

	public void handleEvent(LinkEnterEvent event) {
		logLinkEnteranceTime(event.getPersonId(), event.getTime());
		
		lastLinkEntered.put(event.getPersonId(), event.getLinkId());
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
		if (isValidArrivalEventWithCar(event.getPersonId(),event.getLinkId())){
			updateEnergyConsumptionOfLeg(event.getPersonId(),event.getTime(),event.getLinkId());
			
			handleLegCompletion(event.getPersonId());
			
			resetLastLinkEntered(event.getPersonId());
		}
		
	}

	private void updateEnergyConsumptionOfLeg(Id personId,double linkLeaveTime, Id linkId){
		Double linkEnteranceTime=timeOfEnteringOrWaitingToEnterCurrentLink.get(personId);
		
		if (linkEnteranceTime==null){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		double timeSpendOnLink= GeneralLib.getIntervalDuration(linkEnteranceTime, linkLeaveTime);
		resetLinkEnteranceTime(personId);
		Link link = network.getLinks().get(linkId);
		double averageSpeedDrivenInMetersPerSecond=link.getLength()/timeSpendOnLink;
		if (timeSpendOnLink==GeneralLib.numberOfSecondsInDay || averageSpeedDrivenInMetersPerSecond>link.getFreespeed()){
			// do not consider events with "zero" travel time and such, which produce higher travel times than
			// allowed due to artifacts from different simulations, e.g. wait2link, etc.
			return;
		}
//		if (averageSpeedDrivenInMetersPerSecond<3){
//			// avoid too small speeds, which lead to negative number in energy consumption regression model
//			return;
//		}
				
		
		double energyConsumptionOnLink=getEnergyConsumptionAndHandleSoc(personId,timeSpendOnLink,link,linkEnteranceTime);
		energyConsumptionOfCurrentLeg.incrementBy(personId, energyConsumptionOnLink);
	}
	
	private double getEnergyConsumptionAndHandleSoc(Id personId, double timeSpendOnLink, Link link, Double linkEnteranceTime) {
		VehicleTypeLAV vehicle=null;
		if (agentVehicleMapping.containsKey(personId)){
			vehicle=agentVehicleMapping.get(personId);
		} else {
			DebugLib.stopSystemAndReportInconsistency("personId:" + personId);
		}
		
		Double energyConsumptionOnLink=energyConsumptionModel.getEnergyConsumptionForLinkInJoule(vehicle, timeSpendOnLink, link);
		
		VehicleSOC vehicleSOC = agentSocMapping.get(personId);
		
		if (vehicle.powerTrainClass==LAVLib.getBatteryElectricPowerTrainClass()){
			vehicleSOC.useBattery(energyConsumptionOnLink);
		} else if (vehicle.powerTrainClass==LAVLib.getPHEVPowerTrainClass()){
			if (vehicleSOC.getSocInJoule()<energyConsumptionOnLink){
				vehicle.ifPHEVSwitchToGasolineMode();
				energyConsumptionOnLink=energyConsumptionModel.getEnergyConsumptionForLinkInJoule(vehicle, timeSpendOnLink, link);
			} else {
				vehicle.ifPHEVSwitchToElectricity();
				vehicleSOC.useBattery(energyConsumptionOnLink);
			}
		}
		
		if (energyConsumptionOnLink<0){
			vehicle.print();
			DebugLib.stopSystemAndReportInconsistency("energyConsumptionOnLink:"+energyConsumptionOnLink);
		}
		
		updateEnergyConsumptionLog(timeSpendOnLink, link, vehicle, energyConsumptionOnLink, linkEnteranceTime, personId);
		
		return energyConsumptionOnLink;
	}

	private void updateEnergyConsumptionLog(double timeSpendOnLink, Link link, VehicleTypeLAV vehicle,
			Double energyConsumptionOnLink, Double linkEnteranceTime, Id personId) {
		
		if (!useLog){
			return;
		}
		
		StringBuffer stringBuffer=new StringBuffer();
		
		double averageSpeedDriven = link.getLength()/timeSpendOnLink;
		if (averageSpeedDriven>link.getFreespeed()){
			// remove artifacts
			//averageSpeedDriven=link.getFreespeed();
			DebugLib.stopSystemAndReportInconsistency();
		}
//		
//		if (averageSpeedDriven<1.0){
//			DebugLib.emptyFunctionForSettingBreakPoint();
//		}
		
		stringBuffer.append(averageSpeedDriven);
		stringBuffer.append("\t");
		stringBuffer.append(link.getFreespeed());
		stringBuffer.append("\t");
		stringBuffer.append(energyConsumptionOnLink);
		stringBuffer.append("\t");
		stringBuffer.append(vehicle.powerTrainClass);
		stringBuffer.append("\t");
		stringBuffer.append(vehicle.fuelClass);
		stringBuffer.append("\t");
		stringBuffer.append(vehicle.powerClass);
		stringBuffer.append("\t");
		stringBuffer.append(vehicle.massClass);
		stringBuffer.append("\t");
		stringBuffer.append(personId);
		stringBuffer.append("\t");
		stringBuffer.append(GeneralLib.projectTimeWithin24Hours(linkEnteranceTime));
		stringBuffer.append("\t");
		stringBuffer.append(link.getLength());
		
		outputLogEnergyConsumptionPerLink.add(stringBuffer.toString());
	}

	private void resetLinkEnteranceTime(Id personId){
		timeOfEnteringOrWaitingToEnterCurrentLink.remove(personId);
	}
	
	private void handleLegCompletion(Id personId) {
		energyConsumptionOfLegs.put(personId, energyConsumptionOfCurrentLeg.get(personId));
		energyConsumptionOfCurrentLeg.put(personId, 0.0);
	}
	
	private void resetLastLinkEntered(Id personId){
		lastLinkEntered.put(personId, null);
	}
	
	private boolean isValidArrivalEventWithCar(Id personId, Id linkId){
		return lastLinkEntered.containsKey(personId) && lastLinkEntered.get(personId)!=null && lastLinkEntered.get(personId).equals(linkId);
	}
	
	public void writeOutputLog(String outputFileName){
		GeneralLib.writeList(outputLogEnergyConsumptionPerLink, outputFileName);
	}
}
