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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;

public class LinkSwitcher {// TODO more meaningful name for this class [gl April '13]


	private final Network net;

	private final Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();


	private final PhysicalSim2DEnvironment pEnv;

	public LinkSwitcher(Scenario sc, PhysicalSim2DEnvironment pEnv) {
		this.net = sc.getNetwork();
		Sim2DScenario s2dsc = (Sim2DScenario) sc.getScenarioElement(Sim2DScenario.ELEMENT_NAME);
		Sim2DConfig s2dc = s2dsc.getSim2DConfig();
		this.pEnv = pEnv;
	}

	public boolean isSwitchLink(double [] pos, double dx, double dy, Id currentLinkId) {
		final LinkInfo li = getLinkInfo(currentLinkId);
		final double newXPosX = pos[0] + dx;
		final double newXPosY = pos[1] + dy;
		final LineSegment fl = li.finishLine;
		double isLeftOfLine = CGAL.isLeftOfLine(newXPosX, newXPosY, fl.x0, fl.y0, fl.x1, fl.y1);
		return isLeftOfLine >= 0;
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
		
//		if (currentLinkId.toString().equals("l9")){
//			System.out.println("got you!");
//		}
		
		LinkInfo li = new LinkInfo();
		Link l = this.net.getLinks().get(currentLinkId);
		Coord from = l.getFromNode().getCoord();
		Coord to = l.getToNode().getCoord();
		LineSegment seg = new LineSegment();
		seg.x0 = from.getX();
		seg.x1 = to.getX();
		seg.y0 = from.getY();
		seg.y1 = to.getY();

		double dx = seg.x1-seg.x0;
		double dy = seg.y1-seg.y0;
		double length = Math.sqrt(dx*dx+dy*dy);
		dx /= length;
		dy /= length;

		li.link = seg;
		li.dx = dx;
		li.dy = dy;

		Section sec = this.pEnv.getSectionAssociatedWithLinkId(currentLinkId);
		
//		if (pSec == null) {
//			System.out.println();
//		}
		LineSegment fl = null;
		if (sec != null) {
//			fl = getTouchingSegment(seg, sec.getOpeningSegments());
			fl = sec.getOpening(l.getToNode().getId());
		}
		
		
		LineSegment targetLine = null;
		//all polygons are clockwise oriented so we rotate to the right here 
		//TODO find intersections with pSec!! [gl April '13]
		if (fl != null) {
//				fdx /= w;
//				fdy /= w;
//				fdx *= 0.4;
//				fdy *= 0.4;
//				fl.x0 = fl.x0 + fdx;
//				fl.y0 = fl.y0 + fdy;
//				fl.x1 = fl.x1 - fdx;
//				fl.y1 = fl.y1 - fdy;
			

			
			double fdx = fl.dx;
			double fdy = fl.dy;
			
			double dxx = fl.x0 - fl.x1;
			double dyy = fl.y0 - fl.y1;
			double ll = Math.sqrt(dxx*dxx+dyy*dyy);
			
			fdx *= .4 * ll;
			fdy *= .4 * ll;
			
			targetLine = new LineSegment();
			targetLine.x0 = fl.x1 - fdx;
			targetLine.y0 = fl.y1 - fdy;
			targetLine.x1 = fl.x0 + fdx;
			targetLine.y1 = fl.y0 + fdy;	
			targetLine.dx = -fl.dx;
			targetLine.dy = -fl.dy;
			
		} else {
//			//HACK July '13 [gl]
//			double cap = l.getCapacity();
			double width = 5;//(cap)/1.3;
			fl = new LineSegment();
			fl.x0 = seg.x1 - width/2*li.dy;// + rX;
			fl.y0 = seg.y1 + width/2*li.dx;// + rY;
			fl.x1 = seg.x1 + width/2*li.dy;// + rY;
			fl.y1 = seg.y1 - width/2*li.dx;// + rX;
			targetLine = fl;
			targetLine.dx = -li.dy;
			targetLine.dy = li.dx;
		}
		li.finishLine = fl;
		double dx2 = targetLine.x0-targetLine.x1;
		double dy2 = targetLine.y0-targetLine.y1;
		li.width = Math.sqrt(dx2*dx2+dy2*dy2);
		
		li.targetLine = targetLine;
		return li;
	}


	private LineSegment getTouchingSegment(LineSegment seg, List<LineSegment> openings) {

		for (LineSegment opening : openings) {

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
		public LineSegment link;
		LineSegment fromOpening;
		public LineSegment finishLine;
		public LineSegment targetLine;
	}
}
