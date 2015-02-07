/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.noise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.util.StringUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;

import playground.ikaddoura.noise.NoiseEvent.CarOrHdv;
import playground.ikaddoura.noise.NoiseConfig;

/**
 * @author lkroeger
 *
 */

public class NoiseTollHandler implements NoiseEventHandler , NoiseEventAffectedHandler , LinkEnterEventHandler , LinkLeaveEventHandler , ActivityEndEventHandler , ActivityStartEventHandler{

	private static final Logger log = Logger.getLogger(NoiseTollHandler.class);
	
	private double timeBinSize = NoiseConfig.getTimeBinSize();
	
	private Scenario scenario;
	private EventsManager events;
	
	private List<NoiseEvent> noiseEvents = new ArrayList<NoiseEvent>();
	private List<NoiseEvent> noiseEventsCar = new ArrayList<NoiseEvent>();
	private List<NoiseEvent> noiseEventsHdv = new ArrayList<NoiseEvent>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	private List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();
	
	private Map<Id, Map<Double, Double>> linkId2timeBin2tollSum = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Integer>> linkId2timeBin2leavingAgents = new HashMap<Id, Map<Double, Integer>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgToll = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgTollOldValue = new HashMap<Id, Map<Double, Double>>();

	private Map<Id, Map<Double, Double>> linkId2timeBin2tollSumCar = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Integer>> linkId2timeBin2leavingAgentsCar = new HashMap<Id, Map<Double, Integer>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgTollCar = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgTollCarOldValue = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2tollSumHdv = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Integer>> linkId2timeBin2leavingAgentsHdv = new HashMap<Id, Map<Double, Integer>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgTollHdv = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgTollHdvOldValue = new HashMap<Id, Map<Double, Double>>();
	
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCost = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCostPerCar = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCostPerHdvVehicle = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,List<Id>>> linkId2timeInterval2leavingAgents = new HashMap<Id, Map<Double,List<Id>>>();
	private Map<Id,Map<Double,List<Id>>> linkId2timeInterval2leavingAgentsCar = new HashMap<Id, Map<Double,List<Id>>>();
	private Map<Id,Map<Double,List<Id>>> linkId2timeInterval2leavingAgentsHdv = new HashMap<Id, Map<Double,List<Id>>>();
	
	private Map<Id,Double> personId2tollSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2damageSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2homeBasedDamageSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2differenceTollDamage = new HashMap<Id, Double>();
	
	private Map<Id,Id> personId2homeReceiverPointId = new HashMap<Id, Id>();
	
	private Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2costShare = new HashMap<Id, Map<Double,Map<Id,Double>>>();
	
	private List<LinkEnterEvent> linkLeaveEvents = new ArrayList<LinkEnterEvent>();
	private List<LinkEnterEvent> linkLeaveEventsCar = new ArrayList<LinkEnterEvent>();
	private List<LinkEnterEvent> linkLeaveEventsHdv = new ArrayList<LinkEnterEvent>();
	
	private Map<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>> receiverPointId2personId2actNumber2activityStartAndActivityEnd = new HashMap<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>>();
	private Map<Id,Map<Integer,Map<Id,Tuple<Double, Double>>>> personId2actNumber2receiverPointId2activityStartAndActivityEnd = new HashMap<Id,Map<Integer,Map<Id,Tuple<Double, Double>>>>();
	private Map<Id,Map<Integer,String>> personId2actNumber2actType = new HashMap<Id, Map<Integer,String>>();
	private Map<Id,Integer> personId2actualActNumber = new HashMap<Id, Integer>();
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>> receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap<Id, Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>>();
	private Map<Id,List<Id>> receiverPointId2ListOfHomeAgents = new HashMap<Id, List<Id>>();
	
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCostAgentBased = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission = new HashMap<Id, Map<Double,Map<Id,Double>>>();
	
	private List<Id> tunnelLinks = new ArrayList<Id>();
	private List<Id> hdvVehicles = new ArrayList<Id>();
	private Map<Id,List<LinkEnterEvent>> linkId2linkLeaveEvents = new HashMap<Id, List<LinkEnterEvent>>();
	private Map<Id,List<LinkEnterEvent>> linkId2linkLeaveEventsCar = new HashMap<Id, List<LinkEnterEvent>>();
	private Map<Id,List<LinkEnterEvent>> linkId2linkLeaveEventsHdv = new HashMap<Id, List<LinkEnterEvent>>();
	private Map<Id, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkLeaveEvents = new HashMap<Id, Map<Double,List<LinkEnterEvent>>>();
	private Map<Id, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkLeaveEventsCar = new HashMap<Id, Map<Double,List<LinkEnterEvent>>>();
	private Map<Id, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkLeaveEventsHdv = new HashMap<Id, Map<Double,List<LinkEnterEvent>>>();
	
	Map<Double,Integer> interval2departures = new HashMap<Double, Integer>();
	
	private double totalToll = 0.;
	private double totalTollAffected = 0.;
	private double totalTollAffectedAgentBasedCalculation = 0.;
	private double totalTollAffectedAgentBasedCalculationControl = 0.;
	
	Map<Id,Map<Id,Map<Double,Double>>> personId2receiverPointId2timeInterval2costs = new HashMap<Id, Map<Id,Map<Double,Double>>>();
	
	private Map<Integer,Double> iteration2tollSum = new HashMap<Integer, Double>();
	private Map<Integer,Double> iteration2tollSumCar = new HashMap<Integer, Double>();
	private Map<Integer,Double> iteration2tollSumHdv = new HashMap<Integer, Double>();
	
	private SpatialInfo spatialInfo;
	private NoiseImmissionCalculator noiseImmissionCalculator;
	private NoiseEmissionCalculator noiseEmissionCalculator;
	private double annualCostRate;
	
	private double vtts_car;
	
	double nGesamt = 0.;
	
	public NoiseTollHandler (Scenario scenario , EventsManager events, SpatialInfo spatialInfo, double annualCostRate) {
		this.scenario = scenario;
		this.events = events;
		this.spatialInfo = spatialInfo;
		this.noiseImmissionCalculator = new NoiseImmissionCalculator(spatialInfo);
		this.noiseEmissionCalculator = new NoiseEmissionCalculator();
		
		this.vtts_car = (scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS_car: " + vtts_car);
		
		if (annualCostRate == 0.) {
			log.warn("Annual cost rate is zero. Setting the annual cost rate by the EWS value: (85.0/(1.95583))*(Math.pow(1.02, (2014-1995))).");
			this.annualCostRate = (85.0/(1.95583))*(Math.pow(1.02, (2014-1995)));

		} else {
			this.annualCostRate = annualCostRate;
		}
	}
	
	@Override
	public void reset(int iteration) {
		
		personId2homeReceiverPointId.clear();
		
		linkId2timeBin2tollSum.clear();
		linkId2timeBin2leavingAgents.clear();
		linkId2timeBin2tollSumCar.clear();
		linkId2timeBin2leavingAgentsCar.clear();
		linkId2timeBin2tollSumHdv.clear();
		linkId2timeBin2leavingAgentsHdv.clear();
		
		linkId2timeBin2avgTollOldValue.clear();
		linkId2timeBin2avgTollOldValue.putAll(linkId2timeBin2avgToll);
		linkId2timeBin2avgTollCarOldValue.clear();
		linkId2timeBin2avgTollCarOldValue.putAll(linkId2timeBin2avgTollCar);
		linkId2timeBin2avgTollHdvOldValue.clear();
		linkId2timeBin2avgTollHdvOldValue.putAll(linkId2timeBin2avgTollHdv);
		linkId2timeBin2avgToll.clear();
		linkId2timeBin2avgTollCar.clear();
		linkId2timeBin2avgTollHdv.clear();
		
		noiseEvents.clear();
		noiseEventsCar.clear();
		noiseEventsHdv.clear();
		noiseEventsAffected.clear();
		moneyEvents.clear();
		linkId2timeInterval2damageCost.clear();
		linkId2timeInterval2damageCostPerCar.clear();
		linkId2timeInterval2damageCostPerHdvVehicle.clear();
		linkId2timeInterval2leavingAgents.clear();
		linkId2timeInterval2leavingAgentsCar.clear();
		linkId2timeInterval2leavingAgentsHdv.clear();
		receiverPointIds2timeIntervals2noiseLinks2costShare.clear();
		personId2tollSum.clear();
		personId2damageSum.clear();
		personId2homeBasedDamageSum.clear();
		personId2differenceTollDamage.clear();
		personId2homeReceiverPointId.clear();
		
		receiverPointId2ListOfHomeAgents.clear();
		receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.clear();
		receiverPointId2personId2actNumber2activityStartAndActivityEnd.clear();
		personId2actNumber2receiverPointId2activityStartAndActivityEnd.clear();
		personId2actNumber2actType.clear();
		personId2actualActNumber.clear();
		receiverPointId2timeInterval2affectedAgentUnits.clear();
		receiverPointId2timeInterval2damageCost.clear();
		receiverPointId2timeInterval2damageCostAgentBased.clear();
		receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.clear();
		linkId2timeInterval2noiseEmission.clear();
		receiverPointId2timeInterval2noiseImmission.clear();
		receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.clear();
		linkLeaveEvents.clear();
		linkLeaveEventsCar.clear();
		linkLeaveEventsHdv.clear();
		linkId2linkLeaveEvents.clear();
		linkId2linkLeaveEventsCar.clear();
		linkId2linkLeaveEventsHdv.clear();
		linkId2linkLeaveEvents.clear();
		linkId2timeInterval2linkLeaveEvents.clear();
		linkId2timeInterval2linkLeaveEventsCar.clear();
		linkId2timeInterval2linkLeaveEventsHdv.clear();

		personId2receiverPointId2timeInterval2costs.clear();
		
		log.info("totalTollAffectedAgentBasedCalculation: "+totalTollAffectedAgentBasedCalculation);
		totalTollAffectedAgentBasedCalculation = 0.;
		log.info("totalTollAffectedAgentBasedCalculationControl: "+totalTollAffectedAgentBasedCalculationControl);
		totalTollAffectedAgentBasedCalculationControl = 0.;
		log.info("totalToll in previous iteration: "+totalToll);
		totalToll = 0.;
		log.info("totalTollAffected in previous iteration: "+totalTollAffected);
		totalTollAffected = 0.;
	}
	
	public void setTunnelLinks() {
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			String linkIdString = scenario.getNetwork().getLinks().get(linkId).toString();
			if(
					(StringUtils.endsWithIgnoreCase(linkIdString, "test"))
//					(StringUtils.endsWithIgnoreCase(linkIdString, "tunnelLink")) ||
//					(StringUtils.endsWithIgnoreCase(linkIdString, "1"))
			) {
						tunnelLinks.add(linkId);
			} else {
				// do nothing: no tunnel link
			}
		}
	}
	
