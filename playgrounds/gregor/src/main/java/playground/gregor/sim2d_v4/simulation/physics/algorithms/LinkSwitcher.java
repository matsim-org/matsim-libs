/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSwitcher.java
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;

public class LinkSwitcher {// TODO more meaningful name for this class [gl April '13]

	
	private final Network net;
	
	private final Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();

	private final double offsetX;
	private final double offsetY;

	private final PhysicalSim2DEnvironment pEnv;

	public LinkSwitcher(Scenario sc, PhysicalSim2DEnvironment pEnv) {
		this.net = sc.getNetwork();
		Sim2DScenario s2dsc = sc.getScenarioElement(Sim2DScenario.class);
		Sim2DConfig s2dc = s2dsc.getSim2DConfig();
		this.offsetX = s2dc.getOffsetX();
		this.offsetY = s2dc.getOffsetY();
		this.pEnv = pEnv;
	}
	
	public boolean isSwitchLink(double [] pos, double dx, double dy, Id currentLinkId) {
		final LinkInfo li = getLinkInfo(currentLinkId);
		final double newXPosX = pos[0] + dx;
		final double newXPosY = pos[1] + dy;
		final Segment fl = li.finishLine;
		double isLeftOfLine = CGAL.isLeftOfLine(newXPosX, newXPosY, fl.x0, fl.y0, fl.x1, fl.y1);
		return isLeftOfLine > 0;
	}

	public LinkInfo getLinkInfo(Id currentLinkId) {
		LinkInfo li = this.linkInfos.get(currentLinkId);
		if (li == null) {
			li = createLinkInfo(currentLinkId);
			this.linkInfos.put(currentLinkId, li);
		}
		return li;
	}

	private LinkInfo createLinkInfo(Id currentLinkId) {
		LinkInfo li = new LinkInfo();
		Link l = this.net.getLinks().get(currentLinkId);
		Coord from = l.getFromNode().getCoord();
		Coord to = l.getToNode().getCoord();
		Segment seg = new Segment();
		seg.x0 = from.getX()-this.offsetX;
		seg.x1 = to.getX()-this.offsetX;
		seg.y0 = from.getY()-this.offsetY;
		seg.y1 = to.getY()-this.offsetY;

		double dx = seg.x1-seg.x0;
		double dy = seg.y1-seg.y0;
		double length = Math.sqrt(dx*dx+dy*dy);
		dx /= length;
		dy /= length;

		li.link = seg;
		li.dx = dx;
		li.dy = dy;
		
		PhysicalSim2DSection pSec = this.pEnv.getPhysicalSim2DSectionAssociatedWithLinkId(currentLinkId);
		if (pSec == null) {
			System.out.println(pSec);
		}
		Segment fl = getTouchingSegment(seg, pSec.getOpenings());
		if (fl == null) {
			fl = new Segment();
			fl.x0 = seg.x1;
			fl.y0 = seg.y1;

			//all polygons are clockwise oriented so we rotate to the right here 
			fl.x1 = fl.x0 + li.dy;
			fl.y1 = fl.y0 - li.dx;
		}
		li.finishLine = fl;
		li.width = 10; //TODO section width [gl Jan'13];
		return li;
	}
	
	
	private Segment getTouchingSegment(Segment seg, Segment[] openings) {

		for (Segment opening : openings) {

			boolean onSegment = CGAL.isOnVector(seg.x1, seg.y1,opening.x0, opening.y0, opening.x1, opening.y1); // this is a necessary but not sufficient condition since a section is only approx convex. so we need the following test as well
			if (onSegment) {
				double isLeft0  = CGAL.isLeftOfLine(opening.x0, opening.y0,seg.x0, seg.y0, seg.x1, seg.y1);
				double isLeft1  = CGAL.isLeftOfLine(opening.x1, opening.y1,seg.x0, seg.y0, seg.x1, seg.y1);
				if (isLeft0 * isLeft1 >= 0){ // coordinate is on a vector given by the segment opening but not on the segment itself. This case is unlikely to occur! 
					onSegment = false;
				}
				if (onSegment) {
					return opening;
				}
			}
		}
		return null;
	}
	
	public static final class LinkInfo {

		public double width;
		public double dx;
		public double dy;
		public Segment link;
		Segment fromOpening;
		public Segment finishLine;
	}
}
