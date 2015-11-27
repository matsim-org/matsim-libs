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
import org.matsim.core.utils.misc.Time;

/**
 * Default, abstract implementation of the {@link Route}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute implements Route, Cloneable {

	private double dist = Double.NaN;

	private double travTime = Time.UNDEFINED_TIME;

	private Id<Link> startLinkId = null;
	private Id<Link> endLinkId = null;

	public AbstractRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
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
	public final double getTravelTime() {
		return this.travTime;
	}

	@Override
	public final void setTravelTime(final double travTime) {
		this.travTime = travTime;
	}

	@Override
	public void setEndLinkId(final Id<Link> linkId) {
		// overridden in Compressed...
		this.endLinkId = linkId;
	}

	@Override
	public void setStartLinkId(final Id<Link> linkId) {
		// overridden in Compressed...
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
			return (AbstractRoute) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
	
	@Override
	public String toString() {
		String str = "";
		str +=  " startLinkId=" + startLinkId ;
		str += " endLinkId=" + endLinkId ;
		str += " travTime=" + travTime ;
		str += " dist=" + dist ;
		return str ;
	}
	
}
