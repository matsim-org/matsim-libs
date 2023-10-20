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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**
 * Data container storing the data of a single plan.
 * 
 * @author neuma, alex94263
 */
public class PlanInheritanceRecord {
	
	/**
	 * Id of the person that plan record belongs to.
	 */
	private Id<Person> agentId;
	
	/**
	 * The globally unique plan id.
	 */
	private Id<Plan> planId;
	
	/**
	 * Id of the plan that this plan had been copied from before mutating. 
	 */
	private Id<Plan> ancestorId;
	
	/**
	 * The name of the strategy that altered this plan.
	 */
	private String mutatedBy;
	
	/**
	 * Iteration in which this plan had been created. May be {@linkplain PlanInheritanceModule#INITIAL_PLAN} if the plan had been in the choice-set from the very beginning.
	 */
	private int iterationCreated;
	
	/**
	 * Iteration in which the plan had been removed from the choice-set.
	 */
	private int iterationRemoved = -1;
	
	/**
	 * Collection of iterations this plan had been the selected plan.
	 * Initialize this with one since each plan is selected at least once. 
	 */
	private List<Integer> iterationsSelected = new ArrayList<>(1);

	public Id<Plan> getPlanId() {
		return planId;
	}

	public void setPlanId(Id<Plan> planId) {
		this.planId = planId;
	}

	public Id<Plan> getAncestorId() {
		return ancestorId;
	}

	public void setAncestorId(Id<Plan> ancestorId) {
		this.ancestorId = ancestorId;
	}

	public String getMutatedBy() {
		return mutatedBy;
	}

	public void setMutatedBy(String mutatedBy) {
		this.mutatedBy = mutatedBy;
	}

	public int getIterationCreated() {
		return iterationCreated;
	}

	public void setIterationCreated(int iterationCreated) {
		this.iterationCreated = iterationCreated;
	}

	public int getIterationRemoved() {
		return iterationRemoved;
	}

	public void setIterationRemoved(int iterationRemoved) {
		this.iterationRemoved = iterationRemoved;
	}

	public List<Integer> getIterationsSelected() {
		return iterationsSelected;
	}

	public void setIterationsSelected(List<Integer> iterationsSelected) {
		this.iterationsSelected = iterationsSelected;
	}

	public Id<Person> getAgentId() {
		return agentId;
	}

	public void setAgentId(Id<Person> agentId) {
		this.agentId = agentId;
	}

}
