/* *********************************************************************** *
 * project: org.matsim.*
 * HRVHOForce.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.hrvohelper.HRVO;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.hrvohelper.Segment;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.geom.Coordinate;

public class HRVOForce implements DynamicForceModule{

	
	private final Set<Agent2D> agents;

	public HRVOForce(PhysicalFloor floor) {
		this.agents = floor.getAgents();
	}
	
	@Override
	public void run(Agent2D agent, double time) {
		
		List<HRVO> hrvos = new ArrayList<HRVO>();
		for (Agent2D other : this.agents) {
			if (other.equals(agent)) {
				continue;
			}
			hrvos.add(getHRVO(agent,other));
		}
		
	}

	private HRVO getHRVO(Agent2D a, Agent2D b) {
		//1. calc collision cones
		double ra = a.getPhysicalAgentRepresentation().getAgentDiameter()/2;
		double rb = b.getPhysicalAgentRepresentation().getAgentDiameter()/2;
		Coordinate[] tan = Algorithms.computeTangentsThroughPoint(b.getPosition(), ra+rb, a.getPosition());
		
		Segment s1 = new Segment();
		Segment s2 = new Segment();
//		Algorithms.computeTangentsThroughPoint(b.getPosition(), ra+rb, a.getPosition(),s1,s2);
		
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forceUpdate() {
		// TODO Auto-generated method stub
		
	}

}
