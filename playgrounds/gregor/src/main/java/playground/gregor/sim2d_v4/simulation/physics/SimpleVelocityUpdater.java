/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleVelocityUpdater.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;

import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher.LinkInfo;


public class SimpleVelocityUpdater implements VelocityUpdater {

	private final Sim2DAgent agent;
	private final LinkSwitcher ls;

	private final double v0 = 1.34;
	
	public SimpleVelocityUpdater(Sim2DAgent agent, LinkSwitcher ls, Scenario sc) {
		this.agent = agent;
		this.ls = ls;
		
	}
	
	@Override
	public void updateVelocity() {
		Id id = this.agent.getCurrentLinkId();
		LinkInfo li = this.ls.getLinkInfo(id);
		double[] v = this.agent.getVelocity();
		v[0] = li.dx * this.v0;
		v[1] = li.dy * this.v0;
	}
	

}
