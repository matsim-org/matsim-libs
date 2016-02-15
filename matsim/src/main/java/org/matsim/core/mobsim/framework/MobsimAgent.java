/* *********************************************************************** *
 * project: matsim
 * PlanAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

/**
 * Minimal interface for an agent that can do activities and legs.  For the legs, there is the minimal information to
 * perform teleportation, but nothing else.
 * </p>
 * Design decisions:<UL>
 * <li> Note that the plain mobsim agent (= this interface) does not know anything about plans.
 * </ul>
 *
 * @author dgrether
 * @author nagel
 */
public interface MobsimAgent extends NetworkAgent, Identifiable<Person> {

    public enum State {
        ACTIVITY, LEG, ABORT
    }

    /**
     * The method through which the agent announces state changes.  For example, when a leg ends, the agent is "in limbo".  When it sets its
     * state to "ACTIVITY", then when returning to the main engine, that engine will ask the agent about its state and then insert it into
     * the correct queue.
     * <p/>
     * Design comments/questions:<ul>
     * <li> Should this be renamed into "get<i>CurrentOrNext</i>State"?
     * </ul>
     */
    State getState();

    /**
     * The time the agent wants to depart from an Activity. If the agent is currently driving,
     * the return value cannot be interpreted (e.g. it is not defined if it is the departure time
     * from the previous activity, or from the next one).
     * <p/>
     * There is no corresponding setter, as the implementation should set the corresponding time internally, e.g. in endLegAndComputeNextState() .
     *
     * @return the time when the agent wants to depart from an activity.
     */
    public double getActivityEndTime();

    /**
     * Informs the agent that the activity has ended.  The agent is responsible for what comes next.
     *
     * @param now
     */
    public void endActivityAndComputeNextState(final double now);

    /**
     * Informs the agent that the leg has ended.  The agent is responsible for what comes next.
     *
     * @param now the current time in the simulation
     */
    public void endLegAndComputeNextState(final double now);

    /**
     * This is another method besides endLeg... and endActivity... .  Seems to be necessary to abort agents
     * in states where they should not be.  They should then either set their internal state to abort, or try to recover
     * if possible.  If neither is done, an infinite loop may result.  kai, feb'12
     * <p/>
     * With respect to "recovery": Possible states (may'14) are LEG and ACTIVITY.  I cannot say what the consistency requirements here are
     * (e.g. if the agent can only start an activity on the link from where the abort is called).
     */
    public void setStateToAbort(final double now);

    /**
     * This returns the expected travel time of a leg that was just started.  There is no crystal-clear design requirement for this;
     * it is probably used by the TeleportationEngine to obtain the duration of the leg; it is probably ignored by all other modes.
     * One can probably return "null" if one wants an exception when this is passed to the teleportation engine, or "1"(sec) if
     * one wants some default behavior.
     */
    public Double getExpectedTravelTime();

    /**
     * This returns the expected travel distance of a leg that was just started.
     */
    public Double getExpectedTravelDistance();

    /**
     * Design thoughts:<ul>
     * <li>There needs to be some method that tells the agent that a teleportation has happened, similar to "moveOverNode".
     * Could be separated out to a "teleportation" agent, but can as well leave it here. Also used by transit and by
     * taxicabs.  kai, nov'10
     * </ul>
     */
    public void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId);

    public Facility<? extends Facility<?>> getCurrentFacility() ;
    
    public Facility<? extends Facility<?>> getDestinationFacility() ;
}
