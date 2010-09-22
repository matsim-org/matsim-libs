/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.impl;

import java.util.Iterator;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.mrieser.core.mobsim.api.PlanAgent;

public class DefaultPlanAgent implements PlanAgent {

	private final Plan plan;
	private final Iterator<PlanElement> peIterator;
	private PlanElement currentElement = null;
	private final double weight;

	public DefaultPlanAgent(final Plan plan, final double weight) {
		this.plan = plan;
		this.weight = weight;
		this.peIterator = this.plan.getPlanElements().iterator();
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return this.currentElement;
	}

	@Override
	public Plan getPlan() {
		return this.plan;
	}

	@Override
	public PlanElement useNextPlanElement() {
		if (this.peIterator.hasNext()) {
			this.currentElement = this.peIterator.next();
		} else {
			this.currentElement = null;
		}
		return this.currentElement;
	}

	 @Override
	public double getWeight() {
		return this.weight;
	}

}
