/* *********************************************************************** *
 * project: org.matsim.*
 * PersonalSpace.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.VoronoiCell;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import be.humphreys.simplevoronoi.GraphEdge;

public class PerceivedPersonalSpaceApproach implements SpaceDependentSpeed {

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v4.simulation.physics.algorithms.SpaceDependentSpeed#computePersonalSpace(playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent)
	 */
	@Override
	public double computeSpaceDependentSpeed(Sim2DAgent agent, List<Sim2DAgent> neighbors) {

		List<LineSegment> obs = agent.getPSec().getObstacleSegments();

		VoronoiCell vc = agent.getVoronoiCell();
		if (vc == null) {
			return Double.POSITIVE_INFINITY;
		}
		double area = 0;
		double x = agent.getPos()[0];
		double y = agent.getPos()[1];
		Map<LineSegment,GraphEdge> open = new HashMap<LineSegment,GraphEdge>(obs.size());
		List<GraphEdge> closed = new ArrayList<GraphEdge>();
		for (GraphEdge ed : vc.getGraphEdges()) {

//			intersection(ed,obs,open,closed);
			double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
			double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x, y, ed.x1, ed.y1) < 0 ? -1 : 1;
			contr *= leftOf;
			area += contr;

		}

		double minSqrDist = Double.POSITIVE_INFINITY;
		double orthoX = x + agent.getVelocity()[1];
		double orthoY = y - agent.getVelocity()[0];
		for (Sim2DAgent n : neighbors) {
			if (CGAL.isLeftOfLine(n.getPos()[0], n.getPos()[1], x, y, orthoX, orthoY) > 0) {
				double dx = Math.abs(n.getPos()[0] - x)-n.getRadius();
				double dy = Math.abs(n.getPos()[1] - y)-n.getRadius();
				double sqrDist = dx*dx + dy*dy;
				if (sqrDist < minSqrDist) {
					minSqrDist = sqrDist;
				}
			}
		}


		return Math.min(area, minSqrDist*Math.PI);
	}

	private void intersection(GraphEdge ed, List<LineSegment> obs,
			Map<LineSegment, GraphEdge> open, List<GraphEdge> closed) {

		double dxu = ed.x2 - ed.x1;
		double dyu = ed.y2 - ed.y1;
		for (LineSegment o : obs) {
			double lft0 = CGAL.isLeftOfLine(ed.x1, ed.y1, o.x0, o.y0, o.x1, o.y1);
			double lft1 = CGAL.isLeftOfLine(ed.x2, ed.y2, o.x0, o.y0, o.x1, o.y1);

			if (lft0 > 0 && lft1 > 0) {
				ed.x1 = 0; ed.x2 = 0; ed.y1 =0; ed.y2 = 0; //invalid edge (outside boundary)
				//				throw new RuntimeException("")
			}

			if (lft0 * lft1 < 0) {
				double dxv = o.x1 - o.x0;
				double dyv = o.y1 - o.y0;
				double dxw = ed.x1 - o.x0;
				double dyw = ed.y1 - o.y0;
				double d = CGAL.perpDot(dxu, dyu, dxv, dyv);
				double c = CGAL.perpDot(dxv, dyv, dxw, dyw)/d;
				if (c < 0 || c > 1) {
					continue;
				}
				double cc = CGAL.perpDot(dxu, dyu, dxw, dyw)/d;
				if (cc < 0 || cc > 1) {
					continue;
				}

				double xx = ed.x1 + dxu * c;
				double yy = ed.y1 + dyu * c;
			}
		}
	}
}


