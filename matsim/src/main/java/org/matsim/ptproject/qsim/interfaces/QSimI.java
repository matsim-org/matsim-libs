/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.interfaces;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.AgentFactory;

/**
 * @author nagel
 *
 */
public interface QSimI extends IOSimulation {

	/**
	 * @param now
	 * @param abstractTransitDriver
	 * @param startLinkId
	 */
	@Deprecated // yyyyyy unclear if this is "actEnd" or "departure"!  kai, may'10
	// depending on this, it is a "PersonAgent" or "DriverAgent".  kai, may'10
	void agentDeparts(double now, PersonDriverAgent driverAgent, Id startLinkId);

	EventsManager getEventsManager();

	AgentCounterI getAgentCounter();

	// yyyyyy imho, should take a PersonAgent as argument.  kai, may'10
	void scheduleActivityEnd(PersonDriverAgent personAgent);

	/**
	 * @param now
	 * @param qPersonAgent
	 */
	@Deprecated // yyyyyy does not make much sense conceptually to have the "Person" in here ... what about
	// automatic drivers?  kai, may'10
	void handleAgentArrival(double now, PersonDriverAgent qPersonAgent);

//	/**
//	 * @return
//	 */
//	@Deprecated // yyyyyy seems to me that one should be able to get this via the config object.  kai, may'10
//	boolean isUseActivityDurations();

	/**
	 * @return
	 */
	Scenario getScenario();

	/**
	 * @param agentFactory
	 */
	void setAgentFactory(AgentFactory agentFactory);

//	/**
//	 * @return
//	 */
//	@Deprecated // this is rarely used, and I find it rather unstable anyways.  kai, jun'10
//	Collection<TransportMode> getNotTeleportedModes();

	/**
	 * @return
	 */
	SimTimerI getSimTimer();

	/**
	 * @return
	 */
	QNetworkI getQNetwork();

//	/**
//	 * @return
//	 */
//	@Deprecated // yyyy should be possible to get this from teh config.  kai, may'10
//	double getStuckTime();

}