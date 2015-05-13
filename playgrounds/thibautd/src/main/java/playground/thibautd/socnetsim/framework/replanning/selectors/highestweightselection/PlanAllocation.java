/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAllocation.java
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
package playground.thibautd.socnetsim.framework.replanning.selectors.highestweightselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author thibautd
 */
final class PlanAllocation {
	private final List<PlanRecord> plans = new ArrayList<PlanRecord>();
	private final List<PlanRecord> immutable = Collections.unmodifiableList( plans );
	private double weight = 0;

	public List<PlanRecord> getPlans() {
		return immutable;
	}

	public double getWeight() {
		return weight;
	}

	public void add(final PlanRecord p) {
		plans.add( p );
		weight += p.avgJointPlanWeight;
	}

	public void addAll(final Collection<PlanRecord> ps) {
		for ( PlanRecord p : ps ) add( p );
	}

	public void remove(final PlanRecord p) {
		plans.remove( p );
		weight -= p.avgJointPlanWeight;
	}

	public void removeAll(final Collection<PlanRecord> ps) {
		for ( PlanRecord p : ps ) remove( p );
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof PlanAllocation && ((PlanAllocation) o).plans.equals( plans );
	}

	@Override
	public int hashCode() {
		return plans.hashCode();
	}

	@Override
	public String toString() {
		return "{PlanAllocation: plans="+plans+"}";
	}
}