	public void setRailLinks() {
		// Railroad tracks and especially subays/undergrounds shpuld be excluded from the noise calculation
		// A list of the Ids should be saved
		// When setting the relevant linkIds for the receiver-points, the Ids from the list can be excluded then,
		// This can be done by a query of the allowed modes of the links
		// The bus vehicles should be considered for the calculation of the noise emissions from links,
		// for trams it is not so clear
		// The non-paid part of the noise costs, caused by pt-vehicles should be calculated later on
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			String linkIdString = scenario.getNetwork().getLinks().get(linkId).toString();
			if(
					(StringUtils.endsWithIgnoreCase(linkIdString, "test"))
//					(StringUtils.endsWithIgnoreCase(linkIdString, "railLink")) ||
//					(StringUtils.endsWithIgnoreCase(linkIdString, "1"))
			) {
						tunnelLinks.add(linkId);
			} else {
				// do nothing: no tunnel link
			}
		}
	}
	
	public void setNoiseBarrierLinks() {
		// TODO: not implemented yet,
		// but if tunnels are considered, it would make sense, too.
		//
		// A double calculation of barrier effects (buildings and barriers should be 
		//
		// The definition of links with noise-barriers can be set here
		// In a map, the links may be saved with the information of the height of the noise barrier
		// Map<Id,Double> linkId2noiseBarrierHeight
		// The resulting noise barrier effect depends then on the height of the receiver point and the height of the noise barrier
		// dependent from the density of activity-locations
		// but also from the difference to the link (and by this to the already given shielding term due to the existence of other buildings
	}
	
	public void setHdvVehicles() {
		for(Person person: scenario.getPopulation().getPersons().values()) {
			String personIdString = person.getId().toString();
			// TODO: For each scenario the identification of the hdv-vehicles has to be adapted!
//			if(StringUtils.endsWithIgnoreCase(personIdString, "hdv")) {
			
			if( //These lines are for testing
					(StringUtils.endsWithIgnoreCase(personIdString, "test"))
//					(StringUtils.endsWithIgnoreCase(personIdString, "1")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "2")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "3")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "4")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "5")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "6")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "7")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "8")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "9")) ||
//					(StringUtils.endsWithIgnoreCase(personIdString, "0")) 
					) { 
				hdvVehicles.add(person.getId());
			} else {
				// do nothing: car vehicle
			}
		}
		
		// TODO: Consider pt-vehicles
		Logger.getLogger(this.getClass()).fatal("cannot say if the following should be vehicles or transit vehicles; aborting ... .  kai, feb'15");
		System.exit(-1); 
		if(!(scenario.getTransitVehicles() == null)) {
			for(Id transitVehicleId : scenario.getTransitVehicles().getVehicles().keySet()) {
				hdvVehicles.add(transitVehicleId);
			}
		}
	}
	
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(!(scenario.getPopulation().getPersons().containsKey(event.getVehicleId()))) {
		} else {
		
		if(hdvVehicles.contains(event.getVehicleId())) {
			// hdv vehicle
			if (linkId2linkLeaveEventsHdv.containsKey(event.getLinkId())) {
				List<LinkEnterEvent> listTmp = linkId2linkLeaveEventsHdv.get(event.getLinkId());
				listTmp.add(event);
				linkId2linkLeaveEventsHdv.put(event.getLinkId(), listTmp);
			} else {
				List<LinkEnterEvent> listTmp = new ArrayList<LinkEnterEvent>();
				listTmp.add(event);
				linkId2linkLeaveEventsHdv.put(event.getLinkId(), listTmp);
			}
			linkLeaveEventsHdv.add(event);
		} else {
			// car
			if (linkId2linkLeaveEventsCar.containsKey(event.getLinkId())) {
				List<LinkEnterEvent> listTmp = linkId2linkLeaveEventsCar.get(event.getLinkId());
				listTmp.add(event);
				linkId2linkLeaveEventsCar.put(event.getLinkId(), listTmp);
			} else {
				List<LinkEnterEvent> listTmp = new ArrayList<LinkEnterEvent>();
				listTmp.add(event);
				linkId2linkLeaveEventsCar.put(event.getLinkId(), listTmp);
			}
			linkLeaveEventsCar.add(event);
		}
		
		if (linkId2linkLeaveEvents.containsKey(event.getLinkId())) {
			List<LinkEnterEvent> listTmp = linkId2linkLeaveEvents.get(event.getLinkId());
			listTmp.add(event);
			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
		} else {
			List<LinkEnterEvent> listTmp = new ArrayList<LinkEnterEvent>();
			listTmp.add(event);
			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
		}
		linkLeaveEvents.add(event);
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		// Instead of the linkLeave-based implementation,
		// the linkEnterEvents are used now
		
//		if(!(scenario.getPopulation().getPersons().containsKey(event.getVehicleId()))) {
//		} else {
//		
//		if(hdvVehicles.contains(event.getVehicleId())) {
//			// hdv vehicle
//			if (linkId2linkLeaveEventsHdv.containsKey(event.getLinkId())) {
//				List<LinkLeaveEvent> listTmp = linkId2linkLeaveEventsHdv.get(event.getLinkId());
//				listTmp.add(event);
//				linkId2linkLeaveEventsHdv.put(event.getLinkId(), listTmp);
//			} else {
//				List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
//				listTmp.add(event);
//				linkId2linkLeaveEventsHdv.put(event.getLinkId(), listTmp);
//			}
//			linkLeaveEventsHdv.add(event);
//		} else {
//			// car
//			if (linkId2linkLeaveEventsCar.containsKey(event.getLinkId())) {
//				List<LinkLeaveEvent> listTmp = linkId2linkLeaveEventsCar.get(event.getLinkId());
//				listTmp.add(event);
//				linkId2linkLeaveEventsCar.put(event.getLinkId(), listTmp);
//			} else {
//				List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
//				listTmp.add(event);
//				linkId2linkLeaveEventsCar.put(event.getLinkId(), listTmp);
//			}
//			linkLeaveEventsCar.add(event);
//		}
//		
//		if (linkId2linkLeaveEvents.containsKey(event.getLinkId())) {
//			List<LinkLeaveEvent> listTmp = linkId2linkLeaveEvents.get(event.getLinkId());
//			listTmp.add(event);
//			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
//		} else {
//			List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
//			listTmp.add(event);
//			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
//		}
//		linkLeaveEvents.add(event);
//		}
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!(scenario.getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
		
		if(!event.getActType().toString().equals("pt_interaction")) {
			Id personId = event.getPersonId();
			
			personId2actualActNumber.put(event.getPersonId(), personId2actualActNumber.get(event.getPersonId())+1);
			int actNumber = personId2actualActNumber.get(personId);
			double time = event.getTime();
			Coord coord = spatialInfo.getPersonId2listOfCoords().get(personId).get(actNumber-1);
			Id receiverPointId = spatialInfo.getActivityCoord2receiverPointId().get(coord);
			
			double startTime = time;
			double EndTime = 30*3600;
			Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
			Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
			receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
			Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
			actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
			personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
			
			String actType = event.getActType();
			Map <Integer,String> actNumber2actType = personId2actNumber2actType.get(personId);
			actNumber2actType.put(actNumber,actType);
			personId2actNumber2actType.put(personId,actNumber2actType);
			
			if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.containsKey(receiverPointId)) {
				// already at least one activity at this receiverPoint
				if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
					// already at least the second activity of this person at this receiverPoint
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId);
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
				} else {
					// the first activity of this person at this receiverPoint
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
				}
			} else {
				// the first activity at this receiver Point
				Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
				actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
				Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = new HashMap<Id, Map<Integer,Tuple<Double,Double>>>();
				personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
				receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
			}
		} else {
			// do nothing
		}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!(scenario.getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
			
			double interval = ((((int) (event.getTime()/3600.))*3600.)+3600.);
			int newValue = 1;
			if(interval2departures.containsKey(interval)) {
				int valueBefore = interval2departures.get(interval);
				newValue = valueBefore + 1;
			}
			interval2departures.put(interval, newValue);
			
		if(!event.getActType().toString().equals("pt_interaction")) {
			Id personId = event.getPersonId();
			if(!(personId2actualActNumber.containsKey(event.getPersonId()))) {
				personId2actualActNumber.put(event.getPersonId(), 1);
			}
			int actNumber = personId2actualActNumber.get(personId);
			double time = event.getTime();

			Coord coord = spatialInfo.getPersonId2listOfCoords().get(personId).get(actNumber-1);
			Id receiverPointId = spatialInfo.getActivityCoord2receiverPointId().get(coord);
			
			if(personId2actNumber2receiverPointId2activityStartAndActivityEnd.containsKey(personId)) {
				// not the first activity
				double startTime = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId).get(actNumber).get(receiverPointId).getFirst();
				double EndTime = time;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
				receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
				Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
				actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
				personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
				
			} else {
				// the first activity
				double startTime = 0.;
				double EndTime = time;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
				receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
				Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = new HashMap<Integer, Map<Id,Tuple<Double,Double>>>();
				actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
				personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);

				String actType = event.getActType();
				Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
				actNumber2actType.put(actNumber,actType);
				personId2actNumber2actType.put(personId, actNumber2actType);
				
				if(receiverPointId2ListOfHomeAgents.containsKey(receiverPointId)) {
					List <Id> listOfHomeAgents = receiverPointId2ListOfHomeAgents.get(receiverPointId);
					listOfHomeAgents.add(personId);
					receiverPointId2ListOfHomeAgents.put(receiverPointId, listOfHomeAgents);
				} else {
					List <Id> listOfHomeAgents = new ArrayList<Id>();
					listOfHomeAgents.add(personId);
					receiverPointId2ListOfHomeAgents.put(receiverPointId, listOfHomeAgents);
				}
			}
			
			if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.containsKey(receiverPointId)) {
				// already at least one activity at this receiver point
				if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
					// at least the second activity of this person at this receiver point
					double startTime = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getFirst();
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId);
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);

				} else {
					// the first activity of this person at this receiver point
					double startTime = 0.; // this must be the home activity in the morning;
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);

					String actType = event.getActType();
					Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
					actNumber2actType.put(actNumber,actType);
					personId2actNumber2actType.put(personId, actNumber2actType);
				}
			} else {
				// the first activity at this receiver point
				double startTime = 0.; // this must be the home activity in the morning;
				double EndTime = time;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
				actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
				Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = new HashMap<Id, Map<Integer,Tuple<Double,Double>>>();
				personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
				receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);

				String actType = event.getActType();
				Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
				actNumber2actType.put(actNumber,actType);
				personId2actNumber2actType.put(personId, actNumber2actType);
			}
			
		} else { 
			// do nothing
		}
		}
	}
	
	public void calculateFinalNoiseEmissions() {
		
		// initializing the maps
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,List<LinkEnterEvent>> timeInterval2linkLeaveEvents = new HashMap<Double, List<LinkEnterEvent>>();
			Map<Double,List<LinkEnterEvent>> timeInterval2linkLeaveEventsCar = new HashMap<Double, List<LinkEnterEvent>>();
			Map<Double,List<LinkEnterEvent>> timeInterval2linkLeaveEventsHdv = new HashMap<Double, List<LinkEnterEvent>>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				List<LinkEnterEvent> listLinkLeaveEvents = new ArrayList<LinkEnterEvent>();
				List<LinkEnterEvent> listLinkLeaveEventsCar = new ArrayList<LinkEnterEvent>();
				List<LinkEnterEvent> listLinkLeaveEventsHdv = new ArrayList<LinkEnterEvent>();
				timeInterval2linkLeaveEvents.put(timeInterval, listLinkLeaveEvents);
				timeInterval2linkLeaveEventsCar.put(timeInterval, listLinkLeaveEventsCar);
				timeInterval2linkLeaveEventsHdv.put(timeInterval, listLinkLeaveEventsHdv);
			}
			// sort the linkLeaveEvents by linkIds and timeIntervals
			if(linkId2linkLeaveEvents.containsKey(linkId)) {
				for(LinkEnterEvent linkLeaveEvent : linkId2linkLeaveEvents.get(linkId)) {
					double time = linkLeaveEvent.getTime();
					double timeInterval = 0.;
					if((time % NoiseConfig.getIntervalLength()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (((int)(time/NoiseConfig.getIntervalLength()))*NoiseConfig.getIntervalLength()) + NoiseConfig.getIntervalLength();
					}
					List<LinkEnterEvent> linkLeaveEvents = timeInterval2linkLeaveEvents.get(timeInterval);
					linkLeaveEvents.add(linkLeaveEvent);
					timeInterval2linkLeaveEvents.put(timeInterval, linkLeaveEvents);
				}
			}
			if(linkId2linkLeaveEventsCar.containsKey(linkId)) {
				for(LinkEnterEvent linkLeaveEventCar : linkId2linkLeaveEventsCar.get(linkId)) {
					double time = linkLeaveEventCar.getTime();
					double timeInterval = 0.;
					if((time % NoiseConfig.getIntervalLength()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (((int)(time/NoiseConfig.getIntervalLength()))*NoiseConfig.getIntervalLength()) + NoiseConfig.getIntervalLength();
					}
					List<LinkEnterEvent> linkLeaveEventsCar = timeInterval2linkLeaveEventsCar.get(timeInterval);
					linkLeaveEventsCar.add(linkLeaveEventCar);
					timeInterval2linkLeaveEventsCar.put(timeInterval, linkLeaveEventsCar);
				}
			}
			if(linkId2linkLeaveEventsHdv.containsKey(linkId)) {
				for(LinkEnterEvent linkLeaveEventHdv : linkId2linkLeaveEventsHdv.get(linkId)) {
					double time = linkLeaveEventHdv.getTime();
					double timeInterval = 0.;
					if((time % NoiseConfig.getIntervalLength()) == 0) {
						timeInterval = time;
					} else {
						timeInterval = (((int)(time/NoiseConfig.getIntervalLength()))*NoiseConfig.getIntervalLength()) + NoiseConfig.getIntervalLength();
					}
					List<LinkEnterEvent> linkLeaveEventsHdv = timeInterval2linkLeaveEventsHdv.get(timeInterval);
					linkLeaveEventsHdv.add(linkLeaveEventHdv);
					timeInterval2linkLeaveEventsHdv.put(timeInterval, linkLeaveEventsHdv);
				}
			}
			linkId2timeInterval2linkLeaveEvents.put(linkId, timeInterval2linkLeaveEvents);
			linkId2timeInterval2linkLeaveEventsCar.put(linkId, timeInterval2linkLeaveEventsCar);
			linkId2timeInterval2linkLeaveEventsHdv.put(linkId, timeInterval2linkLeaveEventsHdv);
		}
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()){
			Map<Double,Double> timeInterval2NoiseEmission = new HashMap<Double, Double>();
			double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed())*3.6;
			double vHdv = vCar;
			// TODO: If different speeds for different vehicle types have to be considered, adapt the calculation here.
			// For example, a maximum speed for hdv-vehicles could be set here (for instance for German highways) 
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				double noiseEmission = 0.;

				int N_car = linkId2timeInterval2linkLeaveEventsCar.get(linkId).get(timeInterval).size();
				int N_hdv = linkId2timeInterval2linkLeaveEventsHdv.get(linkId).get(timeInterval).size();
				int N = N_car + N_hdv;
				double p = 0.;
				
				if(!(N == 0)) {
					p = N_hdv / ((double) N);
				}
				if(!(N == 0)) {
//					// correction for a sample, multiplicate the scale factor
					N = (int) (N * (NoiseConfig.getScaleFactor()));
					// correction for intervals unequal to 3600 seconds (= one hour)
					N = (int) (N * (3600./NoiseConfig.getIntervalLength()));
					noiseEmission = noiseEmissionCalculator.calculateEmissionspegel(N, p, vCar, vHdv);
				}	
				timeInterval2NoiseEmission.put(timeInterval, noiseEmission);
			}
			linkId2timeInterval2noiseEmission.put(linkId , timeInterval2NoiseEmission);
		}
	}

	public void calculateImmissionSharesPerReceiverPointPerTimeInterval() {
		for(Id coordId : spatialInfo.getReceiverPoints().keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2isolatedImmission = new HashMap<Double, Map<Id,Double>>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
			 	Map<Id,Double> noiseLinks2isolatedImmission = new HashMap<Id, Double>();
//				if(!(spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)==null)) {
					for(Id linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
						double noiseEmission = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);
						double noiseImmission = 0.;
						Coord coord = spatialInfo.getReceiverPoints().get(coordId);
						if(!(noiseEmission == 0.)) {
							if(NoiseConfig.getRLSMethod().toString().equals("straights")) {
								noiseImmission = noiseImmissionCalculator.calculateNoiseImmission(scenario , linkId , noiseEmission , coord);						
//								log.info("Coords1: "+spatialInfo.getReceiverPoints().get(coordId));
							} else if(NoiseConfig.getRLSMethod().toString().equals("parts")) {
								noiseImmission = noiseImmissionCalculator.calculateNoiseImmissionParts(scenario , coordId , linkId , noiseEmission, spatialInfo.getActivityCoords2densityValue().get(coord), spatialInfo.getReceiverPointId2RelevantLinkIds2AdditionalValue().get(coordId).get(linkId));
							} else {
								log.warn("unknown RLS-method");
							}
						} else {
//							log.info("emission is 0");
						}
						noiseLinks2isolatedImmission.put(linkId,noiseImmission);
					}
					timeIntervals2noiseLinks2isolatedImmission.put(timeInterval, noiseLinks2isolatedImmission);
//				}
//				else {
//					// if no link has to to be considered for the calculation due to too long distances
//					timeIntervals2noiseLinks2isolatedImmission.put(timeInterval, noiseLinks2isolatedImmission);
//				}
			}
			receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.put(coordId, timeIntervals2noiseLinks2isolatedImmission);
		}
	}
	
	public void calculateFinalNoiseImmissions() {
		for(Id coordId : spatialInfo.getReceiverPoints().keySet()) {
			Map<Double,Double> timeInterval2noiseImmission = new HashMap<Double, Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				List<Double> noiseImmissions = new ArrayList<Double>();
				if(!(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval)==null)) {
					for(Id linkId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).keySet()) {
						if(!(linkId2timeInterval2linkLeaveEvents.get(linkId).get(timeInterval).size()==0.)) {
							noiseImmissions.add(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).get(linkId));
						}
					}	
					double resultingNoiseImmission = noiseImmissionCalculator.calculateResultingNoiseImmission(noiseImmissions);
					timeInterval2noiseImmission.put(timeInterval, resultingNoiseImmission);
				} else {
					// if no link has to to be considered for the calculation due to too long distances
					timeInterval2noiseImmission.put(timeInterval, 0.);
				}
			}
			receiverPointId2timeInterval2noiseImmission.put(coordId, timeInterval2noiseImmission);
		}
	}
	
	int ldenCounter = 0;
	
	public void calculatePersonId2Lden() {
		
		Map<Id,Double> personId2Lden = new HashMap<Id, Double>();
		
		for(Id personId : scenario.getPopulation().getPersons().keySet()) {
			ldenCounter++;
			if(ldenCounter%10000. == 0) {
				log.info(ldenCounter);
			}
			Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2actStartAndActEnd = new HashMap<Integer, Map<Id,Tuple<Double,Double>>>();
			actNumber2receiverPointId2actStartAndActEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
			Map<Id,Map<Double,Tuple<Double,Double>>> receiverPointId2timeInterval2durationOfStayAndNoiseImmission = new HashMap<Id, Map<Double,Tuple<Double,Double>>>();
			
			for(int actNumber : actNumber2receiverPointId2actStartAndActEnd.keySet()) {
				Id receiverPointId = null;
				for(Id id : actNumber2receiverPointId2actStartAndActEnd.get(actNumber).keySet()) {
					// only one key
					receiverPointId = id;
				}
				Map<Double,Double> timeInterval2noiseImmission = receiverPointId2timeInterval2noiseImmission.get(receiverPointId);
				double actStart = actNumber2receiverPointId2actStartAndActEnd.get(actNumber).get(receiverPointId).getFirst();
				double actEnd = actNumber2receiverPointId2actStartAndActEnd.get(actNumber).get(receiverPointId).getSecond();
				Map<Double,Tuple<Double,Double>> timeInterval2durationOfStayAndNoiseImmission = new HashMap<Double, Tuple<Double,Double>>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					if(!(actStart>(timeInterval-NoiseConfig.getIntervalLength()))) {
						if(actStart<(timeInterval-NoiseConfig.getIntervalLength())) {
							if(actEnd>timeInterval) {
								double factor = 1.;
								timeInterval2durationOfStayAndNoiseImmission.put(timeInterval, new Tuple<Double,Double>(factor, timeInterval2noiseImmission.get(timeInterval)));
							} else if (actEnd<(timeInterval-NoiseConfig.getIntervalLength())){
								double factor = 0.;
								timeInterval2durationOfStayAndNoiseImmission.put(timeInterval, new Tuple<Double,Double>(factor, timeInterval2noiseImmission.get(timeInterval)));
							} else {
								double factor = (actEnd - (timeInterval-NoiseConfig.getIntervalLength()))/NoiseConfig.getIntervalLength();
								timeInterval2durationOfStayAndNoiseImmission.put(timeInterval, new Tuple<Double,Double>(factor, timeInterval2noiseImmission.get(timeInterval)));
							}
						} else if(actStart < timeInterval) {	
							if(actEnd>timeInterval) {
								double factor = (timeInterval - actStart)/NoiseConfig.getIntervalLength();
								timeInterval2durationOfStayAndNoiseImmission.put(timeInterval, new Tuple<Double,Double>(factor, timeInterval2noiseImmission.get(timeInterval)));
							} else {
								double factor = (actEnd - actStart)/NoiseConfig.getIntervalLength();
								timeInterval2durationOfStayAndNoiseImmission.put(timeInterval, new Tuple<Double,Double>(factor, timeInterval2noiseImmission.get(timeInterval)));
							}
						} else {
							double factor = 0.;
							timeInterval2durationOfStayAndNoiseImmission.put(timeInterval, new Tuple<Double,Double>(factor, timeInterval2noiseImmission.get(timeInterval)));
						}
					}
				}
				if(receiverPointId2timeInterval2durationOfStayAndNoiseImmission.containsKey(receiverPointId)) {
					Map<Double,Tuple<Double,Double>> timeInterval2durationOfStayAndNoiseImmissionBefore = receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId);
					for(double timeInterval : timeInterval2durationOfStayAndNoiseImmission.keySet()) {
						if(timeInterval2durationOfStayAndNoiseImmissionBefore.containsKey(timeInterval)) {
							double newFactor = (timeInterval2durationOfStayAndNoiseImmissionBefore.get(timeInterval).getFirst()) + (timeInterval2durationOfStayAndNoiseImmission.get(timeInterval).getFirst());
							Tuple<Double,Double> newTuple = new Tuple<Double, Double>(newFactor, timeInterval2durationOfStayAndNoiseImmissionBefore.get(timeInterval).getSecond());
							timeInterval2durationOfStayAndNoiseImmissionBefore.put(timeInterval, newTuple);
						} else {
							timeInterval2durationOfStayAndNoiseImmissionBefore.put(timeInterval, timeInterval2durationOfStayAndNoiseImmission.get(timeInterval));
						}
					}
					receiverPointId2timeInterval2durationOfStayAndNoiseImmission.put(receiverPointId, timeInterval2durationOfStayAndNoiseImmissionBefore);
				} else {
					receiverPointId2timeInterval2durationOfStayAndNoiseImmission.put(receiverPointId, timeInterval2durationOfStayAndNoiseImmission);
				}
			}
			
			List<Tuple<Double,Double>> listTmp = new ArrayList<Tuple<Double,Double>>();
			double n = 0;
			double sum = 0;
			for(Id receiverPointId : receiverPointId2timeInterval2durationOfStayAndNoiseImmission.keySet()) {
				for(double timeInterval : receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).keySet()) {
					listTmp.add(receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).get(timeInterval));
					double factor = receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).get(timeInterval).getFirst();
					double immission = receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).get(timeInterval).getSecond();
					if(timeInterval<=6*3600) {
						immission = immission + 10.;
					} else if(timeInterval>6*3600 && timeInterval<=22*3600) {
					} else if(timeInterval>18*3600 && timeInterval<=22*3600) {
						immission = immission + 5.;
					} else if(timeInterval>22*3600 && timeInterval<=24*3600) {
						immission = immission + 10.;
					}

					n = n + factor;
					sum = sum + (factor*(Math.pow(10,0.1*immission)));
				}
			}
			double differenceTo24Hours = 24. - n;
//			sum = sum + (differenceTo24Hours*1.); // 10.^(0.1*0.) = 1.
//			double Lden = 10 * Math.log10((1./24.)*(sum));
			double Lden = 10 * Math.log10((1./n)*(sum)); //
			
			nGesamt = nGesamt + n;
			
			personId2Lden.put(personId, Lden);
			
			double tabularCostPerYear = calculateCostPerYearCorrespondingToLdenValue(Lden);
			double costPerDay = tabularCostPerYear/365.;
			double costPerAgent = costPerDay;
