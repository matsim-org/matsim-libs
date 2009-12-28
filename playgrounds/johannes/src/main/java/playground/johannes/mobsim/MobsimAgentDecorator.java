/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimAgentDecorator.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

/**
 * A base-class for writing decorators of {@link MobsimAgent}.
 * 
 * @param A
 *            The concrete implementation of {@link MobsimAgent} that will be
 *            decorated.
 * 
 * @author illenberger
 * 
 */
public abstract class MobsimAgentDecorator<A extends MobsimAgent> implements
		MobsimAgent {

	/**
	 * The decorated MobsimAgent instance.
	 */
	protected final A agent;

	/**
	 * Creates a new decorator delegating all methods of {@link MobsimAgent} to
	 * <tt>agent</tt>.
	 * 
	 * @param agent
	 *            the agent to decorate.
	 */
	public MobsimAgentDecorator(A agent) {
		this.agent = agent;
	}

	public void beforeSim() {
		agent.beforeSim();
	}

	public Link getLink() {
		return agent.getLink();
	}

	public double getDepartureTime(double time) {
		return agent.getDepartureTime(time);
	}

	public TransportMode getMode(double time) {
		return agent.getMode(time);
	}

	public Link getNextLink(double time) {
		return agent.getNextLink(time);
	}

	public Link getDestinationLink(double time) {
		return agent.getDestinationLink(time);
	}

	public boolean isDone() {
		return agent.isDone();
	}

	public void arrival(double time) {
		agent.arrival(time);
	}

	public void departure(double time) {
		agent.departure(time);
	}

	public void enterLink(Link link, double time) {
		agent.enterLink(link, time);
	}

	public Id getId() {
		return agent.getId();
	}
}
