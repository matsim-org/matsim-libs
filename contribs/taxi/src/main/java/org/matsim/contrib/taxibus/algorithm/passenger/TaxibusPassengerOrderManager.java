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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxibus.algorithm.utils.TaxibusUtils;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * @author jbischoff
 *
 */
public class TaxibusPassengerOrderManager implements ActivityStartEventHandler, MobsimInitializedListener {
	private QSim qSim;
	private TaxibusPassengerEngine passengerEngine;

	public TaxibusPassengerOrderManager() {

	}

	public void setPassengerEngine(TaxibusPassengerEngine passengerEngine) {
		this.passengerEngine = passengerEngine;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().startsWith("pt"))
			return;
		Id<MobsimAgent> mid = Id.create(event.getPersonId(), MobsimAgent.class);
		if (qSim.getAgents().containsKey(mid))
		// to filter out drivers without an agent plan
		{
			MobsimAgent mobsimAgent = qSim.getAgents().get(mid);
			if (mobsimAgent instanceof PlanAgent) {
				if (mobsimAgent.getState().equals(State.LEG))
					return;
				if (mobsimAgent.getState().equals(State.ABORT))
					return;

				PlanAgent agent = (PlanAgent)mobsimAgent;
				PlanElement nextPlanElement = agent.getNextPlanElement();
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
				PlanAgent agent = (PlanAgent)mobsimAgent;
				Leg leg = (Leg)agent.getNextPlanElement();
				if (leg.getMode().equals(TaxibusUtils.TAXIBUS_MODE)) {
					// if (leg.getMode().equals("car")){
					double departureTime = mobsimAgent.getActivityEndTime();
					prebookTaxiBusTrip(mobsimAgent, leg, departureTime);
				}
			}
		}
	}

	private void prebookTaxiBusTrip(MobsimAgent mobsimAgent, Leg leg, double departureTime) {
		// System.out.println("taxi trip booked for " + mobsimAgent.getId() +" at "+Time.writeTime(departureTime));
		this.passengerEngine.prebookTrip(qSim.getSimTimer().getTimeOfDay(), (MobsimPassengerAgent)mobsimAgent,
				leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId(), departureTime);

	}

}