/* *********************************************************************** *
 * project: org.matsim.*
 * Person.java
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
package playground.johannes.plans.view;

import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author illenberger
 *
 */
public interface Person {
	
	public Id getId();
	
	public List<? extends Plan> getPlans();
	
	public void addPlan(Plan plan);
	
	public void removePlan(Plan plan);

	public Plan getSelectedPlan();
	
	public void setSelectedPlan(Plan plan);
	
}
