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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.AgentFactory;

/**The logic is as follows:<ul>
 * <li>When the agent starts something (an activity, a leg), the agent requests this from the central simulation.</li>
 * <li>When something (an activity, a leg) ends, control is directly passed to the agent.</li>
 *</ul>
 *
 * @author nagel
 *
 */
public interface Mobsim extends IOSimulation, ObservableSimulation {

	void agentDeparts(PersonAgent personAgent, Id startLinkId);

	EventsManager getEventsManager();

	AgentCounterI getAgentCounter();

	/**Method that inserts the agent (back) into the mobsim, where the agent will be at an activity until its scheduled end
	 * (coming from the plan).
	 * 
	 * @param personAgent
	 */
	void scheduleActivityEnd(PersonAgent personAgent);

	/**Method that reschedules the activity end for an agent that is already at an activity.  Necessary for within-day replanning.
	 * 
	 * @param agent
	 * @param oldTime - time when activity end was scheduled previously.  Sometimes necessary so that our book-keeping about
	 * alive agents remains correct. 
	 * @param newTime - time when the activity end is now scheduled.  This is here so that "oldTime" and "newTime" do not get
	 * confused.
	 */
	void rescheduleActivityEnd(final PersonAgent agent, final double oldTime, final double newTime ) ;


	Scenario getScenario();

	void setAgentFactory(AgentFactory agentFactory);

	SimTimerI getSimTimer();

	NetsimNetwork getNetsimNetwork();
	
	Collection<PersonAgent> getActivityEndsList() ;
	
	/**Registering and unregistering agents on links for visualization.  
	 * 
	 * Design decision: In theory, the links could also ask the central
	 * "activities" or "agentTracker" data structures, but in practice this is too slow.  kai, aug'10
	 * 
	 * yyyy not sure if this could not be the more general "registerOnLink" function.  kai, aug'10
	 * 	 */
	void registerAgentAtPtWaitLocation( final PersonAgent agent ) ;
	void unregisterAgentAtPtWaitLocation( final PersonAgent agent ) ;


}