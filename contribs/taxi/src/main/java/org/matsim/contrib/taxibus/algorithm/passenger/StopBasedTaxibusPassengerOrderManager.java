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

package org.matsim.contrib.taxibus.algorithm.passenger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxibus.algorithm.utils.TaxibusUtils;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.pt.PtConstants;

/**
 * @author jbischoff
 *
 */
public class StopBasedTaxibusPassengerOrderManager implements OrderManager, ActivityStartEventHandler, PersonDepartureEventHandler,  MobsimInitializedListener {
	private QSim qSim;
	private TaxibusPassengerEngine passengerEngine;
	
	private Map<Id<Person>,MutableInt> currentPE = new HashMap<>();
	
	public StopBasedTaxibusPassengerOrderManager() {

	}
	@Override
	public void setPassengerEngine(TaxibusPassengerEngine passengerEngine) {
		this.passengerEngine = passengerEngine;
	}

	@Override
	public void reset(int iteration) {
		currentPE.clear();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<MobsimAgent> mid = Id.create(event.getPersonId(), MobsimAgent.class);
		if (qSim.getAgents().containsKey(mid))
			// to filter out drivers without an agent plan
		{
		currentPE.get(event.getPersonId()).increment();
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
		return;
			MobsimAgent mobsimAgent = qSim.getAgents().get(mid);
			if (mobsimAgent instanceof PlanAgent) {
				if (mobsimAgent.getState().equals(State.LEG))
					return;
				if (mobsimAgent.getState().equals(State.ABORT))
					return;

				PlanAgent agent = (PlanAgent)mobsimAgent;
				int nextTBRide = currentPE.get(event.getPersonId()).intValue()+3;
				PlanElement nextPlanElement = null;
				if (agent.getCurrentPlan().getPlanElements().size()>nextTBRide){
					nextPlanElement = agent.getCurrentPlan().getPlanElements().get(nextTBRide);
				}
				if (nextPlanElement != null) {
					if (nextPlanElement instanceof Activity) {
						Logger.getLogger(getClass()).error(
								"Agent" + mid.toString() + " started activity: " + event.getActType() + " next act ");
						return;
					}
					if (nextPlanElement instanceof Leg) {
						Leg leg = (Leg)nextPlanElement;
						if (leg.getMode().equals(TaxibusUtils.TAXIBUS_MODE)) {
							double departureTime = mobsimAgent.getActivityEndTime();
							if (departureTime < event.getTime()) {
								departureTime = event.getTime() + 60;
							}
							Leg wleg = (Leg) agent.getCurrentPlan().getPlanElements().get(nextTBRide-2);
							departureTime +=wleg.getTravelTime(); 
							prebookTaxiBusTrip(mobsimAgent, leg, departureTime);
						}
					}
				}
			}
		}
	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {

		this.qSim = (QSim)e.getQueueSimulation();
		Collection<MobsimAgent> agents = qSim.getAgents().values();

		for (MobsimAgent mobsimAgent : agents) {
			if (mobsimAgent instanceof PlanAgent) {
				this.currentPE.put(mobsimAgent.getId(), new MutableInt(0));
				PlanAgent agent = (PlanAgent)mobsimAgent;
				if(agent.getCurrentPlan().getPlanElements().size()>3){
				Leg leg =  (Leg) agent.getCurrentPlan().getPlanElements().get(3);
				if (leg.getMode().equals(TaxibusUtils.TAXIBUS_MODE)) {
					// if (leg.getMode().equals("car")){
					Leg wleg =  (Leg) agent.getCurrentPlan().getPlanElements().get(1);
					
					double departureTime = mobsimAgent.getActivityEndTime()+wleg.getTravelTime();
					prebookTaxiBusTrip(mobsimAgent, leg, departureTime);
				}
			}}
		}
	}

	private void prebookTaxiBusTrip(MobsimAgent mobsimAgent, Leg leg, double departureTime) {
		// System.out.println("taxi trip booked for " + mobsimAgent.getId() +" at "+Time.writeTime(departureTime));
		this.passengerEngine.prebookTrip(qSim.getSimTimer().getTimeOfDay(), (MobsimPassengerAgent)mobsimAgent,
				leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId(), departureTime);

	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (currentPE.containsKey(event.getPersonId())){
			currentPE.get(event.getPersonId()).increment();

		}
	}

}