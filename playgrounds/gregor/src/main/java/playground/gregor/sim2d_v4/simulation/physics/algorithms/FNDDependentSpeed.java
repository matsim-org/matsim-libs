/* *********************************************************************** *
 * project: org.matsim.*
 * FNDDependentSpeed.java
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

import java.util.List;

import playground.gregor.sim2d_v4.cgal.VoronoiCell;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class FNDDependentSpeed implements SpaceDependentSpeed {

	@Override
	public double computeSpaceDependentSpeed(Sim2DAgent agent,
			List<Sim2DAgent> neighbors) {
		
		VoronoiCell cell = agent.getVoronoiCell();
		if (cell != null && cell.isClosed()) {
			double v = 1.34 *(1-Math.exp(-1.913*(cell.getArea()-1/5.4)));
			agent.setDesiredSpeed(v);
		} else {
			agent.setDesiredSpeed(1.34);
		}
		
		return 0;
	}

}
