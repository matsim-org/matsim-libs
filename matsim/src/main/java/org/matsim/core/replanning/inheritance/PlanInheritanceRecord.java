package org.matsim.core.replanning.inheritance;

/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPopulationReaderMatsimV6.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

/**
 * Data container storing the data of a single plan.
 * 
 * @author neuma
 */
public class PlanInheritanceRecord {
	
	/**
	 * Id of the person that plan record belongs to.
	 */
	String agentId;
	
	/**
	 * The globally unique plan id.
	 */
	String planId;
	
	/**
	 * Id of the plan that this plan had been copied from before mutating. 
	 */
	String ancestorId;
	
	/**
	 * The name of the strategy that altered this plan.
	 */
	String mutatedBy;
	
	/**
	 * Iteration in which this plan had been created. May be {@linkplain PlanInheritanceModule#INITIAL_PLAN} if the plan had been in the choice-set from the very beginning.
	 */
	int iterationCreated;
	
	/**
	 * Iteration in which the plan had been removed from the choice-set.
	 */
	int iterationRemoved;
	
	/**
	 * Collection of iterations this plan had been the selected plan.
	 * Initialize this with one since each plan is selected at least once. 
	 */
	List<Integer> iterationsSelected = new ArrayList<>(1);

}
