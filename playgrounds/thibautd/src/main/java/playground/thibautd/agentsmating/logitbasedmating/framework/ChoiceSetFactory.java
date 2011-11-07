/* *********************************************************************** *
 * project: org.matsim.*
 * ChoiceSetFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author thibautd
 */
public interface ChoiceSetFactory extends MatsimFactory {
	/**
	 * Creates alternatives to choose from for the given leg.
	 * Alternatives for car pooling MUST be TripRequest instances.
	 *
	 * @param decisionMaker the decision maker
	 * @param plan the Plan concerned by the choice
	 * @param indexOfLeg the index in the plan, for the leg to choose the mode for
	 *
	 * @return a list of possible alternatives. 
	 */
	public List<Alternative> createChoiceSet(DecisionMaker decisionMaker, Plan plan, int indexOfleg);
}

