/* *********************************************************************** *
 * project: org.matsim.*
 * WarmEmissionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.agarwalamit.marginalTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import playground.agarwalamit.marginalTesting.WarmEmissionAnalysisModuleImplV2.WarmEmissionAnalysisModuleParameter;
import playground.agarwalamit.siouxFalls.emissionAnalyzer.EmissionUtilsExtended;
import playground.ikaddoura.internalizationCar.LinkCongestionInfo;


/**
 * @author amit after benjamin, ihab
 *
 */
public class WarmEmissionHandlerImplV2 implements 
LinkEnterEventHandler, 
LinkLeaveEventHandler, 
TransitDriverStartsEventHandler,
PersonArrivalEventHandler, 
PersonDepartureEventHandler,
ActivityEndEventHandler,
PersonStuckEventHandler{
	private static final Logger log = Logger.getLogger(WarmEmissionHandlerImplV2.class);

	Network network;
	Vehicles emissionVehicles;
	WarmEmissionAnalysisModuleImplV2 warmEmissionAnalysisModule;

	int linkLeaveCnt = 0;
	int linkLeaveFirstActWarnCnt = 0;
	final int maxLinkLeaveFirstActWarnCnt = 3;
	int linkLeaveSomeActWarnCnt = 0;
	final int maxLinkLeaveSomeActWarnCnt =3;

	Map<Id, Tuple<Id, Double>> linkenter = new HashMap<Id, Tuple<Id, Double>>();
	Map<Id, Tuple<Id, Double>> agentarrival = new HashMap<Id, Tuple<Id, Double>>();
	Map<Id, Tuple<Id, Double>> agentdeparture = new HashMap<Id, Tuple<Id, Double>>();

	//=== copied from MarginalCongestionHandler
	// If the following parameter is false, a Runtime Exception is thrown in case an agent is delayed by the storage capacity.
	final boolean allowForStorageCapacityConstraint = true;

	// If the following parameter is false, the delays resulting from the storage capacity are not internalized.
	final boolean calculateStorageCapacityConstraints = true;
	double delayNotInternalized_storageCapacity = 0.0;
	double delay_storageCapacity =0.0;

	ScenarioImpl scenario;
	EventsManager emissionEventsManager;
	final List<Id> ptVehicleIDs = new ArrayList<Id>();
	final Map<Id, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfo>();

	double totalInternalizedDelay = 0.0;
	double totalDelay = 0.0;
	double totalStorageDelay = 0.0;
	double delayNotInternalized_roundingErrors = 0.0;
	double delayNotInternalized_spillbackNoCausingAgent = 0.0;
	private final Map<Id, Double> agentId2storageDelay = new HashMap<Id, Double>();
	private final EmissionUtilsExtended eu = new EmissionUtilsExtended();
	private final Map<Id, Map<WarmPollutant, Double>> personId2StorageEmission = new HashMap<Id, Map<WarmPollutant,Double>>();
	//===

	public WarmEmissionHandlerImplV2(
			Vehicles emissionVehicles,
			WarmEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor, ScenarioImpl scenario) {

		this.emissionVehicles = emissionVehicles;
		this.network = scenario.getNetwork();
		this.emissionEventsManager = emissionEventsManager;
		this.warmEmissionAnalysisModule = new WarmEmissionAnalysisModuleImplV2(parameterObject, emissionEventsManager, emissionEfficiencyFactor);
		this.scenario = scenario;

		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			log.warn("Expecting a capacity period of 3600. Calculation might be wrong. Keep eyes open.");
		}

		if (this.scenario.getConfig().qsim().getFlowCapFactor() != 1.0) {
			log.warn("Flow capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().qsim().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().scenario().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}
	}

	@Override
	public void reset(int iteration) {
		linkLeaveCnt = 0;
		linkLeaveFirstActWarnCnt = 0;

		linkenter.clear();
		agentarrival.clear();
		agentdeparture.clear();

		warmEmissionAnalysisModule.reset();
		//===
		this.linkId2congestionInfo.clear();
		this.ptVehicleIDs.clear();
		this.delayNotInternalized_storageCapacity = 0.0;
		this.totalDelay = 0.0;
		this.totalInternalizedDelay = 0.0;
		this.delayNotInternalized_roundingErrors = 0.0;
		this.delayNotInternalized_spillbackNoCausingAgent = 0.0;
		//===
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects: " + event.toString());
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){// link travel time calculation not neccessary for other modes
			Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
			this.agentarrival.put(event.getPersonId(), linkId2Time);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		if (this.agentId2storageDelay.get(event.getPersonId()) == null) {
			// skip that person

		} else {
			if (this.agentId2storageDelay.get(event.getPersonId()) != 0.) {
				//				log.warn("A delay of " + this.agentId2storageDelay.get(event.getPersonId()) + " sec. resulting from spill-back effects was not internalized. Setting the delay to 0.");
				this.delayNotInternalized_spillbackNoCausingAgent += this.agentId2storageDelay.get(event.getPersonId());
			}
			this.agentId2storageDelay.put(event.getPersonId(), 0.);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){ // link travel time calculation not neccessary for other modes

			Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
			this.agentdeparture.put(event.getPersonId(), linkId2Time);
			//===
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
		}
		//===
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
		this.linkenter.put(event.getPersonId(), linkId2Time);

		//===
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());	
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getVehicleId(), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(event.getVehicleId(), event.getTime());
		}	
		//===
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		linkLeaveCnt++;
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		Double leaveTime = event.getTime();
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Double linkLength = link.getLength();
		Double freeVelocity = link.getFreespeed();
		String roadTypeString = link.getType();
		Integer roadType;
		Map<WarmPollutant, Double> stopAndGoWarmEmission = new HashMap<WarmPollutant, Double>(); // Other agents (agents left this link before current agent) will pay for these emissions;
		try{
			roadType = Integer.parseInt(roadTypeString);
		}
		catch(NumberFormatException e){
			log.error("Roadtype missing in network information!");
			throw new RuntimeException(e);
		}

		if(!this.linkenter.containsKey(personId)){
			//			link emissions can be included here as well, for that we require, stopAndGoDistance, which can be calculated as = stopAndGoSpeedFromTable * delays 
			if(linkLeaveFirstActWarnCnt < maxLinkLeaveFirstActWarnCnt){
				log.info("Person " + personId + " is ending its first activity of the day and leaving link " + linkId + " without having entered.");
				log.info("This is because of the MATSim logic that there is no link enter event for the link of the first activity");
				log.info("Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveFirstActWarnCnt == maxLinkLeaveFirstActWarnCnt) log.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveFirstActWarnCnt++;
			//			return;
		} else if (!this.linkenter.get(personId).getFirst().equals(linkId)){
			if(linkLeaveSomeActWarnCnt < maxLinkLeaveSomeActWarnCnt){
				log.warn("Person " + personId + " is ending an activity other than the first and leaving link " + linkId + " without having entered.");
				log.warn("This indicates that there is some inconsistency in vehicle use; please check your inital plans file for consistency.");
				log.warn("Thus, no emissions are calculated neither for this link leave event nor for the last link that was entered.");
				if (linkLeaveSomeActWarnCnt == maxLinkLeaveSomeActWarnCnt) log.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveSomeActWarnCnt++;
			//			return;
		} else {
			double enterTime = this.linkenter.get(personId).getSecond();
			double travelTime;
			if(!this.agentarrival.containsKey(personId) || !this.agentdeparture.containsKey(personId)){
				travelTime = leaveTime - enterTime;
			}
			else if(!this.agentarrival.get(personId).getFirst().equals(event.getLinkId())
					|| !this.agentdeparture.get(personId).getFirst().equals(event.getLinkId())){

				travelTime = leaveTime - enterTime;
			} else {
				double arrivalTime = this.agentarrival.get(personId).getSecond();		
				double departureTime = this.agentdeparture.get(personId).getSecond();	
				travelTime = leaveTime - enterTime - departureTime + arrivalTime;	
			}

			Id vehicleId = personId;
			String vehicleInformation;
			if(!this.emissionVehicles.getVehicles().containsKey(vehicleId)){
				throw new RuntimeException("No vehicle defined for person " + personId + ". " +
						"Please make sure that requirements for emission vehicles in " + 
						VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");
			}
			Vehicle vehicle = this.emissionVehicles.getVehicles().get(vehicleId);
			VehicleType vehicleType = vehicle.getType();
			vehicleInformation = vehicleType.getId().toString();

			Map<WarmPollutant, double[]> warmEmissions  = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
					personId,
					roadType,
					freeVelocity,
					linkLength,
					travelTime,
					vehicleInformation);
			log.info("Warm emissions have two parts, 1) own emissions 2) emissions caused by other users and therefore others should be charged for emissions 2).");
			// here throw two warm emissions events 1)for freeFlowWarmEmission 2) for stopAndGoWarmEmission
			Map<WarmPollutant, Double> freeFlowWarmEmission = new HashMap<WarmPollutant, Double>(); // Agent, itself will pay for these emissions


			for(WarmPollutant wp:warmEmissions.keySet()){
				freeFlowWarmEmission.put(wp, warmEmissions.get(wp)[0]);
				stopAndGoWarmEmission.put(wp, warmEmissions.get(wp)[1]);
			}
			warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, linkId, vehicleId, freeFlowWarmEmission);
		}
		//===
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one left this link before
				collectLinkInfos(event.getLinkId());
			}

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			if (linkInfo.getLeavingAgents().size() != 0) {
				// Clear tracking of persons leaving that link previously.
				double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
				double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			
				if (event.getTime() > Math.floor(earliestLeaveTime)+1 ){// Flow congestion has disappeared on that link.
			
					// Deleting the information of agents previously leaving that link.
					linkInfo.getLeavingAgents().clear();
					linkInfo.getPersonId2linkLeaveTime().clear();
				}
			}
			
			calculateCongestion(event,stopAndGoWarmEmission);

			linkInfo.getLeavingAgents().add(event.getVehicleId());
			linkInfo.getPersonId2linkLeaveTime().put(event.getVehicleId(), event.getTime());

			linkInfo.setLastLeavingAgent(event.getVehicleId());
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getVehicleId());
		}//===
	}

	public int getLinkLeaveCnt() {
		return linkLeaveCnt;
	}
	public int getLinkLeaveWarnCnt() {
		return linkLeaveFirstActWarnCnt;
	}

	public WarmEmissionAnalysisModuleImplV2 getWarmEmissionAnalysisModule(){
		return warmEmissionAnalysisModule;
	}

	private Map<WarmPollutant, Double> getDelaysEquivalentEmissions(double delaysRatio, Map<WarmPollutant, Double> emissionsIn){
		Map<WarmPollutant, Double> emissionsOut = new HashMap<WarmPollutant, Double>();

		for(WarmPollutant wm : emissionsIn.keySet()){
			emissionsOut.put(wm, delaysRatio*emissionsIn.get(wm));
		}
		return emissionsOut;
	}

	private void throwWarmEmissionEventAndStoreEmissionsDueToSpillBack(Map<WarmPollutant, Double> totalEmissions, LinkLeaveEvent event, double totalDelay){
		Map<WarmPollutant, Double> emissionsToPayFor = totalEmissions;
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id> reverseList = new ArrayList<Id>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);

		double delayToPayFor = totalDelay;
		for (Id id : reverseList){
			if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
				Map<WarmPollutant, Double> emissionCorrespondingToDelays = new HashMap<WarmPollutant, Double>();;
				if (event.getVehicleId().toString().equals(id.toString())) {
					//						log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
				} else {
					// using the time when the causing agent entered the link
					this.totalInternalizedDelay = this.totalInternalizedDelay + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
					emissionCorrespondingToDelays = getDelaysEquivalentEmissions(linkInfo.getMarginalDelayPerLeavingVehicle_sec()/delayToPayFor, emissionsToPayFor);
					warmEmissionAnalysisModule.throwWarmEmissionEvent(event.getTime(), event.getLinkId(), event.getVehicleId(), emissionCorrespondingToDelays);
				}
				delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				emissionsToPayFor = eu.subtractTwoWarmEmissionsMap(emissionsToPayFor, emissionCorrespondingToDelays);

			} else if(delayToPayFor > 0){
				if (event.getVehicleId().toString().equals(id.toString())) {
					//							log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
				} else {
					// using the time when the causing agent entered the link
					this.totalInternalizedDelay = this.totalInternalizedDelay + delayToPayFor;
					warmEmissionAnalysisModule.throwWarmEmissionEvent(event.getTime(), event.getLinkId(), event.getVehicleId(), emissionsToPayFor);
				}
				delayToPayFor = 0.;
				emissionsToPayFor = new HashMap<WarmPollutant, Double>();
			}
		}

		if (delayToPayFor <0) {
			throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + delayToPayFor + ") Aborting...");
		} else if(delayToPayFor==0) return;
		else if (delayToPayFor<= 1.){
			// The remaining delay of up to 1 sec may result from rounding errors. The delay caused by the flow capacity sometimes varies by 1 sec.
			// Setting the remaining delay to 0 sec.
			this.delayNotInternalized_roundingErrors += delayToPayFor;
			delayToPayFor = 0.;
		} else {	
			if (this.allowForStorageCapacityConstraint) {
				if (this.calculateStorageCapacityConstraints) {
					// Saving the delay resulting from the storage capacity constraint for later when reaching the bottleneck link.
					this.agentId2storageDelay.put(event.getVehicleId(), delayToPayFor);
					personId2StorageEmission.put(event.getVehicleId(), emissionsToPayFor);
				} else {
					this.delay_storageCapacity+=delayToPayFor;
					this.delayNotInternalized_storageCapacity += delayToPayFor;
					log.warn("Delay which is not internalized: " + this.delayNotInternalized_storageCapacity);
				}

			} else {
				throw new RuntimeException("There is a delay resulting from the storage capacity on link " + event.getLinkId() + ": " + delayToPayFor + 
						"; this delay and corresponding emissions are not carried over further. Aborting...");
			}
		}
	}

	private void collectLinkInfos(Id linkId) {
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));

		double flowCapacity_hour = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
		double marginalDelay_sec = ((1 / (flowCapacity_hour / this.scenario.getNetwork().getCapacityPeriod()) ) );
		linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);

		int storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) / network.getEffectiveCellSize()) * this.scenario.getConfig().qsim().getStorageCapFactor() );
		linkInfo.setStorageCapacity_cars(storageCapacity_cars);

		this.linkId2congestionInfo.put(link.getId(), linkInfo);
	}

	private double getLastLeavingTime(Map<Id, Double> personId2LinkLeaveTime) {

		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id id : personId2LinkLeaveTime.keySet()){
			if (personId2LinkLeaveTime.get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = personId2LinkLeaveTime.get(id);
			}
		}
		return lastLeavingFromThatLink;
	}

	private void calculateCongestion(LinkLeaveEvent event, Map<WarmPollutant, Double> stopAndGoWarmEmissionOnThisLink) {

		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		this.totalDelay = this.totalDelay + delayOnThisLink;

		// Check if this (affected) agent was previously delayed without internalizing the delay.
		double totalDelayWithDelaysOnPreviousLinks = 0.;
		Map<WarmPollutant, Double> emissionsWithDelaysOnPreviousLinks = new HashMap<WarmPollutant, Double>();

		if (this.agentId2storageDelay.get(event.getVehicleId()) == null) {
			totalDelayWithDelaysOnPreviousLinks = delayOnThisLink;
			emissionsWithDelaysOnPreviousLinks = stopAndGoWarmEmissionOnThisLink;
		} else {
			totalDelayWithDelaysOnPreviousLinks = delayOnThisLink + this.agentId2storageDelay.get(event.getVehicleId());
			emissionsWithDelaysOnPreviousLinks = eu.addTwoWarmEmissionsMap(stopAndGoWarmEmissionOnThisLink, personId2StorageEmission.get(event.getVehicleId()));
			this.agentId2storageDelay.put(event.getVehicleId(), 0.);
			personId2StorageEmission.put(event.getVehicleId(), new HashMap<WarmPollutant, Double>());
		}

		throwWarmEmissionEventAndStoreEmissionsDueToSpillBack(emissionsWithDelaysOnPreviousLinks, event, totalDelayWithDelaysOnPreviousLinks);
	}
}