/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DSectionConnector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.scenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v4.cgal.LineSegment;

import com.vividsolutions.jts.geom.Coordinate;

@Deprecated //TODO this should have happened as a preprocessing step and stored in the xml file
public abstract class Sim2DSectionPreprocessor {

	public static void preprocessSections(Sim2DEnvironment env) {
		for (Section sec : env.getSections().values() ) {
			genLineSegments(sec);
		}
		
		for (Section sec : env.getSections().values() ) {
			connectToNeighbors(sec,env);
		}		
	}
	
	public static void genLineSegments(Section sec) {
		Coordinate[] coords = sec.getPolygon().getCoordinates();
		int[] openings = sec.getOpenings();
		int oidx = 0;

		List<LineSegment> obst = new ArrayList<LineSegment>();
		List<LineSegment> open = new ArrayList<LineSegment>();

		for (int i = 0; i < coords.length-1; i++){
			Coordinate c0 = coords[i];
			Coordinate c1 = coords[i+1];
			LineSegment seg = new LineSegment();
			seg.x0 = c0.x;
			seg.x1 = c1.x;
			seg.y0 = c0.y;
			seg.y1 = c1.y;

			double dx = seg.x1-seg.x0;
			double dy = seg.y1-seg.y0;
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			seg.dx = dx;
			seg.dy = dy;

			if (oidx < openings.length && i == openings[oidx]) {
				oidx++;
				open.add(seg);
			} else {
				obst.add(seg);
			}
		}
		
		sec.setObstacles(obst);
		sec.setOpenings(open);
	}

	private static void connectToNeighbors(Section sec, Sim2DEnvironment env) {
		for (LineSegment opening : sec.getOpeningSegments()) {
			for (Id<Section> n : sec.getNeighbors()) {
				Section nSec = env.getSections().get(n);
				if (isConnectedViaOpening(opening,nSec)){
					sec.addOpeningNeighborMapping(opening, nSec);
					break;
				}
			}
		}
	}

	private static boolean isConnectedViaOpening(LineSegment opening, Section nSec) {
		for (LineSegment nOpening : nSec.getOpeningSegments()) {
			if (nOpening.equalInverse(opening)) {
				return true;
			}
		}
		return false;
	}
}
