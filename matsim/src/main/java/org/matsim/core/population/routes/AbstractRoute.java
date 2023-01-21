/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.population.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Default, abstract implementation of the {@link Route}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute implements Route, Cloneable {
	// This has a public non-final non-empty method, which is "clone".  But in the end this is how it is designed.
	// So we leave it as is; if we ever want to re-design it in the core, we will have to copy it and start
	// from the copy.  kai, may'17

	protected static final double UNDEFINED_TIME = Double.NEGATIVE_INFINITY;

	private boolean locked = false ;

	private double dist = Double.NaN;

	protected double travTime = UNDEFINED_TIME;

	private Id<Link> startLinkId = null;
	private Id<Link> endLinkId = null;

	public AbstractRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
	}

	protected static OptionalTime asOptionalTime(double seconds) {
		return seconds == UNDEFINED_TIME ? OptionalTime.undefined() : OptionalTime.defined(seconds);
	}

	@Override
	public final double getDistance() {
		return dist;
	}

	@Override
	public final void setDistance(final double dist) {
		this.dist = dist;
	}

	@Override
	public final OptionalTime getTravelTime() {
		return asOptionalTime(this.travTime);
	}

	@Override
	public final void setTravelTime(final double travTime) {
		OptionalTime.assertDefined(travTime);
		this.travTime = travTime;
	}

	@Override
	public void setTravelTimeUndefined() {
		this.travTime = UNDEFINED_TIME;
	}

	@Override
	public final void setEndLinkId(final Id<Link> linkId) {
		testForLocked();
		this.endLinkId = linkId;
	}

	@Override
	public final void setStartLinkId(final Id<Link> linkId) {
		testForLocked();
		this.startLinkId = linkId;
	}

	@Override
	public final Id<Link> getStartLinkId() {
		return this.startLinkId;
	}

	@Override
	public final Id<Link> getEndLinkId() {
		return this.endLinkId;
	}
	
	public final void setLocked() {
		locked = true ;
	}

	@Override
	public AbstractRoute clone() {
		// "clone" is some automagic that, by itself, makes a copy of the "bit pattern" of the object.  That is:
		// * the content of primitive types is copied
		// * for objects the reference to the objects is copied.
		// Consequences for matsim:
		// * primitive types can be changed on the copy without affecting the original
		// * the references to objects can be changed on the copy without affecting the original.  For example, a copy can
		//    point to other links or nodes or persons or IDs.
		// * One has to be careful with objects where the contents can be changed AND they are not shared between the 
		//    original and the copy.  For example, changing the income of the person is not a problem since it is the same
		//    for two plans pointing to the same person.  In contrast (and potentially quite dangerous): the contents of 
		//    Customizable (currently not applicable for Route) would have to be explicitly deepcopied.
		// The method can only be called if a class implements "Cloneable"; otherwise, it leads to a runtime exception (!).
		// It is, however, sufficient to have clone available as protected.
		try {
			final AbstractRoute clone = (AbstractRoute) super.clone();
			clone.locked = false ; // not obvious that this is the right way to go.
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
	
	@Override
	public String toString() {
		String str = "";
		str +=  " startLinkId=" + startLinkId ;
		str += " endLinkId=" + endLinkId ;
		str += " travTime=" + travTime;
		str += " dist=" + dist ;
		return str ;
	}
	
	@SuppressWarnings("unused")
	private void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "Route is locked; too late to do this.  See comments in code.") ;
		}
	}

}
