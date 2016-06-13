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
package playground.artemc.psim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import playground.artemc.crowding.internalization.CapacityDelayEvent;
import playground.artemc.crowding.internalization.TransferDelayInVehicleEvent;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;

import java.util.HashMap;

/**
 * @author ikaddoura
 * @author artemc
 *
 */
public class CapacityDelayEventHandler implements playground.artemc.crowding.internalization.CapacityDelayEventHandler, PersonEntersVehicleEventHandler {

	private final static Logger log = Logger.getLogger(CapacityDelayEventHandler.class);

	private final Scenario scenario;
	private final double vtts_inVehicle;
	private final double vtts_waiting;
	private ScoreTracker scoreTracker;

	public HashMap<Integer, Integer> getTimeBinToPassengers() {
		return timeBinToPassengers;
	}

	private HashMap<Integer, Integer> timeBinToPassengers = new HashMap<Integer, Integer>();
	private HashMap<Integer, Double> timeBinToCapacityDelayExternality = new HashMap<Integer, Double>();

	private HashMap<Id<Person>, Double> personToBoardingTime = new HashMap<Id<Person>, Double>();

	private HashMap<Integer, Double> avgCapacityDelayExternality;

	// TODO: make configurable
//	private final double operatorCostPerVehHour = 39.93; // = 33 * 1.21 (overhead)

	public CapacityDelayEventHandler(Scenario scenario) {
		this.scenario = scenario;
		this.vtts_inVehicle = (this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.vtts_waiting = (this.scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		log.info("VTTS_inVehicleTime: " + vtts_inVehicle);
		log.info("VTTS_waiting: " + vtts_waiting);
	}

	@Override
	public void reset(int iteration) {
	}



	@Override
	public void handleEvent(CapacityDelayEvent event) {
		double amount = (event.getDelay() / 3600.0 ) * this.vtts_waiting;

		int bin = (int) (personToBoardingTime.get(event.getCausingAgentId()) / 300.0);

		if (timeBinToCapacityDelayExternality.containsKey(bin)) {
			timeBinToCapacityDelayExternality.put(bin, timeBinToCapacityDelayExternality.get(bin) + amount);
		} else {
			timeBinToCapacityDelayExternality.put(bin, amount);
		}

		//log.info("CapacityDelay:  Agent: "+ event.getCausingAgentId().toString()+"    Money: "+amount);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		if(!event.getPersonId().toString().contains("pt") && event.getVehicleId().toString().contains("bus")){
			int bin = (int) (event.getTime() / 300.0);
			if (timeBinToPassengers.containsKey(bin)) {
				timeBinToPassengers.put(bin, (timeBinToPassengers.get(bin) + 1));
			}
			else{
				timeBinToPassengers.put(bin, 1);
			}

			if(personToBoardingTime.containsKey(event.getPersonId())){
				personToBoardingTime.remove(event.getPersonId());
			}

			personToBoardingTime.put(event.getPersonId(),event.getTime());
		}
	}


	public HashMap<Integer, Double> getTimeBinToCapacityDelayExternality() {
		return timeBinToCapacityDelayExternality;
	}



}