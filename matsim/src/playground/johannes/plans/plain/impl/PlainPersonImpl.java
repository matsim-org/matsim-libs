/* *********************************************************************** *
 * project: org.matsim.*
 * RawPersonImpl.java
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
package playground.johannes.plans.plain.impl;

import java.util.List;

import playground.johannes.plans.ModCount;
import playground.johannes.plans.plain.PlainPerson;
import playground.johannes.plans.plain.PlainPlan;

/**
 * @author illenberger
 *
 */
public class PlainPersonImpl implements PlainPerson, ModCount {

	private List<PlainPlan> plans;
	
	public List<PlainPlan> getPlans() {
		return plans;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.plans.ModCount#getModCount()
	 */
	public long getModCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
