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
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;

import playground.gregor.sim2d_v4.events.debug.NeighborsEvent;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.PhysicalSim2DSectionVoronoiDensity.Cell;

public class VDNeighbors {

	private final Sim2DAgent agent;

	public VDNeighbors(Sim2DAgent agent) {
		this.agent = agent;
	}

	public List<Sim2DAgent> getNeighbors() {
		List<Sim2DAgent> ret = new ArrayList<Sim2DAgent>();
		PhysicalSim2DSection psec = this.agent.getPSec();
		PhysicalSim2DSectionVoronoiDensity vd = psec.getVD();
		Cell cell = vd.getCell(this.agent);
		if (cell == null) {
			return ret;
		}
		for (int n : cell.neighbors) {
			ret.add(vd.getCell(n).agent);
		}

		//		if (this.agent.getId().equals(new IdImpl("b7301"))) {
		//			
		//		}
		if (this.agent.getId().equals(new IdImpl("b6575"))) {
			this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new NeighborsEvent(0, this.agent.getId(), ret, this.agent));
		}

		return ret;
	}
}
