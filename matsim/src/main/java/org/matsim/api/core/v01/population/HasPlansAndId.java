
/* *********************************************************************** *
 * project: org.matsim.*
 * HasPlansAndId.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.api.core.v01.population;

import java.util.List;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;

public interface HasPlansAndId<T extends BasicPlan, I> extends Identifiable<I>, Attributable {

	// I added "Attributable".  Does not feel like it keeps the interface minimal.  However, the subpopulation attribute comes from it, which is used
	// rather centrally in the StrategyManager stuff, which is the place for which this interface exists.  Also so far wasn't totally systematic there
	// since it was doing something for persons (subpopulation-based strategy) that it was not offering for others, forcing those who use StrategyManager
	// to implement Attributable seems like an acceptable burden.  kai, may'19

	/**
	 * Seems that <? extends T> is actually more restrictive than <T>, i.e. we may later switch from 
	 * <? extends T> to <T>, but not the other way around.
	 * <p></p>
	 * Practically, with <? extends T>, you cannot do getPlans().add( new MyPlans() ), where MyPlans is derived from T.
	 */
	public abstract List<? extends T> getPlans();

	/**
	 * adds the plan to the Person's List of plans and
	 * sets the reference to this person in the Plan instance.
	 */
	public abstract boolean addPlan(T p);
	
	public abstract boolean removePlan(T p);

	public abstract T getSelectedPlan();

	public abstract void setSelectedPlan(T selectedPlan);

	public abstract T createCopyOfSelectedPlanAndMakeSelected() ;

}
