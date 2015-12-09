/* *********************************************************************** *
 * project: org.matsim.*
 * PlanWithCachedJointPlan.java
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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;

/**
 * For performance reasons.
 * Using this class is by no means mandatory.
 *
 * @author thibautd
 */
public final class PlanWithCachedJointPlan implements Plan {
	// should be small, because we use linear search: the idea is to be much faster
	// then a HashMap lookup, which runs in constant time but is relatively slow.
	// For a given state of the plans, there are typically two joint plans to remember:
	// the "weak" and "strong" plans. Hence the size.
	private static final int SIZE = 3;
	
	private Plan delegate ;

	private final JointPlan[] jointPlans = new JointPlan[ SIZE ];
	private final int[] lastAccess = new int[ SIZE ];
	private final int[] keys = new int[ SIZE ];

	private int accessCount = 0;

	public PlanWithCachedJointPlan( final Person person ) {
		delegate = new PlanImpl( person ) ;

		for ( int i=0; i < SIZE; i++ ) {
			lastAccess[ i ] = -1;
			keys[ i ] = Integer.MIN_VALUE;
		}
	}

	public boolean hasCached( final int containerId ) {
		// could not find a better way... The problem is that the fact
		// that the cache returns null does not mean that there is
		// no joint plan attached, but a null is a meaningful value (meaning no
		// joint plan), so searching the HashMap for each null value kills the
		// performance.
		// If this causes (performance) problems, one could cache the index of
		// this id and use it for the next get.
		for ( int i=0; i < SIZE; i++ ) {
			if ( keys[ i ] == containerId ) return true;
		}
		return false;
	}

	public JointPlan getJointPlan(final int containerId ) {
		for ( int i=0; i < SIZE; i++ ) {
			if ( keys[ i ] == containerId ) {
				notifyAccess( i );
				return jointPlans[ i ];
			}
		}
		return null;
	}

	public void setJointPlan(
			final int containerId,
			final JointPlan jp) {
		final int slot = findLeastRecentlyUsedSlot();

		notifyAccess( slot );
		keys[ slot ] = containerId;
		jointPlans[ slot ] = jp;
	}

	private int findLeastRecentlyUsedSlot() {
		int cacheLoc = -1;
		int lastAccessLoc = Integer.MAX_VALUE;

		for ( int i=0; i < SIZE; i++ ) {
			if ( lastAccess[ i ] == -1 ) return i; // free slot
			if ( lastAccess[ i ] < lastAccessLoc ) {
				// was last accessed before the currently chosen loc
				cacheLoc = i;
				lastAccessLoc = lastAccess[ i ];
			}
		}

		return cacheLoc;
	}

	private void notifyAccess( final int slot ) {
		lastAccess[ slot ] = accessCount++;
	}

	// should not be useful, but necessary for the remove method of JointPlans
	// to be well behaved
	public void resetJointPlan( final int containerId ) {
		for ( int i=0; i < SIZE; i++ ) {
			if ( keys[ i ] == containerId ) {
				lastAccess[ i ] = -1;
				jointPlans[ i ] = null;
				return;
			}
		}
	}

	@Override
	public void setScore(Double score) {
		this.delegate.setScore(score);
	}

	@Override
	public Double getScore() {
		return this.delegate.getScore();
	}

	@Override
	public List<PlanElement> getPlanElements() {
		return this.delegate.getPlanElements();
	}

	@Override
	public void addLeg(Leg leg) {
		this.delegate.addLeg(leg);
	}

	@Override
	public void addActivity(Activity act) {
		this.delegate.addActivity(act);
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return this.delegate.getCustomAttributes();
	}

	@Override
	public String getType() {
		return this.delegate.getType();
	}

	@Override
	public void setType(String type) {
		this.delegate.setType(type);
	}

	@Override
	public Person getPerson() {
		return this.delegate.getPerson();
	}

	@Override
	public void setPerson(Person person) {
		this.delegate.setPerson(person);
	}
	
	public final void copyFrom( Plan in) {
		// yy we really need a more systematic way for plans copying.  kai, nov15
		((PlanImpl) this.delegate).copyFrom(in);
	}
	
	
}

