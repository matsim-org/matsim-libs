/* *********************************************************************** *
 * project: org.matsim.*
 * PlanModRunnable.java
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
package playground.johannes.coopsim.mental.planmod.concurrent;

import java.util.Map;

import org.matsim.api.core.v01.population.Plan;

import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.coopsim.mental.planmod.PlanModifier;

/**
 * @author illenberger
 *
 */
class PlanModRunnable implements Runnable {

	private final Map<String, Object> choices;

	private final Plan plan;
	
	private Choice2ModAdaptor adaptor;
	
	PlanModRunnable(Map<String, Object> choices, Plan plan) {
		this.choices = choices;
		this.plan = plan;
	}
	
	void setAdaptor(Choice2ModAdaptor adaptor) {
		this.adaptor = adaptor;
	}
	
	@Override
	public void run() {
		PlanModifier mod = adaptor.convert(choices);
		mod.apply(plan);
	}

}
