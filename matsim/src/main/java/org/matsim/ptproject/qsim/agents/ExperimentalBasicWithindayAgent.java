/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalBasicWithindayPersonDriverAgent.java
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
package org.matsim.ptproject.qsim.agents;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

/**
 * @author nagel
 *
 */
public class ExperimentalBasicWithindayAgent extends DefaultPersonDriverAgent {

	public ExperimentalBasicWithindayAgent(Person p, Mobsim simulation) {
		super(p, simulation);
	}

	public final List<PlanElement> getModifiablePlanElements() {
		return this.person.getSelectedPlan().getPlanElements() ;
	}

	public final Integer getCurrentPlanElementIndex() {
		return this.currentPlanElementIndex ;
	}

	public final Integer getCurrentRouteLinkIdIndex() {
		return this.currentLinkIdIndex ;
	}
	
	protected void setCachedNextLinkId(Id cachedNextLinkId) {
		// yyyy I am not convinced that this method really makes sense.  Needed for DgWithinday...  .  kai, oct'10
		
		this.cachedNextLinkId = cachedNextLinkId;
	}

	protected Id getCachedNextLinkId() {
		return this.cachedNextLinkId;
	}
	
	@Override
	public final void calculateDepartureTime( Activity act ) {
		super.calculateDepartureTime( act ) ;
	}



}
