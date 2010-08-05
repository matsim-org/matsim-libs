/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatXPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.mfeil;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;


/**
 * @author Matthias Feil
 * Extends standard Plan object to enable sorting lists of Plans following their scores.
 * Use 
 * java.util.Arrays.sort (nameOfArray[]) 
 * or
 * java.util.Collections.sort (nameOfList<>).
 * See e.g., PlanomatX class.
 */
public class PlanomatXPlan extends PlanImpl implements Comparable<PlanomatXPlan>{

	public PlanomatXPlan (Person person){
		super(person);
	}
	
	public final int compareTo(PlanomatXPlan p){
		return this.getScore().compareTo(p.getScore());
	}
	
	public void setActsLegs (List<? extends PlanElement> actslegs){
		List planElements = getPlanElements();
		planElements.clear();
		planElements.addAll(actslegs);
	}
}
