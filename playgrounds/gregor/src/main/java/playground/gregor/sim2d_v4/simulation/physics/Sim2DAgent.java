/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DAgent.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import playground.gregor.sim2d_v4.debugger.VisDebugger;

public interface Sim2DAgent {
	
	public abstract QVehicle getQVehicle();

//	public void calcNeighbors(PhysicalSim2DSection physicalSim2DSection);
//
//	public void setObstacles(Segment[] obstacles);

	public abstract void updateVelocity();

	public abstract void setPSec(PhysicalSim2DSection physicalSim2DSection);
	
	public abstract PhysicalSim2DSection getPSec();
	
	public abstract void move(float dx, float dy);

	public abstract float[] getVelocity();

	public abstract Id getCurrentLinkId();

	public abstract float[] getPos();

	public abstract Id chooseNextLinkId();

	public abstract Id getId();

	public abstract void notifyMoveOverNode(Id nextLinkId);

	public abstract void debug(VisDebugger visDebugger);
	

}
