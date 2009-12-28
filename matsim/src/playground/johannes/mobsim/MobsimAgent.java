/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimAgent.java
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

package playground.johannes.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

/**
 * @author illenberger
 * 
 */
public interface MobsimAgent {

	/**
	 * Returns the desired transportation mode of the next trip, or the mode of
	 * the current trip if the agent is en-route.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the mode of the next trip, or <tt>null</tt> if there is no next
	 *         trip.
	 */
	public TransportMode getMode(double time);

	/**
	 * Returns the desired departure time of the next trip.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the departure time of the next trip, or {@link Double#MAX_VALUE}
	 *         if there is no next trip.
	 */
	public double getDepartureTime(double time);

	/**
	 * Returns the current link. If the agent is currently not en-route, the
	 * link of the current activity performed is returned.
	 * 
	 * @return the current link.
	 */
	public Link getLink();

	/**
	 * Returns the next desired link the agent wishes to traverse.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the next desired link, or <tt>null</tt> if the agent has
	 *         reached its destination link, i.e., {@link #getLink()} equals
	 *         {@link #getDestinationLink(double)}.
	 */
	public Link getNextLink(double time);

	/**
	 * Returns the desired destination link. If {@link #isDone()} returns
	 * <tt>true</tt> the desired destination link is the current link, e.g.,
	 * {@link #getLink()} equals {@link #getDestinationLink(double)}.
	 * 
	 * @param time
	 *            the current simulation time.
	 * @return the desired destination link.
	 */
	public Link getDestinationLink(double time);

	/**
	 * Returns whether the agent intends to perform a further trip or not.
	 * 
	 * @return <tt>true</tt> if the agent does not intend to perform a further
	 *         trip, otherwise <tt>false</tt>.
	 */
	public boolean isDone();

	/**
	 * Initializes the agent to its initial state.
	 */
	public void beforeSim();

	/**
	 * Informs the agent that it has departed.
	 * 
	 * @param time
	 *            the current simulation time.
	 */
	public void departure(double time);

	/**
	 * Informs the agent that it has been set to a new link. <tt>link</tt>
	 * must be consistent to {@link #getNextLink(double)}, otherwise the
	 * behavior is undefined.
	 * 
	 * @param link
	 *            the link the agent has been set to.
	 * @param time
	 *            the current simulation time.
	 */
	public void enterLink(Link link, double time);

	/**
	 * Informs the agent that it has arrived.
	 * 
	 * @param time
	 *            the current simulation time.
	 */
	public void arrival(double time);

	/**
	 * Returns a unique identifier.
	 * 
	 * @return the agent's id.
	 */
	public Id getId();
}
