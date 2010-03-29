/* *********************************************************************** *
 * project: org.matsim.*
 * PAgent
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package soc.ai.matsim.dbsim;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;


/**
 * Provides methods of an agent that is not driving.
 * TODO Should be called PersonAgent after everybody was asked
 * @author dgrether
 *
 */
public interface PersonAgentI {

	/**
	 * The time the agent wants to depart from an Activity. If the agent is currently driving,
	 * the return value cannot be interpreted (e.g. it is not defined if it is the departure time
	 * from the previous activity, or from the next one).
	 *
	 * @return the time when the agent wants to depart from an activity.
	 */
	public double getDepartureTime();
	/* there is no corresponding setter, as the implementation should set the the corresponding time
	 * internally, e.g. in legEnds().
	 */

	/**
	 * Informs the agent that the currently executed activity is ended / is
	 * no longer performed.
	 * 
	 * @param now
	 */
	public void activityEnds(final double now);
	
	/**
	 * Informs the agent that it arrived at the destination of the current leg.
	 * The agent can then decide if he wants to start an activity, or continue
	 * on another leg.
	 *
	 * @param now the current time in the simulation
	 */
	public void legEnds(final double now);

	public Leg getCurrentLeg();

	public Person getPerson();

	
}
