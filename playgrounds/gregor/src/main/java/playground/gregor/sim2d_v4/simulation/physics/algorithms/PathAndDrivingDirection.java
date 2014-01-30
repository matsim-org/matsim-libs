/* *********************************************************************** *
 * project: org.matsim.*
 * DesiredDirection.java
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

package playground.gregor.sim2d_v4.simulation.physics.algorithms;

import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.math.Math;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher.LinkInfo;

//TODO cite Gloor [gl April '13]
public class PathAndDrivingDirection implements DesiredDirectionCalculator {
	
	
	private final Sim2DAgent agent;
	private final LinkSwitcher ls;

	public PathAndDrivingDirection(Sim2DAgent agent, LinkSwitcher ls) {
		this.agent = agent;
		this.ls = ls;
	}
	
	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirectionCalculator#computeDesiredDirection()
	 */
	@Override
	public double [] computeDesiredDirection() {
		
		final double [] pos = this.agent.getPos();
		final double [] ret = {0,0};
		Id id = this.agent.getCurrentLinkId();
		LinkInfo li = this.ls.getLinkInfo(id);
		final LineSegment link = li.link;
		
		
		double dx = li.dx;
		double dy = li.dy;
		
		double dist = CGAL.signDistPointLine(pos[0],pos[1], link.x0, link.y0, dx, dy);
		
		double px;
		double py;
		if (dist < 0) { //agent is on the left side of the link
			px = dy;
			py = -dx;
		} else { //agent is on the right side of the link
			px = -dy;
			py = dx;
		}

		if (dist < 0) {
			dist = -dist;
		}
		if (dist <1.5){
			dist = 0;
		}
		
		double exp = Math.exp(dist*dist/(li.width));
		
		double w0 = 1 - 1/exp;
		double w1 = 1 - w0;
		
		ret[0] = w1 * dx + w0 * px;
		ret[1] = w1 * dy + w0 * py;
		return ret;
	}

}
