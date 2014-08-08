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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import playground.agarwalamit.marginalTesting.WarmEmissionAnalysisModuleImplV2.WarmEmissionAnalysisModuleParameter;
import playground.agarwalamit.siouxFalls.emissionAnalyzer.EmissionUtilsExtended;


/**
 * @author amit
 */
public class WarmEmissionHandlerImplV2 implements 
LinkEnterEventHandler, 
LinkLeaveEventHandler, 
TransitDriverStartsEventHandler,
PersonArrivalEventHandler, 
PersonDepartureEventHandler,
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
	final Map<Id, LinkCongestionInfoExtended> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfoExtended>();

	double totalInternalizedDelay = 0.0;
	double totalDelay = 0.0;
	double totalStorageDelay = 0.0;
	double delayNotInternalized_roundingErrors = 0.0;
	double delayNotInternalized_spillbackNoCausingAgent = 0.0;
	private final EmissionUtilsExtended eu = new EmissionUtilsExtended();
	//===

	public WarmEmissionHandlerImplV2(
			Vehicles emissionVehicles,
			WarmEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor, ScenarioImpl scenario) {

		this.emissionVehicles = emissionVehicles;
		this.scenario = scenario;
		this.network = scenario.getNetwork();
		this.emissionEventsManager = emissionEventsManager;
		this.warmEmissionAnalysisModule = new WarmEmissionAnalysisModuleImplV2(parameterObject, emissionEventsManager, emissionEfficiencyFactor);

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
		for(Link link:network.getLinks().values()){
			linkIds2PersonEnterList.put(link.getId(), new ArrayList<Id>());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects "
				+ "because there are no linkLeaveEvents for stucked agents.: " + event.toString());
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
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){ // link travel time calculation not neccessary for other modes

			Tuple<Id, Double> linkId2Time = new Tuple<Id, Double>(event.getLinkId(), event.getTime());
			this.agentdeparture.put(event.getPersonId(), linkId2Time);
			//===
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
			LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
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

			LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
//			checkAndModifyLinkEnterAgentList(event);
			List<Id> linkEnterList = linkIds2PersonEnterList.get(event.getLinkId());
			linkEnterList.add(event.getVehicleId());
//			linkIds2PersonEnterList.put(event.getVehicleId(), linkEnterList);
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getVehicleId(), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(event.getVehicleId(), event.getTime());
		}	
		//===
	}
	private Map<Id, List<Id>> linkIds2PersonEnterList = new HashMap<Id, List<Id>>();

	private void checkAndModifyLinkEnterAgentList(LinkEnterEvent event){
		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		List<Id> linkEnterList = linkIds2PersonEnterList.get(event.getLinkId()); 
		double maxVehiclesOnLink = linkInfo.getStorageCapacityCars();
		double noVehicleOnLinkNow = linkEnterList.size();

		if(noVehicleOnLinkNow < maxVehiclesOnLink) {
			linkEnterList.add(event.getVehicleId());
		} else if (noVehicleOnLinkNow == (int) maxVehiclesOnLink){
			linkEnterList.remove(0);
			linkEnterList.add(event.getVehicleId());
		} 
		if(linkEnterList.size()>maxVehiclesOnLink) {
			throw new RuntimeException ("Number of vehicle on the link is more than allowed. Aborting!!!");
		}
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

		} else { // car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one left this link before
				collectLinkInfos(event.getLinkId());
			}

			LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			if (linkInfo.getLeavingAgents().size() != 0) {
				// Clear tracking of persons leaving that link previously.
				double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
				double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();

				if (event.getTime() > Math.floor(earliestLeaveTime)+1 ){// Flow congestion has disappeared on that link.

					// Deleting the information of agents previously leaving that link.
					linkInfo.getLeavingAgents().clear();
					linkInfo.getEnteringAgents().clear();
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

	private void calculateCongestion(LinkLeaveEvent event, Map<WarmPollutant, Double> stopAndGoWarmEmissionOnThisLink) {

		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		linkInfo.getPersonId2DelaysToPayFor().put(event.getVehicleId(), delayOnThisLink);
		linkInfo.getPersonId2WarmEmissionsToPayFor().put(event.getVehicleId(), stopAndGoWarmEmissionOnThisLink);
		this.totalDelay = this.totalDelay + delayOnThisLink;

		throwWarmEmissionEventDueToFlowAndStorageCapacityDelays( event);
	}

	private void chargingForDelaysAndThrowingEvents(double marginalDelaysPerLeavingVehicle, LinkLeaveEvent event, Id personToBeCharged){

		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(event.getVehicleId());
		Map<WarmPollutant, Double> emissionsToPayFor = linkInfo.getPersonId2WarmEmissionsToPayFor().get(event.getVehicleId());

		if (delayToPayFor > marginalDelaysPerLeavingVehicle) {
			Map<WarmPollutant, Double> emissionCorrespondingToDelays = new HashMap<WarmPollutant, Double>();
			if (event.getVehicleId().toString().equals(personToBeCharged.toString())) {
				System.out.println("\n \n \t \t Error \n \n");
				log.error("Causing agent and affected agents "+personToBeCharged.toString()+" are same. Delays at this point is "+delayToPayFor+" sec.");
				return;
				//				throw new RuntimeException("The causing agent and the affected agent are the same (" + personToBeCharged.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
			} else {
				// using the time when the causing agent entered the link
				this.totalInternalizedDelay = this.totalInternalizedDelay + marginalDelaysPerLeavingVehicle;
				emissionCorrespondingToDelays = getDelaysEquivalentEmissions(marginalDelaysPerLeavingVehicle/delayToPayFor, emissionsToPayFor);
				warmEmissionAnalysisModule.throwWarmEmissionEvent(event.getTime(), event.getLinkId(), personToBeCharged, emissionCorrespondingToDelays);
			}
			delayToPayFor = delayToPayFor - marginalDelaysPerLeavingVehicle;
			emissionsToPayFor = eu.subtractTwoWarmEmissionsMap(emissionsToPayFor, emissionCorrespondingToDelays);
		} else if(delayToPayFor > 0){
			if (event.getVehicleId().toString().equals(personToBeCharged.toString())) {
				System.out.println("Error");
				log.error("Causing agent and affected agents "+personToBeCharged.toString()+" are same. Delays at this point is "+delayToPayFor+" sec.");
				//				throw new RuntimeException("The causing agent and the affected agent are the same (" + personToBeCharged.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
			} else {
				// using the time when the causing agent entered the link
				this.totalInternalizedDelay = this.totalInternalizedDelay + delayToPayFor;
				warmEmissionAnalysisModule.throwWarmEmissionEvent(event.getTime(), event.getLinkId(), personToBeCharged, emissionsToPayFor);
			}
			delayToPayFor = 0.;
			emissionsToPayFor = new HashMap<WarmPollutant, Double>();
		}
		linkInfo.getPersonId2DelaysToPayFor().put(event.getVehicleId(), delayToPayFor);
		linkInfo.getPersonId2WarmEmissionsToPayFor().put(event.getVehicleId(), emissionsToPayFor);
	}

	private void throwWarmEmissionEventDueToFlowAndStorageCapacityDelays( LinkLeaveEvent event){
		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id> reverseList = new ArrayList<Id>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);

		double delayToPayFor =this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
		if(delayToPayFor>0) {
			Iterator< Id> personIdListIterator = reverseList.iterator();
			while (personIdListIterator.hasNext() && delayToPayFor>0){
				Id chargedPersonId = personIdListIterator.next();
				chargingForDelaysAndThrowingEvents(linkInfo.getMarginalDelayPerLeavingVehicle_sec(), event, chargedPersonId);
				delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
			}
		} else if(delayToPayFor==0) return;
		else throw new RuntimeException("Delays can not be negative, do a consistency check.");

		//spill back or/and rounding delays are left
		if(delayToPayFor==0) return; 
		else if (delayToPayFor<0.) throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + delayToPayFor + ") Aborting...");
		else if (delayToPayFor>0 && delayToPayFor<1){ // roundingError delays
			// The remaining delay of up to 1 sec may result from rounding errors. The delay caused by the flow capacity sometimes varies by 1 sec.
			// Setting the remaining delay to 0 sec.
			this.delayNotInternalized_roundingErrors += delayToPayFor;
			delayToPayFor = 0.;
			delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().put(event.getVehicleId(),delayToPayFor);
			log.warn(delayToPayFor + " seconds are not internalized assuming these delays due to rounding errors. Do check for emissions at this stage.");
			log.info("Emissions to pay for at this point are "+this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2WarmEmissionsToPayFor().get(event.getVehicleId()).toString());
		}  else {
			if (this.allowForStorageCapacityConstraint) {
				if (this.calculateStorageCapacityConstraints) {
					Id furtherChargingLinkId = new IdImpl("NA");
					if(reverseList.isEmpty()){ 
						Id causingLinkId = getNextLinkInRoute(event.getPersonId(),event.getLinkId());
						linkInfo.getPersonId2CausingLinkId().put(event.getVehicleId(), causingLinkId);
						furtherChargingLinkId = getCausedPersonsListAndChargeThem(causingLinkId, event);
					} else {
						for(Id id:reverseList){ 
							// iterate until causing link is identified.
							if(linkInfo.getPersonId2CausingLinkId().containsKey(id)){ 
								Id causingLinkId = linkInfo.getPersonId2CausingLinkId().get(id);
								furtherChargingLinkId = getCausedPersonsListAndChargeThem(causingLinkId, event);
							}
						} 
					}
					delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
					while(delayToPayFor>0.){
						furtherChargingLinkId = getCausedPersonsListAndChargeThem(furtherChargingLinkId, event);
						delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
					}

					//					if(delayToPayFor>0){
					//						log.error( "Delays to pay for is "+delayToPayFor+" sec. It means, delays due to storage capacity is not internalized correctly. Check!!!");
					//						this.delayNotInternalized_spillbackNoCausingAgent += delayToPayFor;
					//					}
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

	private Id getCausedPersonsListAndChargeThem(Id causingLinkId, LinkLeaveEvent event){
		Id chargeFurtherPersonsOnLinkId= new IdImpl("NA");
		double delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
		LinkCongestionInfoExtended linkInfoForCausingLink = linkId2congestionInfo.get(causingLinkId);

		if(linkInfoForCausingLink==null && delayToPayFor==1){ 
			log.warn("A person is leaving link with delay of 1 sec and there is no flow or storage capacity restriction. This could cause because of QSim. Setting delays to zero again.");
			this.delayNotInternalized_roundingErrors +=delayToPayFor;
			delayToPayFor = 0.;
			this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().put(event.getVehicleId(),0.);
		} else if(linkIds2PersonEnterList.get(causingLinkId)!=null) {
			List<Id> personIdToBeChargedList = new ArrayList<Id>(linkIds2PersonEnterList.get(causingLinkId)); 
			Collections.reverse(personIdToBeChargedList);
			Iterator< Id> personIdListIterator = personIdToBeChargedList.iterator();
			while(personIdListIterator.hasNext() && delayToPayFor>0){
				Id personCharged = personIdListIterator.next();
				chargingForDelaysAndThrowingEvents(linkInfoForCausingLink.getMarginalDelayPerLeavingVehicle_sec(), event, personCharged);
				delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
				chargeFurtherPersonsOnLinkId = getNextLinkInRoute(personCharged, causingLinkId);
			}
		}
		return chargeFurtherPersonsOnLinkId;
	}

	private Id getNextLinkInRoute(Id personId, Id linkId){
		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();

		Map<NetworkRoute, List<Id>> nRoutesAndLinkIds = new LinkedHashMap<NetworkRoute, List<Id>>(); // save all routes and links in each route
		for(PlanElement pe :planElements){
			if(pe instanceof Leg){
				NetworkRoute nRoute = ((NetworkRoute)((Leg)pe).getRoute()); 
				List<Id> linkIds = new ArrayList<Id>();
				linkIds.add(nRoute.getStartLinkId());
				linkIds.addAll(nRoute.getLinkIds());  
				linkIds.add(nRoute.getEndLinkId());
				nRoutesAndLinkIds.put(nRoute, linkIds);
			}
		}

		Id nextLinkInRoute =  new IdImpl("NA");
		for(NetworkRoute nr : nRoutesAndLinkIds.keySet()){
			List<Id> linkIds = nRoutesAndLinkIds.get(nr);
			Iterator<Id> it = linkIds.iterator();
			do{
				if (it.next().equals(linkId)) {
					if(it.hasNext()){ // hasNext() is used again to check if this is the destination link in network route
						if(nextLinkInRoute.equals(new IdImpl("NA"))){
							nextLinkInRoute = it.next();
							break;
						} else {
							System.out.println("Error");
							throw new RuntimeException("There is a situation where, person "+ personId+" leaving on link "+linkId+
									" and it have two next links. They are - "+nextLinkInRoute+" and "+ it.next()+". This situation may arise when a people is using same link in same direction for "
									+ "two different trips to different activities.");
						}
					}
				}
			} while(it.hasNext());
		}
		return nextLinkInRoute;
	}

	private void collectLinkInfos(Id linkId) {
		LinkCongestionInfoExtended linkInfo = new LinkCongestionInfoExtended();	

		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));
 
		double flowCapacity_hour = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
		double marginalDelay_sec = ((1 / (flowCapacity_hour / this.scenario.getNetwork().getCapacityPeriod()) ) );
		linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);

		double storageCapacity_cars = (Math.ceil((link.getLength() * link.getNumberOfLanes()) / network.getEffectiveCellSize()) * this.scenario.getConfig().qsim().getStorageCapFactor() );
		linkInfo.setStorageCapacityCars(storageCapacity_cars);

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

	public double getTotalDelaysInSecs(){
		log.info("\n total delays =  "+ this.totalDelay+
				"\n  delays not internalized rounding errors =  "+ this.delayNotInternalized_roundingErrors+
				"\n delays not internalized no causing agents =  "+ this.delayNotInternalized_spillbackNoCausingAgent+
				"\n delays not internalized storage capacity =  "+ this.delayNotInternalized_storageCapacity+
				"\n delays  storage capacity =  "+ this.delay_storageCapacity+
				"\n total storage delays =  "+ this.totalStorageDelay+
				"\n total internalized delays =  "+ this.totalInternalizedDelay
				);
		return this.totalDelay;
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
}