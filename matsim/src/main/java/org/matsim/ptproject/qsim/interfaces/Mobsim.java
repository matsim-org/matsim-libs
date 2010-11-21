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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.ptproject.qsim.agents.AgentFactory;

/**The logic is as follows:<ul>
 * <li>When the agent starts something (an activity, a leg), the agent requests this from the central simulation.</li>
 * <li>When something (an activity, a leg) ends, control is directly passed to the agent.</li>
 *</ul>
 *</p>
 *<b>Design thoughts for within day re-planning:</b>
 *<p/>
 *We assume that agents are always in some data structure (at-activity, in-leg with subdivisions waiting-for-pt-vehicle, 
 *waiting-for-entry-into-traffic, etc.).  An agent that wants to re-plan herself thus needs to<ul>
 *<li>know in which state she is (responsibility of agent implementation)
 *<li>be able to re-schedule herself inside data structure (re-schedule departure; wait for other transit vehicle)
 *<li>be able to remove herself from data structure <i> where that makes sense </i> and schedule something else 
 *(an activity, a departure).
 *</li>
 *</ul>
 *There seem to be the following cases:
 *<p/>
 *<table border="1">
  <tr>
    <th>Agent is ...</th>
    <th>can replan ...</th>
    <th>necessary call to Mobsim:</th>
  </tr>
  <tr>
    <td>... at an activity</td>
    <td>... activity end time and everything that follows</td>
    <td>rescheduleActivityEnd()</td>
  </tr>
  <tr>
    <td>... as driver on network link</td>
    <td>... "chooseNextLinkId()" and everything that follows (incl. arrival on following link)
    <br/>(possibility of arrival on current link depends on specifics of the mobsim and should thus be avoided)</td>
    <td>./.</td>
  </tr>
  <tr>
    <td>... as passenger in vehicle</td>
    <td>... stop at which she exits the vehicle</td>
    <td>./.</td>
  </tr>
  <tr>
    <td>... as passenger waiting to enter vehicle</td>
    <td>... vehicle it wants to enter</td>
    <td>./.</td>
  </tr>
  <tr>
    <td>... as passenger waiting to enter vehicle</td>
    <td>... to abort the wait</td>
    <td><b>removeFromWait()</b>, <br/> followed by either "scheduleActivityEnd()" or "agentDeparts()"</td>
  </tr>
  <tr>
    <td>... as driver waiting to enter the traffic</td>
    <td>... to abort the wait</td>
    <td>removeFromWait(), <br/> followed by either "scheduleActivityEnd()" or "agentDeparts()" </td>
  </tr>
</table>
<p/>
 * @author nagel
 * 
 *
 */
public interface Mobsim extends IOSimulation, ObservableSimulation {

	void arrangeAgentDeparture(PlanAgent planAgent);

	EventsManager getEventsManager();

	AgentCounterI getAgentCounter();

	/**Method that inserts the agent (back) into the mobsim, where the agent will be at an activity until its scheduled end
	 * (coming from the plan).
	 * 
	 * @param planAgent
	 */
	void scheduleActivityEnd(PlanAgent planAgent);

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
	
	Collection<PlanAgent> getActivityEndsList() ;
	
	/**Registering and unregistering agents on links for visualization.  
	 * 
	 * Design decision: In theory, the links could also ask the central
	 * "activities" or "agentTracker" data structures, but in practice this is too slow.  kai, aug'10
	 * 
	 * yyyy not sure if this could not be the more general "registerOnLink" function.  kai, aug'10
	 * 	 */
	void registerAgentAtPtWaitLocation( final PlanAgent planAgent ) ;
	void unregisterAgentAtPtWaitLocation( final PlanAgent planAgent ) ;


}