/* *********************************************************************** *
 * project: org.matsim.*
 * CopyOfBasicPerson.java
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

package playground.david;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPlanImpl;

public class CopyOfBasicPerson<P extends BasicPlanImpl> {
	
	private final int id; // unchangeable
	private final ArrayList<P> plans = new ArrayList<P>();
	protected BasicPlan selectedPlan = null;
	
	public CopyOfBasicPerson(int id) {
		this.id = id;
	}
	
	public void addPlan(P plan) {
		plans.add(plan);
		// Make sure there is a selected plan if there is at least one plan
		if (selectedPlan == null) selectedPlan = plan;
	}
	
	public BasicPlan getSelectedPlan() {
		return selectedPlan;
	}
	
	public List<? extends BasicPlanImpl> plans() {
		return plans;
	}
	
	public int getId() {
		return id;
	}
	


}
