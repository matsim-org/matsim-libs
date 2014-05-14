/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PersonExperienceListener.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.scoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;

interface PersonExperienceListener {

    /**
     * Tells this listener about an Activity. The Activity which
     * the agent is in when the simulation starts will have a startTime
     * of Time.UNDEFINED_TIME. The Activity which the agent is in when
     * the simulation ends will have an endTime of Time.UNDEFINED_TIME.
     * It is up to the implementation what to make of this,
     * especially to "wrap" it "around".
     */
    public void handleActivity(Activity activity);

    /**
     * Tells this listener about a Leg. Will contain complete route
     * information for network and transit routes (as you would expect in a Plan), but
     * only a GenericRoute for everything else.
     */
    public void handleLeg(Leg leg);

    /**
     * Tells this listener that the agent got stuck in the simulation and
     * is removed from the simulation. This should usually lead to a high penalty
     * in the score, as the agent was not able to perform its plan as wanted.
     * An agent can get stuck while performing an activity or while driving.
     *
     * @param time the time at which the agent got stuck and was removed from the
     * simulation.
     */
    public void agentStuck(final double time);

    /**
     * Tells this listener about a {@link org.matsim.api.core.v01.events.PersonMoneyEvent}.
     *
     * @param amount the amount of money.
     */
    public void addMoney(final double amount);

    public void handleEvent(Event event);

    /**
     * Tells this listener that no more methods of this interface will be called on it.
     */
    public void finish();

}
