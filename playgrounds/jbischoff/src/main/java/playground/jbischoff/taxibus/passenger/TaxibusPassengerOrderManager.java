/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.passenger;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;

import playground.jbischoff.taxibus.utils.TaxibusUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxibusPassengerOrderManager implements  ActivityStartEventHandler, MobsimInitializedListener  {
	private QSim qSim;
	private TaxibusPassengerEngine passengerEngine;
	
	public TaxibusPassengerOrderManager(TaxibusPassengerEngine passengerEngine) {
	this.passengerEngine = passengerEngine;
	}
	
	
	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		MobsimAgent mobsimAgent = qSim.getAgentMap().get(Id.create(event.getPersonId(), MobsimAgent.class));
		if (mobsimAgent instanceof PlanAgent){
			PlanAgent agent = (PlanAgent) mobsimAgent;
			Leg leg = (Leg) agent.getNextPlanElement();
			if (leg!=null ){ 
			if (leg.getMode().equals(TaxibusUtils.TAXIBUS_MODE)){
//			if (leg.getMode().equals("car")){

				Double departureTime = mobsimAgent.getActivityEndTime();
				prebookTaxiBusTrip(mobsimAgent,leg,departureTime);
			}
		}
			}
		
	}
	
	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {

		this.qSim = (QSim) e.getQueueSimulation();
	 Collection<MobsimAgent> agents = qSim.getAgents();

    for (MobsimAgent mobsimAgent : agents) {
    	if (mobsimAgent instanceof PlanAgent){
    		PlanAgent agent = (PlanAgent) mobsimAgent;
			Leg leg = (Leg) agent.getNextPlanElement();
			if (leg.getMode().equals(TaxibusUtils.TAXIBUS_MODE)){
//			if (leg.getMode().equals("car")){
				Double departureTime = mobsimAgent.getActivityEndTime();
				prebookTaxiBusTrip(mobsimAgent,leg,departureTime);
			}
		}
    }
}
	private void prebookTaxiBusTrip(MobsimAgent mobsimAgent, Leg leg, Double departureTime) {
		 if (departureTime == null){
         	throw new IllegalStateException("There is no Activity before the leg or the activity has no end time.");
         } 
		 System.out.println("taxi trip booked for " + mobsimAgent.getId());
		 leg.setDepartureTime(departureTime);
		 //Leg Departure time must be set
		 this.passengerEngine.prebookTrip(qSim.getSimTimer().getTimeOfDay(), (MobsimPassengerAgent)mobsimAgent, leg);
		
		 
	}

}