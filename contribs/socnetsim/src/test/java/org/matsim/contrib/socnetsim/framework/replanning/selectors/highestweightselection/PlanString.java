/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.socnetsim.framework.replanning.selectors.highestweightselection;

import org.matsim.api.core.v01.Id;

final class PlanString {
	public final PlanRecord planRecord;
	public final PlanString tail;
	private final double weight;

	public PlanString(
			final PlanRecord head,
			final PlanString tail) {
		this.planRecord = head;
		this.tail = tail;
		this.weight = head.avgJointPlanWeight + (tail == null ? 0 : tail.getWeight());
	}

	public double getWeight() {
		return weight;
	}

	public boolean containsPerson(final Id id) {
		return planRecord.plan.getPerson().getId().equals( id ) ||
			(tail != null && tail.containsPerson( id ));
	}

	@Override
	public String toString() {
		return "("+planRecord+"; "+tail+")";
	}
}
