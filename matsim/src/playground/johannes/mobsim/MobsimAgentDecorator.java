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

import org.matsim.network.Link;
import org.matsim.utils.identifiers.IdI;

/**
 * @author illenberger
 *
 */
public abstract class MobsimAgentDecorator<A extends MobsimAgent> implements MobsimAgent {
	
	private A agent;
	
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

	public String getNextMode(double time) {
		return agent.getNextMode(time);
	}

	public Link getNextLink(double time) {
		return agent.getNextLink(time);
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
	
	public IdI getId() {
		return agent.getId();
	}
}