//			double costPerAgent = (n/24.)*costPerDay;

			totalTollAffectedAgentBasedCalculation = totalTollAffectedAgentBasedCalculation + costPerAgent;
			
			double LForCalculation = 10 * Math.log10(sum-(differenceTo24Hours*1.));
			
			// assignCostPerAgentToLinksAndIntervals(costPerAgent, receiverPointId2timeInterval2durationOfStayAndNoiseImmission);
			
			// calculate costSharesPerTimeInterval
			for(Id receiverPointId : receiverPointId2timeInterval2durationOfStayAndNoiseImmission.keySet()) {
				for(double timeInterval : receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).keySet()) {
					double factor = receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).get(timeInterval).getFirst();
					double noiseImmission = receiverPointId2timeInterval2durationOfStayAndNoiseImmission.get(receiverPointId).get(timeInterval).getSecond();
					double effectiveNoiseImmission = noiseImmission;
					if(timeInterval<=6*3600) {
						effectiveNoiseImmission = effectiveNoiseImmission + 10.;
					} else if(timeInterval>6*3600 && timeInterval<=22*3600) {
					} else if(timeInterval>18*3600 && timeInterval<=22*3600) {
						effectiveNoiseImmission = effectiveNoiseImmission + 5.;
					} else if(timeInterval>22*3600 && timeInterval<=24*3600) {
						effectiveNoiseImmission = effectiveNoiseImmission + 10.;
					}
					double sharePerHour = Math.pow((Math.pow(10,0.05*effectiveNoiseImmission))/(Math.pow(10,0.05*LForCalculation)), 2);
					double costs = factor*sharePerHour*costPerAgent;
					
					totalTollAffectedAgentBasedCalculationControl = totalTollAffectedAgentBasedCalculationControl + costs;
					
					if(receiverPointId2timeInterval2damageCostAgentBased.containsKey(receiverPointId)) {
						Map<Double,Double> timeInterval2damageCostAgentBased = receiverPointId2timeInterval2damageCostAgentBased.get(receiverPointId);
						if(receiverPointId2timeInterval2damageCostAgentBased.get(receiverPointId).containsKey(timeInterval)) {
							double valueBefore = timeInterval2damageCostAgentBased.get(timeInterval);
							double newValue = valueBefore + costs;
							timeInterval2damageCostAgentBased.put(timeInterval, newValue);
						} else {
							timeInterval2damageCostAgentBased.put(timeInterval, costs);
						}
						receiverPointId2timeInterval2damageCostAgentBased.put(receiverPointId, timeInterval2damageCostAgentBased);
					} else {
						Map<Double,Double> timeInterval2damageCostAgentBased = new HashMap<Double, Double>();
						timeInterval2damageCostAgentBased.put(timeInterval, costs);
						receiverPointId2timeInterval2damageCostAgentBased.put(receiverPointId, timeInterval2damageCostAgentBased);
					}
					if(personId2receiverPointId2timeInterval2costs.containsKey(personId)){
						Map<Id,Map<Double,Double>> receiverPointId2timeInterval2costs = personId2receiverPointId2timeInterval2costs.get(personId);
						if(receiverPointId2timeInterval2costs.containsKey(receiverPointId)){
							Map<Double,Double> timeInterval2costs = receiverPointId2timeInterval2costs.get(receiverPointId);
							timeInterval2costs.put(timeInterval, costs);
							receiverPointId2timeInterval2costs.put(receiverPointId, timeInterval2costs);
						} else {
							Map<Double,Double> timeInterval2costs = new HashMap<Double, Double>();
							timeInterval2costs.put(timeInterval, costs);
							receiverPointId2timeInterval2costs.put(receiverPointId, timeInterval2costs);
						}
						personId2receiverPointId2timeInterval2costs.put(personId,receiverPointId2timeInterval2costs);
					} else {
						Map<Id,Map<Double,Double>> receiverPointId2timeInterval2costs =  new HashMap<Id, Map<Double,Double>>();
						Map<Double,Double> timeInterval2costs = new HashMap<Double, Double>();
						timeInterval2costs.put(timeInterval, costs);
						receiverPointId2timeInterval2costs.put(receiverPointId, timeInterval2costs);
						personId2receiverPointId2timeInterval2costs.put(personId,receiverPointId2timeInterval2costs);
					}
				}
			}
		}
		
		//TODO: The following is for comparing the immission values to real-world-scenarios
		int countersub40 = 0;
		int counter4045 = 0;
		int counter4550 = 0;
		int counter5055 = 0;
		int counter5560 = 0;
		int counter6065 = 0;
		int counter6570 = 0;
		int counterover70 = 0;
		int summe = 0;
		for(Id pId : personId2Lden.keySet()) {
			double lden =personId2Lden.get(pId);
			if(lden<=40.) {
				countersub40++;
			} else if(lden<=45.) {
				counter4045++;
			} else if(lden<=50.) {
				counter4550++;
			} else if(lden<=55.) {
				counter5055++;
			} else if(lden<=60.) {
				counter5560++;
			} else if(lden<=65.) {
				counter6065++;
			} else if(lden<=70.) {
				counter6570++;
			} else {
				counterover70++;
			}
			summe++;
		}
		log.info("statistics...");
		log.info("unter 40: "+countersub40+" Anteil: "+((countersub40/1.0/summe/1.0)));
		log.info("40-45: "+counter4045+" Anteil: "+((counter4045/1.0/summe/1.0)));
		log.info("45-50: "+counter4550+" Anteil: "+((counter4550/1.0/summe/1.0)));
		log.info("50-55: "+counter5055+" Anteil: "+((counter5055/1.0/summe/1.0)));
		log.info("55-60: "+counter5560+" Anteil: "+((counter5560/1.0/summe/1.0)));
		log.info("60-65: "+counter6065+" Anteil: "+((counter6065/1.0/summe/1.0)));
		log.info("65-70: "+counter6570+" Anteil: "+((counter6570/1.0/summe/1.0)));
		log.info("ueber 70: "+counterover70+" Anteil: "+((counterover70/1.0/summe/1.0)));
		
		log.info(nGesamt);
		log.info((nGesamt/(scenario.getPopulation().getPersons().size())));
	}

	public void calculateDurationOfStay() {
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(Integer actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					Tuple<Double,Double> actStartAndActEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber);
					String actType = personId2actNumber2actType.get(personId).get(actNumber);
					double actStart = Double.MAX_VALUE;
					double actEnd = Double.MIN_VALUE;
					if(actStartAndActEnd.getFirst() == 0.) {
						// home activity (morning)
						actStart = 0.;
						actEnd = actStartAndActEnd.getSecond();
					} else if(actStartAndActEnd.getSecond() == 30*3600) {
						// home activity (evening)
						actStart = actStartAndActEnd.getFirst();
						actEnd = 30*3600;
					} else {
						// other activity
						actStart = actStartAndActEnd.getFirst();
						actEnd = actStartAndActEnd.getSecond();
					}
					// now calculation for the time shares of the intervals
					for(double intervalEnd = NoiseConfig.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + NoiseConfig.getIntervalLength()) {
						double intervalStart = intervalEnd - NoiseConfig.getIntervalLength();
//						
						double durationOfStay = 0.;
//						
						if(actEnd <= intervalStart || actStart >= intervalEnd) {
							durationOfStay = 0.;
						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = NoiseConfig.getIntervalLength();
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - intervalStart;
						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = intervalEnd - actStart;
						} else if(actStart >= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - actStart;
						}
						
						// calculation for the individual noiseEventsAffected
						// list for all receiver points and all time intervals for each agent the time, ...
						Map <Double , Map <Id,Map<Integer,Tuple<Double,String>>>> timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Double , Map <Id,Map<Integer,Tuple<Double,String>>>>();
						if(receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(receiverPointId)) {
							timeInterval2personId2actNumber2affectedAgentUnitsAndActType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId);
						} else {
						}
						Map <Id,Map<Integer,Tuple<Double,String>>> personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Id,Map<Integer,Tuple<Double,String>>>();
						if(timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(intervalEnd)) {
							personId2actNumber2affectedAgentUnitsAndActType = timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(intervalEnd);
						} else {
						}
						Map<Integer,Tuple<Double,String>> actNumber2affectedAgentUnitsAndActType = new HashMap <Integer,Tuple<Double,String>>();
						if(personId2actNumber2affectedAgentUnitsAndActType.containsKey(personId)) {
							actNumber2affectedAgentUnitsAndActType = personId2actNumber2affectedAgentUnitsAndActType.get(personId);
						} else {
						}
						Tuple <Double,String> affectedAgentUnitsAndActType = new Tuple<Double, String>((durationOfStay/3600.), actType);
						actNumber2affectedAgentUnitsAndActType.put(actNumber,affectedAgentUnitsAndActType);
						personId2actNumber2affectedAgentUnitsAndActType.put(personId,actNumber2affectedAgentUnitsAndActType);
						timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(intervalEnd,personId2actNumber2affectedAgentUnitsAndActType);
						receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(receiverPointId,timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected (home-based-oriented)
						
						
						// calculation for the damage
						// the adaption of the intervalLength is considered here implicitly,
						// a further correction is not necessary
						double affectedAgentUnits = (NoiseConfig.getScaleFactor())* (durationOfStay/3600.);
						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							} else {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							}
						} else {
							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
						}	
					}
				}
			}
		}
	}
	
	public void calculateDurationOfStayOnlyHomeActivity() {
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(Integer actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					Tuple<Double,Double> actStartAndActEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber);
					String actType = personId2actNumber2actType.get(personId).get(actNumber);
					double actStart = Double.MAX_VALUE;
					double actEnd = Double.MIN_VALUE;
					if(actStartAndActEnd.getFirst() == 0.) {
						// home activity (morning)
						actStart = 0.;
						actEnd = actStartAndActEnd.getSecond();
					} else if(actStartAndActEnd.getSecond() == 30*3600) {
						// TODO: !!! actStartAndActEnd.getSecond() == 30*3600 ?? Right Adaption before?!
						// home activity (evening)
						actStart = actStartAndActEnd.getFirst();
						actEnd = 30*3600;
					} else {
						// other activity
						actStart = actStartAndActEnd.getFirst();
						actEnd = actStartAndActEnd.getSecond();
					}
					
					if(!(actType.toString().equals("home"))) {
						// activity duration is zero if it is a home activity
						actEnd = actStart;
					}
					
					// now calculation for the time shares of the intervals
					for(double intervalEnd = NoiseConfig.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + NoiseConfig.getIntervalLength()) {
						double intervalStart = intervalEnd - NoiseConfig.getIntervalLength();
					
						double durationOfStay = 0.;
					
						if(actEnd <= intervalStart || actStart >= intervalEnd) {
							durationOfStay = 0.;
						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = NoiseConfig.getIntervalLength();
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - intervalStart;
						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = intervalEnd - actStart;
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - actStart;
						}
						
						// calculation for the individual noiseEventsAffected
						// list for all receiver points and all time intervals for each agent the time, ...
						Map <Double , Map <Id,Map<Integer,Tuple<Double,String>>>> timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Double , Map <Id,Map<Integer,Tuple<Double,String>>>>();
						if(receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(receiverPointId)) {
							timeInterval2personId2actNumber2affectedAgentUnitsAndActType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId);
						} else {
						}
						Map <Id,Map<Integer,Tuple<Double,String>>> personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Id,Map<Integer,Tuple<Double,String>>>();
						if(timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(intervalEnd)) {
							personId2actNumber2affectedAgentUnitsAndActType = timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(intervalEnd);
						} else {
						}
						Map<Integer,Tuple<Double,String>> actNumber2affectedAgentUnitsAndActType = new HashMap <Integer,Tuple<Double,String>>();
						if(personId2actNumber2affectedAgentUnitsAndActType.containsKey(personId)) {
							actNumber2affectedAgentUnitsAndActType = personId2actNumber2affectedAgentUnitsAndActType.get(personId);
						} else {
						}
						Tuple <Double,String> affectedAgentUnitsAndActType = new Tuple<Double, String>((durationOfStay/3600.), actType);
						actNumber2affectedAgentUnitsAndActType.put(actNumber,affectedAgentUnitsAndActType);
						personId2actNumber2affectedAgentUnitsAndActType.put(personId,actNumber2affectedAgentUnitsAndActType);
						timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(intervalEnd,personId2actNumber2affectedAgentUnitsAndActType);
						receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(receiverPointId,timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected (home-based-oriented)
						
						// calculation for the damage
						double affectedAgentUnits = (NoiseConfig.getScaleFactor())* (durationOfStay/3600.);
						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							} else {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							}
						} else {
							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
						}	
					}
				}
			}
		}
	}
	
	public void calculateDamagePerReceiverPoint() {
		for(Id receiverPointId : receiverPointId2timeInterval2noiseImmission.keySet()) {
			for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(receiverPointId).keySet()) {
				double noiseImmission = receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get(timeInterval);
				double affectedAgentUnits = 0.;
				if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
					if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(timeInterval)) {
						affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get(timeInterval);
					} 	
				}
				double damageCost = calculateDamageCosts(noiseImmission,affectedAgentUnits,timeInterval);
				double damageCostPerAffectedAgentUnit = calculateDamageCosts(noiseImmission,1.,timeInterval);
				if(receiverPointId2timeInterval2damageCost.containsKey(receiverPointId)) {
					Map<Double,Double> timeInterval2damageCost = receiverPointId2timeInterval2damageCost.get(receiverPointId);
					Map<Double,Double> timeInterval2damageCostPerAffectedAgentUnit = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId);
					timeInterval2damageCost.put(timeInterval, damageCost);
					timeInterval2damageCostPerAffectedAgentUnit.put(timeInterval, damageCostPerAffectedAgentUnit);
					receiverPointId2timeInterval2damageCost.put(receiverPointId, timeInterval2damageCost);
					receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.put(receiverPointId, timeInterval2damageCostPerAffectedAgentUnit);
				} else {
					Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
					Map<Double,Double> timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Double, Double>();
					timeInterval2damageCost.put(timeInterval, damageCost);
					timeInterval2damageCostPerAffectedAgentUnit.put(timeInterval, damageCostPerAffectedAgentUnit);
					receiverPointId2timeInterval2damageCost.put(receiverPointId, timeInterval2damageCost);
					receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.put(receiverPointId, timeInterval2damageCostPerAffectedAgentUnit);
				}
			}
		}
	}
	
	private double calculateCostPerYearCorrespondingToLdenValue (double Lden) {
		double damageCosts = 0.;
		
		// dayOrNight must be "DAY" because the noiseImmission values are already adapted above
		double lautheitsgewicht = calculateLautheitsgewicht(Lden, "DAY");
		// Calculation for each agent separately, therefore no calculation of affectedAgentUnits
		
		damageCosts = annualCostRate * lautheitsgewicht;
		
		return damageCosts;	
	}
	
	private double calculateDamageCosts(double noiseImmission, double affectedAgentUnits , double timeInterval) {
		String dayOrNight = "NIGHT";
		if(timeInterval>6*3600 && timeInterval<=22*3600) {
			dayOrNight = "DAY";
		} else if(timeInterval>18*3600 && timeInterval<=22*3600) {
			dayOrNight = "EVENING";
		}
		
		double lautheitsgewicht = calculateLautheitsgewicht(noiseImmission, dayOrNight);  
		
		double laermEinwohnerGleichwert = lautheitsgewicht*affectedAgentUnits;
		
		double damageCosts = 0.;
		if(dayOrNight == "DAY"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		}else if(dayOrNight == "EVENING"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		}else if(dayOrNight == "NIGHT"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		}else{
			throw new RuntimeException("Neither day nor night!");
		}
		return damageCosts;	
	}

	private double calculateLautheitsgewicht (double noiseImmission , String dayOrNight){
		double lautheitsgewicht = 0;
		
		if(dayOrNight == "DAY"){
			if(noiseImmission<50){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 50));
			}
		}else if(dayOrNight == "EVENING"){
			if(noiseImmission<45){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 45));
			}
		}else if(dayOrNight == "NIGHT"){
			if(noiseImmission<40){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 40));
			}
		}else{
			throw new RuntimeException("Neither day nor night!");
		}
		
		return lautheitsgewicht;
		
	}

	public void calculateCostSharesPerLinkPerTimeInterval() {
		// preparation
		for(Id coordId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2isolatedImmission = receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId);
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2costShare = new HashMap<Double, Map<Id,Double>>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				Map<Id,Double> noiseLinks2isolatedImmission = timeIntervals2noiseLinks2isolatedImmission.get(timeInterval);
				Map<Id,Double> noiseLinks2costShare = new HashMap<Id, Double>();
				double resultingNoiseImmission = receiverPointId2timeInterval2noiseImmission.get(coordId).get(timeInterval);
				if(!((receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval))==0.)) {
					for(Id linkId : noiseLinks2isolatedImmission.keySet()) {
						double noiseImmission = noiseLinks2isolatedImmission.get(linkId);
						double costs = 0.;
						if(!(noiseImmission==0.)) {
							double costShare = noiseImmissionCalculator.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
							costs = costShare * receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval);
							
						}
						noiseLinks2costShare.put(linkId, costs);
					}
				}
				timeIntervals2noiseLinks2costShare.put(timeInterval, noiseLinks2costShare);
			}
			receiverPointIds2timeIntervals2noiseLinks2costShare.put(coordId, timeIntervals2noiseLinks2costShare);
		}
		
		//summing up the link-based-costs
		//initializing the map:
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				timeInterval2damageCost.put(timeInterval, 0.);
			}
			linkId2timeInterval2damageCost.put(linkId,timeInterval2damageCost);
		}

		for(Id coordId : spatialInfo.getReceiverPoints().keySet()) {
			for(Id linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					if(!((receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval))==0.)) {
						double costs = receiverPointIds2timeIntervals2noiseLinks2costShare.get(coordId).get(timeInterval).get(linkId);
						double sumBefore = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
						double sumNew = sumBefore + costs;
						linkId2timeInterval2damageCost.get(linkId).put(timeInterval, sumNew);
					}
				}
			}
		}
	}
	
	public void calculateCostSharesPerLinkPerTimeIntervalAgentBased() {
		// preparation
		for(Id coordId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2isolatedImmission = receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId);
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2costShare = new HashMap<Double, Map<Id,Double>>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				Map<Id,Double> noiseLinks2isolatedImmission = timeIntervals2noiseLinks2isolatedImmission.get(timeInterval);
				Map<Id,Double> noiseLinks2costShare = new HashMap<Id, Double>();
				double resultingNoiseImmission = receiverPointId2timeInterval2noiseImmission.get(coordId).get(timeInterval);
				if(receiverPointId2timeInterval2damageCostAgentBased.containsKey(coordId)) {
					if(receiverPointId2timeInterval2damageCostAgentBased.get(coordId).containsKey(timeInterval)) {
						if(!((receiverPointId2timeInterval2damageCostAgentBased.get(coordId).get(timeInterval))==0.)) {
							for(Id linkId : noiseLinks2isolatedImmission.keySet()) {
								double noiseImmission = noiseLinks2isolatedImmission.get(linkId);
								double costs = 0.;
								if(!(noiseImmission==0.)) {
									double costShare = noiseImmissionCalculator.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
									costs = costShare * receiverPointId2timeInterval2damageCostAgentBased.get(coordId).get(timeInterval);
									
								}
								noiseLinks2costShare.put(linkId, costs);
							}
						}
					}
				}
				
				timeIntervals2noiseLinks2costShare.put(timeInterval, noiseLinks2costShare);
			}
			receiverPointIds2timeIntervals2noiseLinks2costShare.put(coordId, timeIntervals2noiseLinks2costShare);
		}
		
		//summing up the link-based-costs
		//initializing the map:
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				timeInterval2damageCost.put(timeInterval, 0.);
			}
			linkId2timeInterval2damageCost.put(linkId,timeInterval2damageCost);
		}

		for(Id coordId : spatialInfo.getReceiverPoints().keySet()) {
			for(Id linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					if(receiverPointId2timeInterval2damageCostAgentBased.containsKey(coordId)) {
						if(receiverPointId2timeInterval2damageCostAgentBased.get(coordId).containsKey(timeInterval)) {
							if(!((receiverPointId2timeInterval2damageCostAgentBased.get(coordId).get(timeInterval))==0.)) {
								double costs = receiverPointIds2timeIntervals2noiseLinks2costShare.get(coordId).get(timeInterval).get(linkId);
								double sumBefore = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
								double sumNew = sumBefore + costs;
								linkId2timeInterval2damageCost.get(linkId).put(timeInterval, sumNew);
							}
						}
					}					
				}
			}
		}
	}

	public void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
