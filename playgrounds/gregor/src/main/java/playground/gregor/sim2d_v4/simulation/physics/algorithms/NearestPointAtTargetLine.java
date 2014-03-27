/* *********************************************************************** *
 * project: org.matsim.*
 * PointAtFinishLine.java
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
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher.LinkInfo;

public class NearestPointAtTargetLine implements DesiredDirectionCalculator {

	private final Sim2DAgent agent;
	private final LinkSwitcher ls;


	public NearestPointAtTargetLine(Sim2DAgent agent, LinkSwitcher ls) {
		this.agent = agent;
		this.ls = ls;
	}

	@Override
	public double[] computeDesiredDirection() {
		final double [] pos = this.agent.getPos();
		Id id = this.agent.getCurrentLinkId();
		LinkInfo li = this.ls.getLinkInfo(id);
		LineSegment fl = li.targetLine;
		double r = CGAL.vectorCoefOfPerpendicularProjection(pos[0], pos[1], fl.x0, fl.y0, fl.x1, fl.y1);
		//if (this.agent.getId().toString().equals("b1")) {
		//	System.out.println("got you");
		//}

		//TODO intersection line/line segment!
		double dx, dy;



		//		if (r >= 0 && r <= 1) {
		//			//			tx = fl.x0 + r * (fl.x1-fl.x0);
		//			//			ty = fl.y0 + r * (fl.y1-fl.y0);
		//			dx = li.dx;
		//			dy = li.dy;
		//		} else 

		if (r <= 0) {
			final double tx = fl.x0;
			final double ty = fl.y0;
			dx = tx - pos[0];
			dy = ty - pos[1];
			final double l = Math.sqrt(dx*dx+dy*dy);
			dx /= l;
			dy /= l;
		} else {
			final double tx = fl.x1;
			final double ty = fl.y1;
			dx = tx - pos[0];
			dy = ty - pos[1];
			final double l = Math.sqrt(dx*dx+dy*dy);
			dx /= l;
			dy /= l;
		}


		//		//for testing only [GL August '13]
		//		Cell c = this.agent.getVoronoiCell();
		//		if (c != null) {
		//			double dcx = c.cx - pos[0];
		//			double dcy = c.cy - pos[1];
		//			if (Double.isNaN(dcx) || Double.isNaN(dcy) || Math.abs(dcx) > 2 || Math.abs(dcy) > 2) {
		//				
		//			} else {
		//				dx += .5*dcx;
		//				dy += .5*dcy;
		////				System.out.println("corrected");
		//			}
		//		}


		return new double []{dx,dy};
	}

}
