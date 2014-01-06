/* *********************************************************************** *
 * project: org.matsim.*
 * VDNeighbors.java
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
import java.util.Iterator;
import java.util.List;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.VoronoiCell;
import playground.gregor.sim2d_v4.cgal.VoronoiCenter;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class VDNeighbors implements Neighbors{

	private final Sim2DAgent agent;

	private static final double SQR_CUTOFF_DIST = 10*10.;

	public VDNeighbors(Sim2DAgent agent) {
		this.agent = agent;
	}

	@Override
	public List<Sim2DAgent> getNeighbors() {
		List<Sim2DAgent> ret = new ArrayList<Sim2DAgent>();
		PhysicalSim2DSection psec = this.agent.getPSec();
		VoronoiCell vc = this.agent.getVoronoiCell();
		//TODO fixme
		if (vc == null) {
			return ret;
		}

		Iterator<VoronoiCenter> it = vc.getNeighbors().iterator();
		while (it.hasNext()) {
			Sim2DAgent next = (Sim2DAgent) it.next();
			double dx = next.getPos()[0]-this.agent.getPos()[0];
			double dy = next.getPos()[1]-this.agent.getPos()[1];
			if ( (dx*dx+dy*dy) > SQR_CUTOFF_DIST) {
				continue;
			}
			PhysicalSim2DSection np = next.getPSec();
			if (np == psec || np.getClass() != psec.getClass()) {
				ret.add(next);
			} else {
				LineSegment open = psec.getOpening(next.getPSec());
				if (beelineIntersectsSegment(open,this.agent,next)) {
					ret.add(next);
				}
			}
		}

		//		PhysicalSim2DSectionVoronoiDensity vd = psec.getVD();
		//		Cell cell = vd.getCell(this.agent);
		//		if (cell == null) {
		//			return ret;
		//		}
		//		for (int n : cell.neighbors) {
		//			ret.add(vd.getCell(n).agent);
		//		}

		//		if (this.agent.getId().equals(new IdImpl("b7301"))) {
		//			
		//		}
//				if (this.agent.getId().toString().endsWith("5")){
//					this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new NeighborsEvent(0, this.agent.getId(), ret, this.agent));
//				}

		return ret;
	}

	private boolean beelineIntersectsSegment(LineSegment open,
			Sim2DAgent agent, Sim2DAgent next) {
		//fixme
		if (open == null) {
			return false;
		}

		double left0 = CGAL.isLeftOfLine(open.x0, open.y0, agent.getPos()[0], agent.getPos()[1],next.getPos()[0], next.getPos()[1]);
		double left1 = CGAL.isLeftOfLine(open.x1, open.y1, agent.getPos()[0], agent.getPos()[1],next.getPos()[0], next.getPos()[1]);
		if (left0*left1 <= 0) {
			return true;
		}
		return false;
	}

}