//		for(Id linkId : linkId2timeInterval2damageCost.keySet()) {
			Map<Double,Double> timeInterval2damageCostPerCar = new HashMap<Double, Double>();
			Map<Double,Double> timeInterval2damageCostPerHdvVehicle = new HashMap<Double, Double>();
//			for(double timeInterval : linkId2timeInterval2damageCost.get(linkId).keySet()) {
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				double damageCostSum = 0.;
				if(linkId2timeInterval2damageCost.containsKey(linkId)) {
					if(linkId2timeInterval2damageCost.get(linkId).containsKey(timeInterval)) {
						damageCostSum = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
					}
				}
				
				int nCar = linkId2timeInterval2linkLeaveEventsCar.get(linkId).get(timeInterval).size();
				int nHdv = linkId2timeInterval2linkLeaveEventsHdv.get(linkId).get(timeInterval).size();
			
				double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed())*3.6;
				double vHdv = vCar;
				// TODO: If different speeds for different vehicle types have to be considered, adapt the calculation here.
				// For example, a maximum speed for hdv-vehicles could be set here (for instance for German highways) 
				
				double lCar = 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
				double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
				
				double shareCar = 0.;
				double shareHdv = 0.;
				
				if((nCar>0)||(nHdv>0)) {
					shareCar = ((nCar * Math.pow(10, 0.1*lCar)) / ((nCar * Math.pow(10, 0.1*lCar))+(nHdv * Math.pow(10, 0.1*lHdv))));
					shareHdv = ((nHdv * Math.pow(10, 0.1*lHdv)) / ((nCar * Math.pow(10, 0.1*lCar))+(nHdv * Math.pow(10, 0.1*lHdv))));
					if((!(((shareCar+shareHdv)>0.999)&&((shareCar+shareHdv)<1.001)))) {
						
						log.warn("The sum of shareCar and shareHdv is not equal to 1.0! The value is "+(shareCar+shareHdv));
					}
				}
				double damageCostSumCar = shareCar * damageCostSum;
				double damageCostSumHdv = shareHdv * damageCostSum;
				
				double damageCostPerCar = 0.;
				if(!(nCar == 0)) {
					damageCostPerCar = damageCostSumCar/nCar;
				}
				timeInterval2damageCostPerCar.put(timeInterval,damageCostPerCar);
				
				double damageCostPerHdvVehicle = 0.;
				if(!(nHdv == 0)) {
					damageCostPerHdvVehicle = damageCostSumHdv/nHdv;
				}
				timeInterval2damageCostPerHdvVehicle.put(timeInterval,damageCostPerHdvVehicle);
			}
			linkId2timeInterval2damageCostPerCar.put(linkId, timeInterval2damageCostPerCar);
			linkId2timeInterval2damageCostPerHdvVehicle.put(linkId, timeInterval2damageCostPerHdvVehicle);
		}
	}
	
	@Override
	public void handleEvent(NoiseEvent event) {
//		log.info("+++++++++++++++++++++");
//		double amount = event.getAmount() *(-1);
//		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getVehicleId(), amount);
// MoneyEvents processed separately in the NoiseCostPricingHandler
//		events.processEvent(moneyEvent);
//		moneyEvents.add(moneyEvent);
		noiseEvents.add(event);
	}
	
	@Override
	public void handleEvent(NoiseEventAffected event) {
		noiseEventsAffected.add(event);
	}

	public void throwNoiseEvents() {
//		log.info(linkId2timeInterval2damageCostPerCar);
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				double amountCar = (linkId2timeInterval2damageCostPerCar.get(linkId).get(timeInterval))/(NoiseConfig.getScaleFactor());
				double amountHdv = (linkId2timeInterval2damageCostPerHdvVehicle.get(linkId).get(timeInterval))/(NoiseConfig.getScaleFactor());
//				log.info(amountCar+" - amountCar");
//				log.info(amountHdv+" - amountHdv");
				
				List<LinkEnterEvent> listLinkLeaveEventsTmp = linkId2timeInterval2linkLeaveEvents.get(linkId).get(timeInterval);
				List<Id>  listIdsTmp = new ArrayList<Id>();
				
				// calculate shares for the affected Agents

				for(LinkEnterEvent event : listLinkLeaveEventsTmp) {
					listIdsTmp.add(event.getVehicleId());
				}
				for(Id vehicleId : listIdsTmp) {
					// TODO: by now, only cars (no Hdv)
					double amount = 0.;
					boolean isHdv = false;
					
					if(!(hdvVehicles.contains(vehicleId))) {
						amount = amountCar;
//						log.info(linkId2timeInterval2damageCostPerCar);
//						log.info("amount: "+amount);
					} else {
						amount = amountHdv;
						isHdv = true;
					}
					double time = timeInterval-1; // TODO: the real leaving time should be adapted,
					// but for the routing the linkEnterTime or the ActivityEndTime is necessary!
					Id agentId = vehicleId;
					CarOrHdv carOrHdv = CarOrHdv.car;
					if(isHdv == true) {
						carOrHdv = CarOrHdv.hdv;
					}

//					NoiseEvent_Interface noiseEvent = new NoiseEventImpl(time,linkId,vehicleId,agentId,amount);
					NoiseEvent noiseEvent = new NoiseEvent(time,agentId,vehicleId,amount,linkId,carOrHdv);
					// for any causing-affected relation, the noiseEvents should be thrown,
					// then the later computation would be easier
					events.processEvent(noiseEvent);
					if(isHdv == true) {
						noiseEventsHdv.add(noiseEvent);
					} else {
						noiseEventsCar.add(noiseEvent);
					}
//					
//					log.info("time: "+time);
//					events.processEvent((Event) noiseEvent);
					totalToll = totalToll+amount;
//					log.info("amount: "+amount);
//					log.info("totalToll: "+totalToll);
					
					if(personId2tollSum.containsKey(agentId)) {
						double newTollSum = personId2tollSum.get(agentId) + amount;
						personId2tollSum.put(agentId,newTollSum);
					} else {
						personId2tollSum.put(agentId,amount);
					}
				}
			}
		}
	}
	
	public void throwNoiseEventsAffected() {
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(int actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
//					double actStart = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getFirst();
//					double actEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getSecond();
					
					for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
						double factor = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getFirst();
						
						if(!(factor==0.)) {
							double time = timeInterval;
							Id agentId = personId;
							String actType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getSecond();
							
							double costPerUnit = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).get(timeInterval);
							double amount = factor * costPerUnit;
							
							NoiseEventAffected noiseEventAffected = new NoiseEventAffected(time,agentId,amount,receiverPointId,actType);
							// for any causing-affected relation, the noiseEvents should be thrown,
							// then the later computation would be easier
							events.processEvent(noiseEventAffected);
							totalTollAffected = totalTollAffected+amount;
	//						log.info("amount: "+amount);
	//						log.info("totalTollAffected: "+totalTollAffected);
						
							if(personId2damageSum.containsKey(personId)) {
								double newTollSum = personId2damageSum.get(personId) + amount;
								personId2damageSum.put(personId,newTollSum);
							} else {
								personId2damageSum.put(personId,amount);
							}
						}
					}
				}
			}
		}
		
		//for comparison the home-based-oriented calculation
		for(Id receiverPointId : receiverPointId2ListOfHomeAgents.keySet()) {
			for(Id personId : receiverPointId2ListOfHomeAgents.get(receiverPointId)) {
				personId2homeReceiverPointId.put(personId,receiverPointId);
			}
		}
		for(Id personId : scenario.getPopulation().getPersons().keySet()) {
			Id receiverPointId = personId2homeReceiverPointId.get(personId);
			double homeBasedDamageSum = 0.;
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				double cost = 0.;
				if(receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).containsKey(timeInterval)) {
					cost = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).get(timeInterval);
				} else {
					cost = 0.;
				}
				homeBasedDamageSum = homeBasedDamageSum + cost;
			}
			personId2homeBasedDamageSum.put(personId, homeBasedDamageSum);
		}
		
	}

	public void setLinkId2timeBin2avgToll() {
		if (!this.linkId2timeBin2tollSum.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2tollSum should be empty!");
		} else {
			// calculate toll sum for each link and time bin
			setlinkId2timeBin2tollSum();
		}
		
		if (!this.linkId2timeBin2leavingAgents.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2leavingAgents should be empty!");
		} else {
			// calculate leaving agents for each link and time bin
			setlinkId2timeBin2leavingAgents();
		}
		
		if (!this.linkId2timeBin2avgToll.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2avgToll should be empty!");
		} else {
			// calculate average toll for each link and time bin
				
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()) {
				Map<Double, Double> timeBin2tollSum = this.linkId2timeBin2tollSum.get(linkId);
				Map<Double, Double> timeBin2avgToll = new HashMap<Double, Double>();

				for (Double timeBin : timeBin2tollSum.keySet()){
					double avgToll = 0.0;
					double tollSum = timeBin2tollSum.get(timeBin);
					if (tollSum == 0.) {
						// avg toll is zero for this time bin on that link
					} else {
						int leavingAgents = 0;
						if(linkId2timeBin2leavingAgents.get(linkId).containsKey(timeBin)) {
							leavingAgents = linkId2timeBin2leavingAgents.get(linkId).get(timeBin);
								if(!(this.linkId2timeBin2leavingAgents.get(linkId).get(timeBin) == null)) {
									avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgents;
									if(leavingAgents==0) {
										avgToll = 0.;
									}
								}
								avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgents;
							} else {
								avgToll = 0.;
							}
//						log.info("(noise) linkId: " + linkId + " // timeBin: " + Time.writeTime(timeBin, Time.TIMEFORMAT_HHMMSS) + " // toll sum: " + tollSum + " // leaving agents: " + leavingAgents + " // avg toll: " + avgToll);
					}
					timeBin2avgToll.put(timeBin, avgToll);
				}
				linkId2timeBin2avgToll.put(linkId , timeBin2avgToll);
			}
		}
	}
	
	public void setLinkId2timeBin2avgTollCar() {
		if (!this.linkId2timeBin2tollSumCar.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2tollSumCar should be empty!");
		} else {
			// calculate toll sum for each link and time bin
			setlinkId2timeBin2tollSumCar();
		}
		
		if (!this.linkId2timeBin2leavingAgentsCar.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2leavingAgentsCar should be empty!");
		} else {
			// calculate leaving agents for each link and time bin
			setlinkId2timeBin2leavingAgentsCar();
		}
		
		if (!this.linkId2timeBin2avgTollCar.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2avgTollCar should be empty!");
		} else {
			// calculate average toll for each link and time bin
			
			for (Id linkId : this.linkId2timeBin2tollSumCar.keySet()) {
				Map<Double, Double> timeBin2tollSumCar = this.linkId2timeBin2tollSumCar.get(linkId);
				Map<Double, Double> timeBin2avgTollCar = new HashMap<Double, Double>();

				for (Double timeBin : timeBin2tollSumCar.keySet()){
					double avgToll = 0.0;
					double tollSum = timeBin2tollSumCar.get(timeBin);
					if (tollSum == 0.) {
						// avg toll is zero for this time bin on that link
					} else {
						int leavingAgentsCar = 0;
						if(linkId2timeBin2leavingAgentsCar.get(linkId).containsKey(timeBin)) {
							leavingAgentsCar = linkId2timeBin2leavingAgentsCar.get(linkId).get(timeBin);
								if(!(this.linkId2timeBin2leavingAgentsCar.get(linkId).get(timeBin) == null)) {
									avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgentsCar;
									if(leavingAgentsCar==0) {
										avgToll = 0.;
									}
								}
								avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgentsCar;
							} else {
								avgToll = 0.;
							}
//						log.info("(noise) linkId: " + linkId + " // timeBin: " + Time.writeTime(timeBin, Time.TIMEFORMAT_HHMMSS) + " // toll sum: " + tollSum + " // leaving agents: " + leavingAgents + " // avg toll: " + avgToll);
					}
					timeBin2avgTollCar.put(timeBin, avgToll);
				}
				linkId2timeBin2avgTollCar.put(linkId , timeBin2avgTollCar);
			}
		}
	}
	
	public void setLinkId2timeBin2avgTollHdv() {
		if (!this.linkId2timeBin2tollSumHdv.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2tollSumHdv should be empty!");
		} else {
			// calculate toll sum for each link and time bin
			setlinkId2timeBin2tollSumHdv();
		}
		
		if (!this.linkId2timeBin2leavingAgentsHdv.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2leavingAgentsHdv should be empty!");
		} else {
			// calculate leaving agents for each link and time bin
			setlinkId2timeBin2leavingAgentsHdv();
		}
		
		if (!this.linkId2timeBin2avgTollHdv.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2avgTollHdv should be empty!");
		} else {
			// calculate average toll for each link and time bin
				
			for (Id linkId : this.linkId2timeBin2tollSumHdv.keySet()) {
				Map<Double, Double> timeBin2tollSumHdv = this.linkId2timeBin2tollSumHdv.get(linkId);
				Map<Double, Double> timeBin2avgTollHdv = new HashMap<Double, Double>();

				for (Double timeBin : timeBin2tollSumHdv.keySet()){
					double avgToll = 0.0;
					double tollSum = timeBin2tollSumHdv.get(timeBin);
					if (tollSum == 0.) {
						// avg toll is zero for this time bin on that link
					} else {
						int leavingAgentsHdv = 0;
						if(linkId2timeBin2leavingAgentsHdv.get(linkId).containsKey(timeBin)) {
							leavingAgentsHdv = linkId2timeBin2leavingAgentsHdv.get(linkId).get(timeBin);
								if(!(this.linkId2timeBin2leavingAgentsHdv.get(linkId).get(timeBin) == null)) {
									avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgentsHdv;
									if(leavingAgentsHdv==0) {
										avgToll = 0.;
									}
								}
								avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgentsHdv;
							} else {
								avgToll = 0.;
							}
//						log.info("(noise) linkId: " + linkId + " // timeBin: " + Time.writeTime(timeBin, Time.TIMEFORMAT_HHMMSS) + " // toll sum: " + tollSum + " // leaving agents: " + leavingAgents + " // avg toll: " + avgToll);
					}
					timeBin2avgTollHdv.put(timeBin, avgToll);
				}
				linkId2timeBin2avgTollHdv.put(linkId , timeBin2avgTollHdv);
			}
		}
	}
	
	private void setlinkId2timeBin2leavingAgents() {
		for (LinkEnterEvent event : this.linkLeaveEvents){
			
			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())){
				// Tolls paid on this link.
				
				Map<Double, Integer> timeBin2leavingAgents = new HashMap<Double, Integer>();

				if (this.linkId2timeBin2leavingAgents.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2leavingAgents = this.linkId2timeBin2leavingAgents.get(event.getLinkId());
					
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							// update leaving agents on this link and in this time bin
							
							if (timeBin2leavingAgents.get(time) != null) {
								// not the first agent leaving this link in this time bin
								int leavingAgentsSoFar = timeBin2leavingAgents.get(time);
								int leavingAgents = leavingAgentsSoFar + 1;
								timeBin2leavingAgents.put(time, leavingAgents);
							} else {
								// first leaving agent leaving this link in this time bin
								timeBin2leavingAgents.put(time, 1);
							}
						}
					}
					linkId2timeBin2leavingAgents.put(event.getLinkId(), timeBin2leavingAgents);

				} else {
					// link not yet in map

					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							timeBin2leavingAgents.put(time, 1);
						}
					}
					linkId2timeBin2leavingAgents.put(event.getLinkId(), timeBin2leavingAgents);
				}	
			
			} else {
				// No tolls paid on that link. Skip that link.
		
			}
		}
	}
	
	private void setlinkId2timeBin2leavingAgentsCar() {
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double, Integer> timeBin2leavingAgentsCar = new HashMap<Double, Integer>();
			for(double timeBin = NoiseConfig.getIntervalLength() ; timeBin<=30*3600 ; timeBin = timeBin + NoiseConfig.getIntervalLength()) {
				timeBin2leavingAgentsCar.put(timeBin,0);
			}
			linkId2timeBin2leavingAgentsCar.put(linkId, timeBin2leavingAgentsCar);
		}
		
		for (LinkEnterEvent event : this.linkLeaveEventsCar){
//			log.info("vehicle Id: "+event.getVehicleId());
//			
//			log.info("000");
//			log.info(linkId2timeBin2tollSumCar);
			if (this.linkId2timeBin2tollSumCar.containsKey(event.getLinkId())){
				// Tolls paid on this link.
				
				Map<Double, Integer> timeBin2leavingAgentsCar = new HashMap<Double, Integer>();
	
				if (this.linkId2timeBin2leavingAgentsCar.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2leavingAgentsCar = this.linkId2timeBin2leavingAgentsCar.get(event.getLinkId());
						
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;
	
						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							// update leaving agents on this link and in this time bin
								
							if (timeBin2leavingAgentsCar.get(time) != null) {
								// not the first agent leaving this link in this time bin
								int leavingAgentsSoFar = timeBin2leavingAgentsCar.get(time);
								int leavingAgents = leavingAgentsSoFar + 1;
								timeBin2leavingAgentsCar.put(time, leavingAgents);
							} else {
								// first leaving agent leaving this link in this time bin
								timeBin2leavingAgentsCar.put(time, 1);
							}
						}
					}
					linkId2timeBin2leavingAgentsCar.put(event.getLinkId(), timeBin2leavingAgentsCar);
	
				} else {
					// link not yet in map
	
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;
	
						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							timeBin2leavingAgentsCar.put(time, 1);
						}
					}
					linkId2timeBin2leavingAgentsCar.put(event.getLinkId(), timeBin2leavingAgentsCar);
				}	
			
			} else {
				// No tolls paid on that link. Skip that link.
		
			}
		}
	}
	
	private void setlinkId2timeBin2leavingAgentsHdv() {
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double, Integer> timeBin2leavingAgentsHdv = new HashMap<Double, Integer>();
			for(double timeBin = NoiseConfig.getIntervalLength() ; timeBin<=30*3600 ; timeBin = timeBin + NoiseConfig.getIntervalLength()) {
				timeBin2leavingAgentsHdv.put(timeBin,0);
			}
			linkId2timeBin2leavingAgentsHdv.put(linkId, timeBin2leavingAgentsHdv);
		}
		
		for (LinkEnterEvent event : this.linkLeaveEventsHdv){
		
//			log.info(linkId2timeBin2tollSumHdv);
			if (this.linkId2timeBin2tollSumHdv.containsKey(event.getLinkId())){
				// Tolls paid on this link.
					
				Map<Double, Integer> timeBin2leavingAgentsHdv = new HashMap<Double, Integer>();
	
				if (this.linkId2timeBin2leavingAgentsHdv.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2leavingAgentsHdv = this.linkId2timeBin2leavingAgentsHdv.get(event.getLinkId());
						
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;
	
						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							// update leaving agents on this link and in this time bin
								
							if (timeBin2leavingAgentsHdv.get(time) != null) {
								// not the first agent leaving this link in this time bin
								int leavingAgentsSoFar = timeBin2leavingAgentsHdv.get(time);
								int leavingAgents = leavingAgentsSoFar + 1;
								timeBin2leavingAgentsHdv.put(time, leavingAgents);
							} else {
								// first leaving agent leaving this link in this time bin
								timeBin2leavingAgentsHdv.put(time, 1);
							}
						}
					}
					linkId2timeBin2leavingAgentsHdv.put(event.getLinkId(), timeBin2leavingAgentsHdv);
	
				} else {
					// link not yet in map
	
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;
	
						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							timeBin2leavingAgentsHdv.put(time, 1);
						}
					}
					linkId2timeBin2leavingAgentsHdv.put(event.getLinkId(), timeBin2leavingAgentsHdv);
				}	
				
			} else {
				// No tolls paid on that link. Skip that link.
			
			}
		}
	}
	
	private void setlinkId2timeBin2tollSum() {
		for (NoiseEvent event : this.noiseEvents) {
			Map<Double, Double> timeBin2tollSum = new HashMap<Double, Double>();	
			double amount = 0.0;
			
			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())) {
				// link already in map
				timeBin2tollSum = this.linkId2timeBin2tollSum.get(event.getLinkId());

				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
					
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						// update toll sum of this link and time bin
						
						if (timeBin2tollSum.get(time) != null) {
							// toll sum was calculated before for this time bin
							double sum = timeBin2tollSum.get(time);
						
							amount = event.getAmount();
							
							double sumNew = sum + amount;
							timeBin2tollSum.put(time, sumNew);
						
						} else {
							// toll sum was not calculated before for this time bin
							amount = event.getAmount();
						
							timeBin2tollSum.put(time, amount);
							
						}
					}
				}
				linkId2timeBin2tollSum.put(event.getLinkId(),timeBin2tollSum);

			} else {
				// link not yet in map
				
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;

					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						amount = event.getAmount();

						timeBin2tollSum.put(time, amount);
					}
				}
				linkId2timeBin2tollSum.put(event.getLinkId(),timeBin2tollSum);
			}
		}
	}
	
	private void setlinkId2timeBin2tollSumCar() {
		for (NoiseEvent event : this.noiseEventsCar) {
	
			Map<Double, Double> timeBin2tollSumCar = new HashMap<Double, Double>();		
			double amount = 0.0;
				
			if (this.linkId2timeBin2tollSumCar.containsKey(event.getLinkId())) {
				// link already in map
				timeBin2tollSumCar = this.linkId2timeBin2tollSumCar.get(event.getLinkId());
	
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
						
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						// update toll sum of this link and time bin
							
						if (timeBin2tollSumCar.get(time) != null) {
							// toll sum was calculated before for this time bin
							double sum = timeBin2tollSumCar.get(time);
							
							amount = event.getAmount();
								
							double sumNew = sum + amount;
							timeBin2tollSumCar.put(time, sumNew);
							
						} else {
							// toll sum was not calculated before for this time bin
							amount = event.getAmount();
						
							timeBin2tollSumCar.put(time, amount);
								
						}
					}
				}
				linkId2timeBin2tollSumCar.put(event.getLinkId(),timeBin2tollSumCar);
	
			} else {
				// link not yet in map
					
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
	
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						amount = event.getAmount();
	
						timeBin2tollSumCar.put(time, amount);
					}
				}
				linkId2timeBin2tollSumCar.put(event.getLinkId(),timeBin2tollSumCar);
			}
		}
		
		// fill up the time intervals in which no toll was paid
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			if (this.linkId2timeBin2tollSumCar.containsKey(linkId)) {
				// On this link, may be toll was not paid in all time intervals
				Map<Double, Double> timeBin2tollSumCar = linkId2timeBin2tollSumCar.get(linkId);
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					if(!(timeBin2tollSumCar.containsKey(timeInterval))) {
						timeBin2tollSumCar.put(timeInterval,0.);
					}
				}
				linkId2timeBin2tollSumCar.put(linkId, timeBin2tollSumCar);
			} else {
				// On this link, no toll was paid in any time interval
				
				Map<Double, Double> timeBin2tollSumCar = new HashMap<Double, Double>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					timeBin2tollSumCar.put(timeInterval,0.);
				}
				linkId2timeBin2tollSumCar.put(linkId, timeBin2tollSumCar);
			}
			
		}
		
	}
	
	private void setlinkId2timeBin2tollSumHdv() {
		for (NoiseEvent event : this.noiseEventsHdv) {
			
			Map<Double, Double> timeBin2tollSumHdv = new HashMap<Double, Double>();		
	//		log.info(event.getAmount());
			double amount = 0.0;
				
			if (this.linkId2timeBin2tollSumHdv.containsKey(event.getLinkId())) {
				// link already in map
				timeBin2tollSumHdv = this.linkId2timeBin2tollSumHdv.get(event.getLinkId());
	
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
						
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						// update toll sum of this link and time bin
							
						if (timeBin2tollSumHdv.get(time) != null) {
							// toll sum was calculated before for this time bin
							double sum = timeBin2tollSumHdv.get(time);
							
							amount = event.getAmount();
								
							double sumNew = sum + amount;
							timeBin2tollSumHdv.put(time, sumNew);
							
						} else {
							// toll sum was not calculated before for this time bin
							amount = event.getAmount();
							
							timeBin2tollSumHdv.put(time, amount);
								
						}
					}
				}
				linkId2timeBin2tollSumHdv.put(event.getLinkId(),timeBin2tollSumHdv);
	
			} else {
				// link not yet in map
					
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
	
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						amount = event.getAmount();
	
						timeBin2tollSumHdv.put(time, amount);
					}
				}
				linkId2timeBin2tollSumHdv.put(event.getLinkId(),timeBin2tollSumHdv);
			}
		}
		// fill up the time intervals in which no toll was paid
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			if (this.linkId2timeBin2tollSumHdv.containsKey(linkId)) {
				// On this link, may be toll was not paid in all time intervals
				Map<Double, Double> timeBin2tollSumHdv = linkId2timeBin2tollSumHdv.get(linkId);
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					if(!(timeBin2tollSumHdv.containsKey(timeInterval))) {
						timeBin2tollSumHdv.put(timeInterval,0.);
					}
				}
				linkId2timeBin2tollSumHdv.put(linkId, timeBin2tollSumHdv);
			} else {
				// On this link, no toll was paid in any time interval
						
				Map<Double, Double> timeBin2tollSumHdv = new HashMap<Double, Double>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					timeBin2tollSumHdv.put(timeInterval,0.);
				}
				linkId2timeBin2tollSumHdv.put(linkId, timeBin2tollSumHdv);
			}
		}
	}
	
	public double getAvgToll(Id linkId, double time) {
		double avgToll = 0.;
		if (this.linkId2timeBin2avgToll.containsKey(linkId)){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgToll.get(linkId);
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgToll = timeBin2avgToll.get(timeBin);
				}
			}
		}
		return avgToll;
	}
	
	public double getAvgTollOldValue(Id linkId, double time) {
		double avgTollOldValue = 0.;
		if (this.linkId2timeBin2avgTollOldValue.containsKey(linkId)){
			Map<Double, Double> timeBin2avgTollOldValue = this.linkId2timeBin2avgTollOldValue.get(linkId);
			for (Double timeBin : timeBin2avgTollOldValue.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgTollOldValue = timeBin2avgTollOldValue.get(timeBin);
				}
			}
		}
		return avgTollOldValue;
	}

	public double getAvgTollCar(Id linkId, double time) {
		double avgTollCar = 0.;
		if (this.linkId2timeBin2avgTollCar.containsKey(linkId)){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgTollCar.get(linkId);
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgTollCar = timeBin2avgToll.get(timeBin);
				}
			}
		}
		return avgTollCar;
	}
	
	public double getAvgTollCarOldValue(Id linkId, double time) {
		double avgTollCarOldValue = 0.;
		if (this.linkId2timeBin2avgTollCarOldValue.containsKey(linkId)){
			Map<Double, Double> timeBin2avgTollOldValue = this.linkId2timeBin2avgTollCarOldValue.get(linkId);
			for (Double timeBin : timeBin2avgTollOldValue.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgTollCarOldValue = timeBin2avgTollOldValue.get(timeBin);
				}
			}
		}
		return avgTollCarOldValue;
	}
	
	public double getAvgTollHdv(Id linkId, double time) {
		double avgTollHdv = 0.;
		if (this.linkId2timeBin2avgTollHdv.containsKey(linkId)){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgTollHdv.get(linkId);
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgTollHdv = timeBin2avgToll.get(timeBin);
				}
			}
		}
		return avgTollHdv;
	}
	
	public double getAvgTollHdvOldValue(Id linkId, double time) {
		double avgTollHdvOldValue = 0.;
		if (this.linkId2timeBin2avgTollHdvOldValue.containsKey(linkId)){
			Map<Double, Double> timeBin2avgTollOldValue = this.linkId2timeBin2avgTollHdvOldValue.get(linkId);
			for (Double timeBin : timeBin2avgTollOldValue.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgTollHdvOldValue = timeBin2avgTollOldValue.get(timeBin);
				}
			}
		}
		return avgTollHdvOldValue;
	}

	public Map<Id,Double> getPersonId2tollSum(String fileName) {
		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;tollSum");
			bw.newLine();
			
			for (Id personId : this.personId2tollSum.keySet()){
				double toll = personId2tollSum.get(personId);
				
				bw.write(personId + ";" + toll);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2tollSum , "toll");
		return personId2tollSum;
	}
	
	public Map<Id,Double> getPersonId2damageSum(String fileName) {

		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;damageSum");
			bw.newLine();
			
			for (Id personId : this.personId2damageSum.keySet()){
				double damage = personId2damageSum.get(personId);
				
				bw.write(personId + ";" + damage);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2damageSum , "damage");
		return personId2damageSum;
	}
	
	public Map<Id,Double> getPersonId2homeBasedDamageSum(String fileName) {
		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;homeBasedDamageSum");
			bw.newLine();
			
			for (Id personId : this.personId2homeBasedDamageSum.keySet()){
				double difference = personId2homeBasedDamageSum.get(personId);
				
				bw.write(personId + ";" + difference);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2homeBasedDamageSum , "homeBasedDamage");
		return personId2homeBasedDamageSum;
	}
	
	public Map<Id,Double> getPersonId2differenceTollDamage(String fileName) {
		for(Id personId : personId2tollSum.keySet()) {
			double toll = personId2tollSum.get(personId);
			double damage = personId2damageSum.get(personId);
			double difference = toll - damage;
			personId2differenceTollDamage.put(personId, difference);
		}
		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;differenceTollDamage");
			bw.newLine();
			
			for (Id personId : this.personId2differenceTollDamage.keySet()){
				double difference = personId2differenceTollDamage.get(personId);
				
				bw.write(personId + ";" + difference);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2differenceTollDamage , "differenceTollDamage");
		return personId2differenceTollDamage;
	}
	
	public void writeNoiseEmissionStats(String fileName) {
		File file = new File(fileName);
		
		Map<Id,Double> linkId2noiseEmissionDay = new HashMap<Id, Double>();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;avg noiseEmission;avg noiseEmission (day);avg noiseEmission (night);avg noiseEmission (peak);avg noiseEmission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6*3600 + NoiseConfig.getIntervalLength() ; timeInterval<=22*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7*3600 + NoiseConfig.getIntervalLength() ; timeInterval<=9*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15*3600 + NoiseConfig.getIntervalLength() ; timeInterval<=18*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id linkId : this.linkId2timeInterval2noiseEmission.keySet()){
				double avgNoise = 0.;
				double avgNoiseDay = 0.;
				double avgNoiseNight = 0.;
				double avgNoisePeak = 0.;
				double avgNoiseOffPeak = 0.;
				
				double sumAvgNoise = 0.;
				int counterAvgNoise = 0;
				double sumAvgNoiseDay = 0.;
				int counterAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0.;
				int counterAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0.;
				int counterAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0.;
				int counterAvgNoiseOffPeak = 0;
				
				for(double timeInterval : linkId2timeInterval2noiseEmission.get(linkId).keySet()) {
					double noiseValue = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);
					double termToAdd = Math.pow(10., noiseValue/10.);
					
					if(timeInterval<24*3600) {
						sumAvgNoise = sumAvgNoise + termToAdd;
						counterAvgNoise++;
					}
					
					if(day.contains(timeInterval)) {
						sumAvgNoiseDay = sumAvgNoiseDay + termToAdd;
						counterAvgNoiseDay++;
					}
					
					if(night.contains(timeInterval)) {
						sumAvgNoiseNight = sumAvgNoiseNight + termToAdd;
						counterAvgNoiseNight++;
					}
				
					if(peak.contains(timeInterval)) {
						sumAvgNoisePeak = sumAvgNoisePeak + termToAdd;
						counterAvgNoisePeak++;
					}
					
					if(offPeak.contains(timeInterval)) {
						sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + termToAdd;
						counterAvgNoiseOffPeak++;
					}	
				}
				
				avgNoise = 10 * Math.log10(sumAvgNoise / (counterAvgNoise));
				avgNoiseDay = 10 * Math.log10(sumAvgNoiseDay / counterAvgNoiseDay);
				avgNoiseNight = 10 * Math.log10(sumAvgNoiseNight / counterAvgNoiseNight);
				avgNoisePeak = 10 * Math.log10(sumAvgNoisePeak / counterAvgNoisePeak);
				avgNoiseOffPeak = 10 * Math.log10(sumAvgNoiseOffPeak / counterAvgNoiseOffPeak);
				
				linkId2noiseEmissionDay.put(linkId, avgNoiseDay);
				
				bw.write(linkId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		analysis.shapes.IKNetworkPopulationWriter.exportNetwork2Shp(scenario.getNetwork(), linkId2noiseEmissionDay);
		
	}
	
	public void writeNoiseEmissionStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;");
		
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour"+i+";leaving agents (per hour);noiseEmission;");
			}
			bw.newLine();
			
			for (Id linkId : this.linkId2timeInterval2noiseEmission.keySet()){
				bw.write(linkId.toString()+";"); 
				for(int i = 0 ; i < 26 ; i++) {
//					log.info(linkId2timeInterval2linkLeaveEvents.get(linkId).get((i+1)*3600.).size());
//					log.info(linkId2timeInterval2noiseEmission.get(linkId).get((i+1)*3600.));
					bw.write(";hour_"+i+";"+linkId2timeInterval2linkLeaveEvents.get(linkId).get((i+1)*3600.).size()+";"+linkId2timeInterval2noiseEmission.get(linkId).get((i+1)*3600.)+";");	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	public void writeNoiseImmissionStats(String fileName) {
		File file = new File(fileName);
		
		Map<Id,Double> receiverPointId2noiseImmission = new HashMap<Id, Double>();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("receiver point;avg noiseImmission;avg noiseImmission (day);avg noiseImmission (night);avg noiseImmission (peak);avg noiseImmission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6*3600 + NoiseConfig.getIntervalLength() ; timeInterval<=22*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7*3600 + NoiseConfig.getIntervalLength() ; timeInterval<=9*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15*3600 + NoiseConfig.getIntervalLength() ; timeInterval<=18*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id receiverPointId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				double avgNoise = 0;
				double avgNoiseDay = 0;
				double avgNoiseNight = 0;
				double avgNoisePeak = 0;
				double avgNoiseOffPeak = 0;
				
				double sumAvgNoise = 0.;
				int counterAvgNoise = 0;
				double sumAvgNoiseDay = 0.;
				int counterAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0.;
				int counterAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0.;
				int counterAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0.;
				int counterAvgNoiseOffPeak = 0;
				
				for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(receiverPointId).keySet()) {
					double noiseValue = receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get(timeInterval);
					double termToAdd = Math.pow(10., noiseValue/10.);
					
					if(timeInterval<24*3600) {
						sumAvgNoise = sumAvgNoise + termToAdd;
						counterAvgNoise++;
					}
					
					if(day.contains(timeInterval)) {
						sumAvgNoiseDay = sumAvgNoiseDay + termToAdd;
						counterAvgNoiseDay++;
					}
					
					if(night.contains(timeInterval)) {
						sumAvgNoiseNight = sumAvgNoiseNight + termToAdd;
						counterAvgNoiseNight++;
					}
				
					if(peak.contains(timeInterval)) {
						sumAvgNoisePeak = sumAvgNoisePeak + termToAdd;
						counterAvgNoisePeak++;
					}
					
					if(offPeak.contains(timeInterval)) {
						sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + termToAdd;
						counterAvgNoiseOffPeak++;
					}	
				}
				
				avgNoise = 10 * Math.log10(sumAvgNoise / (counterAvgNoise));
				avgNoiseDay = 10 * Math.log10(sumAvgNoiseDay / counterAvgNoiseDay);
				avgNoiseNight = 10 * Math.log10(sumAvgNoiseNight / counterAvgNoiseNight);
				avgNoisePeak = 10 * Math.log10(sumAvgNoisePeak / counterAvgNoisePeak);
				avgNoiseOffPeak = 10 * Math.log10(sumAvgNoiseOffPeak / counterAvgNoiseOffPeak);
				receiverPointId2noiseImmission.put(receiverPointId,avgNoiseDay);
				
				bw.write(receiverPointId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		analysis.shapes.IKNetworkPopulationWriter.exportReceiverPoints2Shp(receiverPointId2noiseImmission);
		
	}
	
	public void writeNoiseImmissionStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("receiverPoint;");
			
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour_"+i+";affectedAgentUnits;noiseImmission;");
			}
			bw.newLine();
			
			for (Id receiverPointId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				
				bw.write(receiverPointId.toString()+";"); 
				for(int i = 0 ; i < 26 ; i++) {
					log.info(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get((i+1)*3600.));
					log.info(receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get((i+1)*3600.));
					bw.write(";hour_"+i+";"+receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get((i+1)*3600.)+";"+receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get((i+1)*3600.)+";");	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStats(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll (per day);leaving agents (per day);toll per agent;toll per agent per km;toll per agent per km (classified);;total toll Car (per day);leaving agents Car (per day);toll Car per agent Car;toll Car per agent Car per km;toll Car per agent Car per km (classified);;total toll Hdv (per day);leaving agents Hdv (per day);toll Hdv per agent Hdv;toll Hdv per agent Hdv per km;toll Hdv per agent Hdv per km (classified)");
			bw.newLine();
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()){
				double totalToll = 0.;
				int leavingAgents = 0;
				double tollPerAgent = 0.;
				double tollPerAgentPerKm = 0.;
				double totalTollCar = 0.;
				int leavingAgentsCar = 0;
				double tollPerAgentCar = 0.;
				double tollPerAgentPerKmCar = 0.;
				double totalTollHdv = 0.;
				int leavingAgentsHdv = 0;
				double tollPerAgentHdv = 0.;
				double tollPerAgentPerKmHdv = 0.;
				
				for (Double tollSum_timeBin : this.linkId2timeBin2tollSum.get(linkId).values()){
					totalToll = totalToll + tollSum_timeBin;
				}
				
				for (Integer leavingAgents_timeBin : this.linkId2timeBin2leavingAgents.get(linkId).values()){
					leavingAgents = leavingAgents + leavingAgents_timeBin;
				}
				
				for (Double tollSum_timeBinCar : this.linkId2timeBin2tollSumCar.get(linkId).values()){
					totalTollCar = totalTollCar + tollSum_timeBinCar;
				}
				
				for (Integer leavingAgents_timeBinCar : this.linkId2timeBin2leavingAgentsCar.get(linkId).values()){
					leavingAgentsCar = leavingAgentsCar + leavingAgents_timeBinCar;
				}
				
				for (Double tollSum_timeBinHdv : this.linkId2timeBin2tollSumHdv.get(linkId).values()){
					totalTollHdv = totalTollHdv + tollSum_timeBinHdv;
				}
				
				for (Integer leavingAgents_timeBinHdv : this.linkId2timeBin2leavingAgentsHdv.get(linkId).values()){
					leavingAgentsHdv = leavingAgentsHdv + leavingAgents_timeBinHdv;
				}
				
				tollPerAgent = totalToll/leavingAgents;
				tollPerAgentPerKm = 1000.*(tollPerAgent/scenario.getNetwork().getLinks().get(linkId).getLength());
				tollPerAgentCar = totalTollCar/leavingAgentsCar;
				tollPerAgentPerKmCar = 1000.*(tollPerAgentCar/scenario.getNetwork().getLinks().get(linkId).getLength());
				tollPerAgentHdv = totalTollHdv/leavingAgentsHdv;
				tollPerAgentPerKmHdv = 1000.*(tollPerAgentHdv/scenario.getNetwork().getLinks().get(linkId).getLength());
				int tollPerAgentPerKmClassified = classifyTollPerAgentPerKm(tollPerAgentPerKm);
				int tollPerAgentPerKmClassifiedCar = classifyTollPerAgentPerKmCar(tollPerAgentPerKm);
				int tollPerAgentPerKmClassifiedHdv = classifyTollPerAgentPerKmHdv(tollPerAgentPerKm);
				
				bw.write(linkId + ";" + totalToll + ";" + leavingAgents + ";" + tollPerAgent + ";" + tollPerAgentPerKm +";" + tollPerAgentPerKmClassified +";;"+ totalTollCar + ";" + leavingAgentsCar + ";" + tollPerAgentCar + ";" + tollPerAgentPerKmCar +";" + tollPerAgentPerKmClassifiedCar +";;"+ totalTollHdv + ";" + leavingAgentsHdv + ";" + tollPerAgentHdv + ";" + tollPerAgentPerKmHdv +";" + tollPerAgentPerKmClassifiedHdv);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsCar(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll Car (per day);leaving agents Car (per day);toll Car per agent Car;toll Car per agent Car per km;toll Car per agent Car per km (classified)");
			bw.newLine();
			
			for (Id linkId : this.linkId2timeBin2tollSumCar.keySet()){
				double totalToll = 0.;
				int leavingAgents = 0;
				double tollPerAgent = 0.;
				double tollPerAgentPerKm = 0.;
				
				for (Double tollSum_timeBin : this.linkId2timeBin2tollSumCar.get(linkId).values()){
					totalToll = totalToll + tollSum_timeBin;
				}
				
				for (Integer leavingAgents_timeBin : this.linkId2timeBin2leavingAgentsCar.get(linkId).values()){
					leavingAgents = leavingAgents + leavingAgents_timeBin;
				}
				
				tollPerAgent = totalToll/leavingAgents;
				tollPerAgentPerKm = 1000.*(tollPerAgent/scenario.getNetwork().getLinks().get(linkId).getLength());
				int tollPerAgentPerKmClassified = classifyTollPerAgentPerKm(tollPerAgentPerKm);
				
				bw.write(linkId + ";" + totalToll + ";" + leavingAgents + ";" + tollPerAgent + ";" + tollPerAgentPerKm +";" + tollPerAgentPerKmClassified);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsHdv(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll Hdv (per day);leaving agents Hdv (per day);toll Hdv per agent Hdv;toll Hdv per agent Hdv per km;toll Hdv per agent Hdv per km (classified)");
			bw.newLine();
			
			for (Id linkId : this.linkId2timeBin2tollSumHdv.keySet()){
				double totalToll = 0.;
				int leavingAgents = 0;
				double tollPerAgent = 0.;
				double tollPerAgentPerKm = 0.;
				
				for (Double tollSum_timeBin : this.linkId2timeBin2tollSumHdv.get(linkId).values()){
					totalToll = totalToll + tollSum_timeBin;
				}
				
				for (Integer leavingAgents_timeBin : this.linkId2timeBin2leavingAgentsHdv.get(linkId).values()){
					leavingAgents = leavingAgents + leavingAgents_timeBin;
				}
				
				tollPerAgent = totalToll/leavingAgents;
				tollPerAgentPerKm = 1000.*(tollPerAgent/scenario.getNetwork().getLinks().get(linkId).getLength());
				int tollPerAgentPerKmClassified = classifyTollPerAgentPerKm(tollPerAgentPerKm);
				
				bw.write(linkId + ";" + totalToll + ";" + leavingAgents + ";" + tollPerAgent + ";" + tollPerAgentPerKm +";" + tollPerAgentPerKmClassified);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsOnlyHomeActivities(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll (per day);leaving agents (per day);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.newLine();
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()){
				double totalToll = 0.;
				int leavingAgents = 0;
				double tollPerAgent = 0.;
				double tollPerAgentPerKm = 0.;
				
				for (Double tollSum_timeBin : this.linkId2timeBin2tollSum.get(linkId).values()){
					totalToll = totalToll + tollSum_timeBin;
				}
				
				for (Integer leavingAgents_timeBin : this.linkId2timeBin2leavingAgents.get(linkId).values()){
					leavingAgents = leavingAgents + leavingAgents_timeBin;
				}
				
				tollPerAgent = totalToll/leavingAgents;
				tollPerAgentPerKm = 1000.*(tollPerAgent/scenario.getNetwork().getLinks().get(linkId).getLength());
				int tollPerAgentPerKmClassified = classifyTollPerAgentPerKm(tollPerAgentPerKm);
				
				bw.write(linkId + ";" + totalToll + ";" + leavingAgents + ";" + tollPerAgent + ";" + tollPerAgentPerKm +";" + tollPerAgentPerKmClassified);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//LOESCHEN
	Map<Id,Map<Double,List<NoiseEvent>>> mapTMP = new HashMap<Id, Map<Double,List<NoiseEvent>>>();
	
	public void writeTollStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write("link;hour0;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour1;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour2;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour3;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour4;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour5;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour6;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour7;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour8;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour9;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour10;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour11;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour12;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour13;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour14;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour15;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour16;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour17;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour18;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour19;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour20;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour21;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour22;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour23;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour24;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour25;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.write("link");
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour"+i+";total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			}
			bw.newLine();
			
			//LOESCHEN
			for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
				Map<Double,List<NoiseEvent>> timeInterval2noiseEvents = new HashMap<Double, List<NoiseEvent>>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					List<NoiseEvent> listNoiseEvents = new ArrayList<NoiseEvent>();
					timeInterval2noiseEvents.put(timeInterval, listNoiseEvents);
				}
				mapTMP.put(linkId, timeInterval2noiseEvents);
			}
			
			//LOESCHEN
			for(NoiseEvent ne : noiseEvents) {
				Id link_id = ne.getLinkId();
				double time_ = (((int) (ne.getTime()/3600.))+1)*3600 ;
				
				Map <Double, List<NoiseEvent>> mapTmp2 = mapTMP.get(link_id);
				List<NoiseEvent> listTmp2 = mapTmp2.get(time_);
				
				listTmp2.add(ne);
				mapTmp2.put(time_, listTmp2);
				
				mapTMP.put(link_id, mapTmp2);
			}
			
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()){
				
				Map<Integer,Double> hour2toll = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2leavingAgents = new HashMap<Integer, Integer>();
				Map<Integer,Double> hour2tollPerAgent = new HashMap<Integer, Double>();
				Map<Integer,Double> hour2tollPerAgentPerKm = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2tollPerAgentPerKmClassified = new HashMap<Integer, Integer>();
				
				for (int i = 0 ; i<31 ; i++) {
					hour2toll.put(i, 0.);
					hour2leavingAgents.put(i, 0);
				}
				
				Map<Double,Double> timeBin2tollSum = linkId2timeBin2tollSum.get(linkId);
				
				for (Double timeBin : timeBin2tollSum.keySet()){
					int hour = (int) (timeBin /3600);
					double toll = hour2toll.get(hour) + timeBin2tollSum.get(timeBin);
					hour2toll.put(hour, toll);
				}
				
				Map<Double,Integer> timeBin2leavingAgents = linkId2timeBin2leavingAgents.get(linkId);
				
				for (Double timeBin : timeBin2leavingAgents.keySet()){
					int hour = (int) (timeBin /3600);
					int leavingAgents = hour2leavingAgents.get(hour) + timeBin2leavingAgents.get(timeBin);
					hour2leavingAgents.put(hour, leavingAgents);
				}
				
				for (int i = 0 ; i<31 ; i++) {
					hour2tollPerAgent.put(i, hour2toll.get(i)/hour2leavingAgents.get(i));
					hour2tollPerAgentPerKm.put(i, 1000.*(hour2tollPerAgent.get(i)/scenario.getNetwork().getLinks().get(linkId).getLength()));
					hour2tollPerAgentPerKmClassified.put(i, classifyTollPerAgentPerKm(hour2tollPerAgentPerKm.get(i)));
				}
				
//				bw.write(linkId + ";" + "" + ";"  + hour2toll.get(0) + ";" + hour2leavingAgents.get(0) + ";" + hour2tollPerAgent.get(0) + ";" + hour2tollPerAgentPerKm.get(0) +";" + hour2tollPerAgentPerKmClassified.get(0) +";" + "" + ";"  + hour2toll.get(1) + ";" + hour2leavingAgents.get(1) + ";" + hour2tollPerAgent.get(1) + ";" + hour2tollPerAgentPerKm.get(1) +";" + hour2tollPerAgentPerKmClassified.get(1) +";" + "" + ";"  + hour2toll.get(2) + ";" + hour2leavingAgents.get(2) + ";" + hour2tollPerAgent.get(2) + ";" + hour2tollPerAgentPerKm.get(2) +";" + hour2tollPerAgentPerKmClassified.get(2) +";" + "" + ";"  + hour2toll.get(3) + ";" + hour2leavingAgents.get(3) + ";" + hour2tollPerAgent.get(3) + ";" + hour2tollPerAgentPerKm.get(3) +";" + hour2tollPerAgentPerKmClassified.get(3) +";" + "" + ";"  + hour2toll.get(4) + ";" + hour2leavingAgents.get(4) + ";" + hour2tollPerAgent.get(4) + ";" + hour2tollPerAgentPerKm.get(4) +";" + hour2tollPerAgentPerKmClassified.get(4) +";" + "" + ";"  + hour2toll.get(5) + ";" + hour2leavingAgents.get(5) + ";" + hour2tollPerAgent.get(5) + ";" + hour2tollPerAgentPerKm.get(5) +";" + hour2tollPerAgentPerKmClassified.get(5) +";" + "" + ";"  + hour2toll.get(6) + ";" + hour2leavingAgents.get(6) + ";" + hour2tollPerAgent.get(6) + ";" + hour2tollPerAgentPerKm.get(6) +";" + hour2tollPerAgentPerKmClassified.get(6) +";" + "" + ";"  + hour2toll.get(7) + ";" + hour2leavingAgents.get(7) + ";" + hour2tollPerAgent.get(7) + ";" + hour2tollPerAgentPerKm.get(7) +";" + hour2tollPerAgentPerKmClassified.get(7) +";" + "" + ";"  + hour2toll.get(8) + ";" + hour2leavingAgents.get(8) + ";" + hour2tollPerAgent.get(8) + ";" + hour2tollPerAgentPerKm.get(8) +";" + hour2tollPerAgentPerKmClassified.get(8) +";" + "" + ";"  + hour2toll.get(9) + ";" + hour2leavingAgents.get(9) + ";" + hour2tollPerAgent.get(9) + ";" + hour2tollPerAgentPerKm.get(9) +";" + hour2tollPerAgentPerKmClassified.get(9) +";" + "" + ";"  + hour2toll.get(10) + ";" + hour2leavingAgents.get(10) + ";" + hour2tollPerAgent.get(10) + ";" + hour2tollPerAgentPerKm.get(10) +";" + hour2tollPerAgentPerKmClassified.get(10) +";" + "" + ";"  + hour2toll.get(11) + ";" + hour2leavingAgents.get(11) + ";" + hour2tollPerAgent.get(11) + ";" + hour2tollPerAgentPerKm.get(11) +";" + hour2tollPerAgentPerKmClassified.get(11) +";" + "" + ";"  + hour2toll.get(12) + ";" + hour2leavingAgents.get(12) + ";" + hour2tollPerAgent.get(12) + ";" + hour2tollPerAgentPerKm.get(12) +";" + hour2tollPerAgentPerKmClassified.get(12) +";" + "" + ";"  + hour2toll.get(13) + ";" + hour2leavingAgents.get(13) + ";" + hour2tollPerAgent.get(13) + ";" + hour2tollPerAgentPerKm.get(13) +";" + hour2tollPerAgentPerKmClassified.get(13) +";" + "" + ";"  + hour2toll.get(14) + ";" + hour2leavingAgents.get(14) + ";" + hour2tollPerAgent.get(14) + ";" + hour2tollPerAgentPerKm.get(14) +";" + hour2tollPerAgentPerKmClassified.get(14) +";" + "" + ";"  + hour2toll.get(15) + ";" + hour2leavingAgents.get(15) + ";" + hour2tollPerAgent.get(15) + ";" + hour2tollPerAgentPerKm.get(15) +";" + hour2tollPerAgentPerKmClassified.get(15) +";" + "" + ";"  + hour2toll.get(16) + ";" + hour2leavingAgents.get(16) + ";" + hour2tollPerAgent.get(16) + ";" + hour2tollPerAgentPerKm.get(16) +";" + hour2tollPerAgentPerKmClassified.get(16) +";" + "" + ";"  + hour2toll.get(17) + ";" + hour2leavingAgents.get(17) + ";" + hour2tollPerAgent.get(17) + ";" + hour2tollPerAgentPerKm.get(17) +";" + hour2tollPerAgentPerKmClassified.get(17) +";" + "" + ";"  + hour2toll.get(18) + ";" + hour2leavingAgents.get(18) + ";" + hour2tollPerAgent.get(18) + ";" + hour2tollPerAgentPerKm.get(18) +";" + hour2tollPerAgentPerKmClassified.get(18) +";" + "" + ";"  + hour2toll.get(19) + ";" + hour2leavingAgents.get(19) + ";" + hour2tollPerAgent.get(19) + ";" + hour2tollPerAgentPerKm.get(19) +";" + hour2tollPerAgentPerKmClassified.get(19) +";" + "" + ";"  + hour2toll.get(20) + ";" + hour2leavingAgents.get(20) + ";" + hour2tollPerAgent.get(20) + ";" + hour2tollPerAgentPerKm.get(20) +";" + hour2tollPerAgentPerKmClassified.get(20) +";" + "" + ";"  + hour2toll.get(21) + ";" + hour2leavingAgents.get(21) + ";" + hour2tollPerAgent.get(21) + ";" + hour2tollPerAgentPerKm.get(21) +";" + hour2tollPerAgentPerKmClassified.get(21) +";"+ "" + ";"  + hour2toll.get(22) + ";" + hour2leavingAgents.get(22) + ";" + hour2tollPerAgent.get(22) + ";" + hour2tollPerAgentPerKm.get(22) +";" + hour2tollPerAgentPerKmClassified.get(22) +";"  + "" + ";"  + hour2toll.get(23) + ";" + hour2leavingAgents.get(23) + ";" + hour2tollPerAgent.get(23) + ";" + hour2tollPerAgentPerKm.get(23) + ";" + hour2tollPerAgentPerKmClassified.get(23) +";"  + "" + ";"  + hour2toll.get(24) + ";" + hour2leavingAgents.get(24) + ";" + hour2tollPerAgent.get(24) + ";" + hour2tollPerAgentPerKm.get(24) + ";" + hour2tollPerAgentPerKmClassified.get(24) +";"  + "" + ";"  + hour2toll.get(25) + ";" + hour2leavingAgents.get(25) + ";" + hour2tollPerAgent.get(25) + ";" + hour2tollPerAgentPerKm.get(25) + ";" + hour2tollPerAgentPerKmClassified.get(25));
				bw.write(linkId.toString()); 
				for(int i = 0 ; i < 26 ; i++) {
					bw.write(";"+""+";"+hour2toll.get(i) + ";" + hour2leavingAgents.get(i) + ";" + hour2tollPerAgent.get(i) + ";" + hour2tollPerAgentPerKm.get(i) +";" + hour2tollPerAgentPerKmClassified.get(i));	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsPerHourCar(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write("link;hour0;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour1;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour2;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour3;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour4;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour5;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour6;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour7;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour8;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour9;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour10;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour11;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour12;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour13;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour14;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour15;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour16;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour17;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour18;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour19;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour20;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour21;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour22;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour23;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour24;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour25;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.write("link");
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour"+i+";total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			}
			bw.newLine();
			
			//LOESCHEN
			for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
				Map<Double,List<NoiseEvent>> timeInterval2noiseEvents = new HashMap<Double, List<NoiseEvent>>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					List<NoiseEvent> listNoiseEvents = new ArrayList<NoiseEvent>();
					timeInterval2noiseEvents.put(timeInterval, listNoiseEvents);
				}
				mapTMP.put(linkId, timeInterval2noiseEvents);
			}
			
			//LOESCHEN
			for(NoiseEvent ne : noiseEventsCar) {
				Id link_id = ne.getLinkId();
				double time_ = (((int) (ne.getTime()/3600.))+1)*3600 ;
				
				Map <Double, List<NoiseEvent>> mapTmp2 = mapTMP.get(link_id);
				List<NoiseEvent> listTmp2 = mapTmp2.get(time_);
				
				listTmp2.add(ne);
				mapTmp2.put(time_, listTmp2);
				
				mapTMP.put(link_id, mapTmp2);
			}
			
			
			for (Id linkId : this.linkId2timeBin2tollSumCar.keySet()){
				
				Map<Integer,Double> hour2toll = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2leavingAgents = new HashMap<Integer, Integer>();
				Map<Integer,Double> hour2tollPerAgent = new HashMap<Integer, Double>();
				Map<Integer,Double> hour2tollPerAgentPerKm = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2tollPerAgentPerKmClassified = new HashMap<Integer, Integer>();
				
				for (int i = 0 ; i<31 ; i++) {
					hour2toll.put(i, 0.);
					hour2leavingAgents.put(i, 0);
				}
				
				Map<Double,Double> timeBin2tollSum = linkId2timeBin2tollSumCar.get(linkId);
				
				for (Double timeBin : timeBin2tollSum.keySet()){
					int hour = (int) (timeBin /3600);
					double toll = hour2toll.get(hour) + timeBin2tollSum.get(timeBin);
					hour2toll.put(hour, toll);
				}
				
				Map<Double,Integer> timeBin2leavingAgents = linkId2timeBin2leavingAgentsCar.get(linkId);
				
				for (Double timeBin : timeBin2leavingAgents.keySet()){
					int hour = (int) (timeBin /3600);
					int leavingAgents = hour2leavingAgents.get(hour) + timeBin2leavingAgents.get(timeBin);
					hour2leavingAgents.put(hour, leavingAgents);
				}
				
				for (int i = 0 ; i<31 ; i++) {
					hour2tollPerAgent.put(i, hour2toll.get(i)/hour2leavingAgents.get(i));
					hour2tollPerAgentPerKm.put(i, 1000.*(hour2tollPerAgent.get(i)/scenario.getNetwork().getLinks().get(linkId).getLength()));
					hour2tollPerAgentPerKmClassified.put(i, classifyTollPerAgentPerKm(hour2tollPerAgentPerKm.get(i)));
				}
				
//				bw.write(linkId + ";" + "" + ";"  + hour2toll.get(0) + ";" + hour2leavingAgents.get(0) + ";" + hour2tollPerAgent.get(0) + ";" + hour2tollPerAgentPerKm.get(0) +";" + hour2tollPerAgentPerKmClassified.get(0) +";" + "" + ";"  + hour2toll.get(1) + ";" + hour2leavingAgents.get(1) + ";" + hour2tollPerAgent.get(1) + ";" + hour2tollPerAgentPerKm.get(1) +";" + hour2tollPerAgentPerKmClassified.get(1) +";" + "" + ";"  + hour2toll.get(2) + ";" + hour2leavingAgents.get(2) + ";" + hour2tollPerAgent.get(2) + ";" + hour2tollPerAgentPerKm.get(2) +";" + hour2tollPerAgentPerKmClassified.get(2) +";" + "" + ";"  + hour2toll.get(3) + ";" + hour2leavingAgents.get(3) + ";" + hour2tollPerAgent.get(3) + ";" + hour2tollPerAgentPerKm.get(3) +";" + hour2tollPerAgentPerKmClassified.get(3) +";" + "" + ";"  + hour2toll.get(4) + ";" + hour2leavingAgents.get(4) + ";" + hour2tollPerAgent.get(4) + ";" + hour2tollPerAgentPerKm.get(4) +";" + hour2tollPerAgentPerKmClassified.get(4) +";" + "" + ";"  + hour2toll.get(5) + ";" + hour2leavingAgents.get(5) + ";" + hour2tollPerAgent.get(5) + ";" + hour2tollPerAgentPerKm.get(5) +";" + hour2tollPerAgentPerKmClassified.get(5) +";" + "" + ";"  + hour2toll.get(6) + ";" + hour2leavingAgents.get(6) + ";" + hour2tollPerAgent.get(6) + ";" + hour2tollPerAgentPerKm.get(6) +";" + hour2tollPerAgentPerKmClassified.get(6) +";" + "" + ";"  + hour2toll.get(7) + ";" + hour2leavingAgents.get(7) + ";" + hour2tollPerAgent.get(7) + ";" + hour2tollPerAgentPerKm.get(7) +";" + hour2tollPerAgentPerKmClassified.get(7) +";" + "" + ";"  + hour2toll.get(8) + ";" + hour2leavingAgents.get(8) + ";" + hour2tollPerAgent.get(8) + ";" + hour2tollPerAgentPerKm.get(8) +";" + hour2tollPerAgentPerKmClassified.get(8) +";" + "" + ";"  + hour2toll.get(9) + ";" + hour2leavingAgents.get(9) + ";" + hour2tollPerAgent.get(9) + ";" + hour2tollPerAgentPerKm.get(9) +";" + hour2tollPerAgentPerKmClassified.get(9) +";" + "" + ";"  + hour2toll.get(10) + ";" + hour2leavingAgents.get(10) + ";" + hour2tollPerAgent.get(10) + ";" + hour2tollPerAgentPerKm.get(10) +";" + hour2tollPerAgentPerKmClassified.get(10) +";" + "" + ";"  + hour2toll.get(11) + ";" + hour2leavingAgents.get(11) + ";" + hour2tollPerAgent.get(11) + ";" + hour2tollPerAgentPerKm.get(11) +";" + hour2tollPerAgentPerKmClassified.get(11) +";" + "" + ";"  + hour2toll.get(12) + ";" + hour2leavingAgents.get(12) + ";" + hour2tollPerAgent.get(12) + ";" + hour2tollPerAgentPerKm.get(12) +";" + hour2tollPerAgentPerKmClassified.get(12) +";" + "" + ";"  + hour2toll.get(13) + ";" + hour2leavingAgents.get(13) + ";" + hour2tollPerAgent.get(13) + ";" + hour2tollPerAgentPerKm.get(13) +";" + hour2tollPerAgentPerKmClassified.get(13) +";" + "" + ";"  + hour2toll.get(14) + ";" + hour2leavingAgents.get(14) + ";" + hour2tollPerAgent.get(14) + ";" + hour2tollPerAgentPerKm.get(14) +";" + hour2tollPerAgentPerKmClassified.get(14) +";" + "" + ";"  + hour2toll.get(15) + ";" + hour2leavingAgents.get(15) + ";" + hour2tollPerAgent.get(15) + ";" + hour2tollPerAgentPerKm.get(15) +";" + hour2tollPerAgentPerKmClassified.get(15) +";" + "" + ";"  + hour2toll.get(16) + ";" + hour2leavingAgents.get(16) + ";" + hour2tollPerAgent.get(16) + ";" + hour2tollPerAgentPerKm.get(16) +";" + hour2tollPerAgentPerKmClassified.get(16) +";" + "" + ";"  + hour2toll.get(17) + ";" + hour2leavingAgents.get(17) + ";" + hour2tollPerAgent.get(17) + ";" + hour2tollPerAgentPerKm.get(17) +";" + hour2tollPerAgentPerKmClassified.get(17) +";" + "" + ";"  + hour2toll.get(18) + ";" + hour2leavingAgents.get(18) + ";" + hour2tollPerAgent.get(18) + ";" + hour2tollPerAgentPerKm.get(18) +";" + hour2tollPerAgentPerKmClassified.get(18) +";" + "" + ";"  + hour2toll.get(19) + ";" + hour2leavingAgents.get(19) + ";" + hour2tollPerAgent.get(19) + ";" + hour2tollPerAgentPerKm.get(19) +";" + hour2tollPerAgentPerKmClassified.get(19) +";" + "" + ";"  + hour2toll.get(20) + ";" + hour2leavingAgents.get(20) + ";" + hour2tollPerAgent.get(20) + ";" + hour2tollPerAgentPerKm.get(20) +";" + hour2tollPerAgentPerKmClassified.get(20) +";" + "" + ";"  + hour2toll.get(21) + ";" + hour2leavingAgents.get(21) + ";" + hour2tollPerAgent.get(21) + ";" + hour2tollPerAgentPerKm.get(21) +";" + hour2tollPerAgentPerKmClassified.get(21) +";"+ "" + ";"  + hour2toll.get(22) + ";" + hour2leavingAgents.get(22) + ";" + hour2tollPerAgent.get(22) + ";" + hour2tollPerAgentPerKm.get(22) +";" + hour2tollPerAgentPerKmClassified.get(22) +";"  + "" + ";"  + hour2toll.get(23) + ";" + hour2leavingAgents.get(23) + ";" + hour2tollPerAgent.get(23) + ";" + hour2tollPerAgentPerKm.get(23) + ";" + hour2tollPerAgentPerKmClassified.get(23) +";"  + "" + ";"  + hour2toll.get(24) + ";" + hour2leavingAgents.get(24) + ";" + hour2tollPerAgent.get(24) + ";" + hour2tollPerAgentPerKm.get(24) + ";" + hour2tollPerAgentPerKmClassified.get(24) +";"  + "" + ";"  + hour2toll.get(25) + ";" + hour2leavingAgents.get(25) + ";" + hour2tollPerAgent.get(25) + ";" + hour2tollPerAgentPerKm.get(25) + ";" + hour2tollPerAgentPerKmClassified.get(25));
				bw.write(linkId.toString()); 
				for(int i = 0 ; i < 26 ; i++) {
					bw.write(";"+""+";"+hour2toll.get(i) + ";" + hour2leavingAgents.get(i) + ";" + hour2tollPerAgent.get(i) + ";" + hour2tollPerAgentPerKm.get(i) +";" + hour2tollPerAgentPerKmClassified.get(i));	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsPerHourHdv(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write("link;hour0;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour1;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour2;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour3;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour4;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour5;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour6;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour7;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour8;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour9;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour10;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour11;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour12;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour13;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour14;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour15;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour16;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour17;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour18;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour19;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour20;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour21;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour22;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour23;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour24;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour25;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.write("link");
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour"+i+";total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			}
			bw.newLine();
			
			//LOESCHEN
			for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
				Map<Double,List<NoiseEvent>> timeInterval2noiseEvents = new HashMap<Double, List<NoiseEvent>>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					List<NoiseEvent> listNoiseEvents = new ArrayList<NoiseEvent>();
					timeInterval2noiseEvents.put(timeInterval, listNoiseEvents);
				}
				mapTMP.put(linkId, timeInterval2noiseEvents);
			}
			
			//LOESCHEN
			for(NoiseEvent ne : noiseEventsHdv) {
				Id link_id = ne.getLinkId();
				double time_ = (((int) (ne.getTime()/3600.))+1)*3600 ;
				
				Map <Double, List<NoiseEvent>> mapTmp2 = mapTMP.get(link_id);
				List<NoiseEvent> listTmp2 = mapTmp2.get(time_);
				
				listTmp2.add(ne);
				mapTmp2.put(time_, listTmp2);
				
				mapTMP.put(link_id, mapTmp2);
			}
			
			
			for (Id linkId : this.linkId2timeBin2tollSumHdv.keySet()){
				
				Map<Integer,Double> hour2toll = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2leavingAgents = new HashMap<Integer, Integer>();
				Map<Integer,Double> hour2tollPerAgent = new HashMap<Integer, Double>();
				Map<Integer,Double> hour2tollPerAgentPerKm = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2tollPerAgentPerKmClassified = new HashMap<Integer, Integer>();
				
				for (int i = 0 ; i<31 ; i++) {
					hour2toll.put(i, 0.);
					hour2leavingAgents.put(i, 0);
				}
				
				Map<Double,Double> timeBin2tollSum = linkId2timeBin2tollSumHdv.get(linkId);
				
				for (Double timeBin : timeBin2tollSum.keySet()){
					int hour = (int) (timeBin /3600);
					double toll = hour2toll.get(hour) + timeBin2tollSum.get(timeBin);
					hour2toll.put(hour, toll);
				}
				
				Map<Double,Integer> timeBin2leavingAgents = linkId2timeBin2leavingAgentsHdv.get(linkId);
				
				for (Double timeBin : timeBin2leavingAgents.keySet()){
					int hour = (int) (timeBin /3600);
					int leavingAgents = hour2leavingAgents.get(hour) + timeBin2leavingAgents.get(timeBin);
					hour2leavingAgents.put(hour, leavingAgents);
				}
				
				for (int i = 0 ; i<31 ; i++) {
					hour2tollPerAgent.put(i, hour2toll.get(i)/hour2leavingAgents.get(i));
					hour2tollPerAgentPerKm.put(i, 1000.*(hour2tollPerAgent.get(i)/scenario.getNetwork().getLinks().get(linkId).getLength()));
					hour2tollPerAgentPerKmClassified.put(i, classifyTollPerAgentPerKm(hour2tollPerAgentPerKm.get(i)));
				}
				
//				bw.write(linkId + ";" + "" + ";"  + hour2toll.get(0) + ";" + hour2leavingAgents.get(0) + ";" + hour2tollPerAgent.get(0) + ";" + hour2tollPerAgentPerKm.get(0) +";" + hour2tollPerAgentPerKmClassified.get(0) +";" + "" + ";"  + hour2toll.get(1) + ";" + hour2leavingAgents.get(1) + ";" + hour2tollPerAgent.get(1) + ";" + hour2tollPerAgentPerKm.get(1) +";" + hour2tollPerAgentPerKmClassified.get(1) +";" + "" + ";"  + hour2toll.get(2) + ";" + hour2leavingAgents.get(2) + ";" + hour2tollPerAgent.get(2) + ";" + hour2tollPerAgentPerKm.get(2) +";" + hour2tollPerAgentPerKmClassified.get(2) +";" + "" + ";"  + hour2toll.get(3) + ";" + hour2leavingAgents.get(3) + ";" + hour2tollPerAgent.get(3) + ";" + hour2tollPerAgentPerKm.get(3) +";" + hour2tollPerAgentPerKmClassified.get(3) +";" + "" + ";"  + hour2toll.get(4) + ";" + hour2leavingAgents.get(4) + ";" + hour2tollPerAgent.get(4) + ";" + hour2tollPerAgentPerKm.get(4) +";" + hour2tollPerAgentPerKmClassified.get(4) +";" + "" + ";"  + hour2toll.get(5) + ";" + hour2leavingAgents.get(5) + ";" + hour2tollPerAgent.get(5) + ";" + hour2tollPerAgentPerKm.get(5) +";" + hour2tollPerAgentPerKmClassified.get(5) +";" + "" + ";"  + hour2toll.get(6) + ";" + hour2leavingAgents.get(6) + ";" + hour2tollPerAgent.get(6) + ";" + hour2tollPerAgentPerKm.get(6) +";" + hour2tollPerAgentPerKmClassified.get(6) +";" + "" + ";"  + hour2toll.get(7) + ";" + hour2leavingAgents.get(7) + ";" + hour2tollPerAgent.get(7) + ";" + hour2tollPerAgentPerKm.get(7) +";" + hour2tollPerAgentPerKmClassified.get(7) +";" + "" + ";"  + hour2toll.get(8) + ";" + hour2leavingAgents.get(8) + ";" + hour2tollPerAgent.get(8) + ";" + hour2tollPerAgentPerKm.get(8) +";" + hour2tollPerAgentPerKmClassified.get(8) +";" + "" + ";"  + hour2toll.get(9) + ";" + hour2leavingAgents.get(9) + ";" + hour2tollPerAgent.get(9) + ";" + hour2tollPerAgentPerKm.get(9) +";" + hour2tollPerAgentPerKmClassified.get(9) +";" + "" + ";"  + hour2toll.get(10) + ";" + hour2leavingAgents.get(10) + ";" + hour2tollPerAgent.get(10) + ";" + hour2tollPerAgentPerKm.get(10) +";" + hour2tollPerAgentPerKmClassified.get(10) +";" + "" + ";"  + hour2toll.get(11) + ";" + hour2leavingAgents.get(11) + ";" + hour2tollPerAgent.get(11) + ";" + hour2tollPerAgentPerKm.get(11) +";" + hour2tollPerAgentPerKmClassified.get(11) +";" + "" + ";"  + hour2toll.get(12) + ";" + hour2leavingAgents.get(12) + ";" + hour2tollPerAgent.get(12) + ";" + hour2tollPerAgentPerKm.get(12) +";" + hour2tollPerAgentPerKmClassified.get(12) +";" + "" + ";"  + hour2toll.get(13) + ";" + hour2leavingAgents.get(13) + ";" + hour2tollPerAgent.get(13) + ";" + hour2tollPerAgentPerKm.get(13) +";" + hour2tollPerAgentPerKmClassified.get(13) +";" + "" + ";"  + hour2toll.get(14) + ";" + hour2leavingAgents.get(14) + ";" + hour2tollPerAgent.get(14) + ";" + hour2tollPerAgentPerKm.get(14) +";" + hour2tollPerAgentPerKmClassified.get(14) +";" + "" + ";"  + hour2toll.get(15) + ";" + hour2leavingAgents.get(15) + ";" + hour2tollPerAgent.get(15) + ";" + hour2tollPerAgentPerKm.get(15) +";" + hour2tollPerAgentPerKmClassified.get(15) +";" + "" + ";"  + hour2toll.get(16) + ";" + hour2leavingAgents.get(16) + ";" + hour2tollPerAgent.get(16) + ";" + hour2tollPerAgentPerKm.get(16) +";" + hour2tollPerAgentPerKmClassified.get(16) +";" + "" + ";"  + hour2toll.get(17) + ";" + hour2leavingAgents.get(17) + ";" + hour2tollPerAgent.get(17) + ";" + hour2tollPerAgentPerKm.get(17) +";" + hour2tollPerAgentPerKmClassified.get(17) +";" + "" + ";"  + hour2toll.get(18) + ";" + hour2leavingAgents.get(18) + ";" + hour2tollPerAgent.get(18) + ";" + hour2tollPerAgentPerKm.get(18) +";" + hour2tollPerAgentPerKmClassified.get(18) +";" + "" + ";"  + hour2toll.get(19) + ";" + hour2leavingAgents.get(19) + ";" + hour2tollPerAgent.get(19) + ";" + hour2tollPerAgentPerKm.get(19) +";" + hour2tollPerAgentPerKmClassified.get(19) +";" + "" + ";"  + hour2toll.get(20) + ";" + hour2leavingAgents.get(20) + ";" + hour2tollPerAgent.get(20) + ";" + hour2tollPerAgentPerKm.get(20) +";" + hour2tollPerAgentPerKmClassified.get(20) +";" + "" + ";"  + hour2toll.get(21) + ";" + hour2leavingAgents.get(21) + ";" + hour2tollPerAgent.get(21) + ";" + hour2tollPerAgentPerKm.get(21) +";" + hour2tollPerAgentPerKmClassified.get(21) +";"+ "" + ";"  + hour2toll.get(22) + ";" + hour2leavingAgents.get(22) + ";" + hour2tollPerAgent.get(22) + ";" + hour2tollPerAgentPerKm.get(22) +";" + hour2tollPerAgentPerKmClassified.get(22) +";"  + "" + ";"  + hour2toll.get(23) + ";" + hour2leavingAgents.get(23) + ";" + hour2tollPerAgent.get(23) + ";" + hour2tollPerAgentPerKm.get(23) + ";" + hour2tollPerAgentPerKmClassified.get(23) +";"  + "" + ";"  + hour2toll.get(24) + ";" + hour2leavingAgents.get(24) + ";" + hour2tollPerAgent.get(24) + ";" + hour2tollPerAgentPerKm.get(24) + ";" + hour2tollPerAgentPerKmClassified.get(24) +";"  + "" + ";"  + hour2toll.get(25) + ";" + hour2leavingAgents.get(25) + ";" + hour2tollPerAgent.get(25) + ";" + hour2tollPerAgentPerKm.get(25) + ";" + hour2tollPerAgentPerKmClassified.get(25));
				bw.write(linkId.toString()); 
				for(int i = 0 ; i < 26 ; i++) {
					bw.write(";"+""+";"+hour2toll.get(i) + ";" + hour2leavingAgents.get(i) + ";" + hour2tollPerAgent.get(i) + ";" + hour2tollPerAgentPerKm.get(i) +";" + hour2tollPerAgentPerKmClassified.get(i));	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeTollStatsPerActivity(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("actType;;sumTollAffected;shareTollAffected;;sumActivityDuration;shareActivityDuration");
			bw.newLine();
			
			List<String> actTypesTollAffected = new ArrayList<String>();
			List<String> actTypesActivityDuration = new ArrayList<String>();
			Map<String,Double> actType2sumTollAffected = new HashMap<String, Double>();
			Map<String,Double> actType2sumActivityDuration = new HashMap<String, Double>();
			
			for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
				for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
					for(int actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
						double actStart = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getFirst();
						double actEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getSecond();
						
						if(actEnd>24*3600){
							actEnd = 24*3600;
						}
						if(actStart>24*3600){
							actStart = 24*3600;
						}
						double activityDuration = actEnd - actStart;
						
						String actType = personId2actNumber2actType.get(personId).get(actNumber);
						
						if(!(actTypesActivityDuration.contains(actType))) {
							actTypesActivityDuration.add(actType);
							actType2sumActivityDuration.put(actType, activityDuration);
						} else {
							double newSum = actType2sumActivityDuration.get(actType) + activityDuration;
							actType2sumActivityDuration.put(actType, newSum);
						}	
					}
				}
			}
			
			for(NoiseEventAffected event : noiseEventsAffected) {
				String actType = event.getActType();
				if(!(actTypesTollAffected.contains(actType))) {
					actTypesTollAffected.add(actType);
					actType2sumTollAffected.put(actType, event.getAmount());
				} else {
					double newSum = actType2sumTollAffected.get(actType) + event.getAmount();
					actType2sumTollAffected.put(actType, newSum);
				}	
			}
			
			double totalSumTollAffected = actType2sumTollAffected.get("home") + actType2sumTollAffected.get("work") + actType2sumTollAffected.get("secondary");
			double totalSumActivityDuration = actType2sumActivityDuration.get("home") + actType2sumActivityDuration.get("work") + actType2sumActivityDuration.get("secondary");
			bw.write("home;;"+actType2sumTollAffected.get("home")+";"+(actType2sumTollAffected.get("home")/totalSumTollAffected)+";;"+actType2sumActivityDuration.get("home")+";"+(actType2sumActivityDuration.get("home")/totalSumActivityDuration));
			bw.newLine();
			bw.write("work;;"+actType2sumTollAffected.get("work")+";"+(actType2sumTollAffected.get("work")/totalSumTollAffected)+";;"+actType2sumActivityDuration.get("work")+";"+(actType2sumActivityDuration.get("work")/totalSumActivityDuration));
			bw.newLine();	
			bw.write("secondary;;"+actType2sumTollAffected.get("secondary")+";"+(actType2sumTollAffected.get("secondary")/totalSumTollAffected)+";;"+actType2sumActivityDuration.get("secondary")+";"+(actType2sumActivityDuration.get("secondary")/totalSumActivityDuration));
			bw.newLine();	
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsCompareHomeVsActivityBased(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;damage homeBased;damage activityBased");
			bw.newLine();
			
			for(Id personId : scenario.getPopulation().getPersons().keySet()) {
				
				double damageActivityBased = personId2damageSum.get(personId);
				double damageHomeBased = personId2homeBasedDamageSum.get(personId);
				
				bw.write(personId+";"+damageHomeBased+";"+damageActivityBased);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsIteration2tollSum(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("iteration;tollSum;tollSumCar;tollSumHdv");
			bw.newLine();
			
			for(int iteration : iteration2tollSum.keySet()) {
				
				bw.write(iteration+";"+iteration2tollSum.get(iteration)+";"+iteration2tollSumCar.get(iteration)+";"+iteration2tollSumHdv.get(iteration));
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int classifyTollPerAgentPerKm(double tollPerAgentPerKm) {
		int classifiedValue = 0;
		
		if(tollPerAgentPerKm==0) {
			classifiedValue = 0;
		} else if(tollPerAgentPerKm<0.00005) {
			classifiedValue = 1;
		} else if(tollPerAgentPerKm<0.0001) {
			classifiedValue = 2;
		} else if(tollPerAgentPerKm<0.0002) {
			classifiedValue = 3;
		} else if(tollPerAgentPerKm<0.0004) {
			classifiedValue = 4;
		} else if(tollPerAgentPerKm<0.0008) {
			classifiedValue = 5;
		} else if(tollPerAgentPerKm<0.0016) {
			classifiedValue = 6;
		} else if(tollPerAgentPerKm<0.0032) {
			classifiedValue = 7;
		}  else if(tollPerAgentPerKm<0.0064) {
			classifiedValue = 8;
		}  else if(tollPerAgentPerKm<0.0128) {
			classifiedValue = 9;
		}  else if(tollPerAgentPerKm<0.0256) {
			classifiedValue = 10;
		}  else if(tollPerAgentPerKm<0.0512) {
			classifiedValue = 11;
		}  else if(tollPerAgentPerKm<0.1) {
			classifiedValue = 12;
		}  else if(tollPerAgentPerKm<0.2) {
			classifiedValue = 13;
		}  else if(tollPerAgentPerKm<0.4) {
			classifiedValue = 14;
		} else if((tollPerAgentPerKm>=0.4)&&(tollPerAgentPerKm<100000)) {
			classifiedValue = 15; 
		}
		
		return classifiedValue;
	}
	
	private int classifyTollPerAgentPerKmCar(double tollPerAgentPerKm) {
		int classifiedValue = 0;
		
		//TODO: Adapt the values!
		if(tollPerAgentPerKm==0) {
			classifiedValue = 0;
		} else if(tollPerAgentPerKm<0.00005) {
			classifiedValue = 1;
		} else if(tollPerAgentPerKm<0.0001) {
			classifiedValue = 2;
		} else if(tollPerAgentPerKm<0.0002) {
			classifiedValue = 3;
		} else if(tollPerAgentPerKm<0.0004) {
			classifiedValue = 4;
		} else if(tollPerAgentPerKm<0.0008) {
			classifiedValue = 5;
		} else if(tollPerAgentPerKm<0.0016) {
			classifiedValue = 6;
		} else if(tollPerAgentPerKm<0.0032) {
			classifiedValue = 7;
		}  else if(tollPerAgentPerKm<0.0064) {
			classifiedValue = 8;
		}  else if(tollPerAgentPerKm<0.0128) {
			classifiedValue = 9;
		}  else if(tollPerAgentPerKm<0.0256) {
			classifiedValue = 10;
		}  else if(tollPerAgentPerKm<0.0512) {
			classifiedValue = 11;
		}  else if(tollPerAgentPerKm<0.1) {
			classifiedValue = 12;
		}  else if(tollPerAgentPerKm<0.2) {
			classifiedValue = 13;
		}  else if(tollPerAgentPerKm<0.4) {
			classifiedValue = 14;
		} else if((tollPerAgentPerKm>=0.4)&&(tollPerAgentPerKm<100000)) {
			classifiedValue = 15; 
		}
		
		return classifiedValue;
	}
	
	private int classifyTollPerAgentPerKmHdv(double tollPerAgentPerKm) {
		int classifiedValue = 0;
		
		// TODO: Adapt the values
		if(tollPerAgentPerKm==0) {
			classifiedValue = 0;
		} else if(tollPerAgentPerKm<0.00005) {
			classifiedValue = 1;
		} else if(tollPerAgentPerKm<0.0001) {
			classifiedValue = 2;
		} else if(tollPerAgentPerKm<0.0002) {
			classifiedValue = 3;
		} else if(tollPerAgentPerKm<0.0004) {
			classifiedValue = 4;
		} else if(tollPerAgentPerKm<0.0008) {
			classifiedValue = 5;
		} else if(tollPerAgentPerKm<0.0016) {
			classifiedValue = 6;
		} else if(tollPerAgentPerKm<0.0032) {
			classifiedValue = 7;
		}  else if(tollPerAgentPerKm<0.0064) {
			classifiedValue = 8;
		}  else if(tollPerAgentPerKm<0.0128) {
			classifiedValue = 9;
		}  else if(tollPerAgentPerKm<0.0256) {
			classifiedValue = 10;
		}  else if(tollPerAgentPerKm<0.0512) {
			classifiedValue = 11;
		}  else if(tollPerAgentPerKm<0.1) {
			classifiedValue = 12;
		}  else if(tollPerAgentPerKm<0.2) {
			classifiedValue = 13;
		}  else if(tollPerAgentPerKm<0.4) {
			classifiedValue = 14;
		} else if((tollPerAgentPerKm>=0.4)&&(tollPerAgentPerKm<100000)) {
			classifiedValue = 15; 
		}
		
		return classifiedValue;
	}

	public Double getTollSum(){
		double sum = 0.;
		for (NoiseEvent event : noiseEvents){
			sum = sum + event.getAmount();
		}
		return sum;
	}
	
	public Double getTollSumCar(){
		double sum = 0.;
		for (NoiseEvent event : noiseEventsCar){
			sum = sum + event.getAmount();
		}
		return sum;
	}
	
	public Double getTollSumHdv(){
		double sum = 0.;
		for (NoiseEvent event : noiseEventsHdv){
			sum = sum + event.getAmount();
		}
		return sum;
	}
	
	public void setIteration2TollSum(int iteration) {
		iteration2tollSum.put(iteration, getTollSum());
	}
	
	public void setIteration2TollSumCar(int iteration) {
		iteration2tollSumCar.put(iteration, getTollSumCar());
	}
	
	public void setIteration2TollSumHdv(int iteration) {
		iteration2tollSumHdv.put(iteration, getTollSumHdv());
	}
	
	public Scenario getScenario() {
		return scenario;
	}

	public Map<Id, Map<Id, Map<Integer, Tuple<Double, Double>>>> getReceiverPointId2personId2actNumber2activityStartAndActivityEnd() {
		return receiverPointId2personId2actNumber2activityStartAndActivityEnd;
	}

	public Map<Id, Map<Integer, Map<Id, Tuple<Double, Double>>>> getPersonId2actNumber2receiverPointId2activityStartAndActivityEnd() {
		return personId2actNumber2receiverPointId2activityStartAndActivityEnd;
	}

	public Map<Id, Map<Integer, String>> getPersonId2actNumber2actType() {
		return personId2actNumber2actType;
	}

	public Map<Id, Integer> getPersonId2actualActNumber() {
		return personId2actualActNumber;
	}

	public Map<Id, Map<Double, Double>> getReceiverPointId2timeInterval2affectedAgentUnits() {
		return receiverPointId2timeInterval2affectedAgentUnits;
	}

	public Map<Id, Map<Double, Map<Id, Map<Integer, Tuple<Double, String>>>>> getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType() {
		return receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType;
	}

	public Map<Id, List<Id>> getReceiverPointId2ListOfHomeAgents() {
		return receiverPointId2ListOfHomeAgents;
	}

	public Map<Id, Map<Double, Double>> getReceiverPointId2timeInterval2damageCost() {
		return receiverPointId2timeInterval2damageCost;
	}

	public Map<Id, Map<Double, Double>> getReceiverPointId2timeInterval2damageCostPerAffectedAgentUnit() {
		return receiverPointId2timeInterval2damageCostPerAffectedAgentUnit;
	}

	public Map<Id, Map<Double, Double>> getLinkId2timeInterval2noiseEmission() {
		return linkId2timeInterval2noiseEmission;
	}

	public Map<Id, Map<Double, Double>> getReceiverPointId2timeInterval2noiseImmission() {
		return receiverPointId2timeInterval2noiseImmission;
	}

	public Map<Id, Map<Double, Map<Id, Double>>> getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission() {
		return receiverPointIds2timeIntervals2noiseLinks2isolatedImmission;
	}

	public List<LinkEnterEvent> getLinkLeaveEvents() {
		return linkLeaveEvents;
	}

	public List<LinkEnterEvent> getLinkLeaveEventsCar() {
		return linkLeaveEventsCar;
	}

	public List<LinkEnterEvent> getLinkLeaveEventsHdv() {
		return linkLeaveEventsHdv;
	}

	public List<Id> getHdvVehicles() {
		return hdvVehicles;
	}

	public Map<Id, List<LinkEnterEvent>> getLinkId2linkLeaveEvents() {
		return linkId2linkLeaveEvents;
	}

	public Map<Id, List<LinkEnterEvent>> getLinkId2linkLeaveEventsCar() {
		return linkId2linkLeaveEventsCar;
	}

	public Map<Id, List<LinkEnterEvent>> getLinkId2linkLeaveEventsHdv() {
		return linkId2linkLeaveEventsHdv;
	}

	public Map<Id, Map<Double, List<LinkEnterEvent>>> getLinkId2timeInterval2linkLeaveEvents() {
		return linkId2timeInterval2linkLeaveEvents;
	}

	public Map<Id, Map<Double, List<LinkEnterEvent>>> getLinkId2timeInterval2linkLeaveEventsCar() {
		return linkId2timeInterval2linkLeaveEventsCar;
	}

	public Map<Id, Map<Double, List<LinkEnterEvent>>> getLinkId2timeInterval2linkLeaveEventsHdv() {
		return linkId2timeInterval2linkLeaveEventsHdv;
	}

	public SpatialInfo getSpatialInfo() {
		return spatialInfo;
	}
	
	public double getTotalTollAffectedAgentBasedCalculation() {
		return totalTollAffectedAgentBasedCalculation;
	}
	
	public double getTotalTollAffectedAgentBasedCalculationControl() {
		return totalTollAffectedAgentBasedCalculationControl;
	}
	
	public double getTotalToll() {
		return totalToll;
	}
	
	public double getTotalTollAffected() {
		return totalTollAffected;
	}
	
	public Map <Double,Integer> getInterval2departures() {
		return interval2departures;
	}
}
