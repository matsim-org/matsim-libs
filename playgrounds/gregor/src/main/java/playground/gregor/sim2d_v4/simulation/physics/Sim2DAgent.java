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

public interface Sim2DAgent {
	
	public QVehicle getQVehicle();

//	public void calcNeighbors(PhysicalSim2DSection physicalSim2DSection);
//
//	public void setObstacles(Segment[] obstacles);

	public void updateVelocity();

	public void setPSec(PhysicalSim2DSection physicalSim2DSection);
	
	public void move(float dx, float dy);

	public float[] getVelocity();

	public Id getCurrentLinkId();

	public float[] getPos();

	public Id chooseNextLinkId();

	public Id getId();

	public void notifyMoveOverNode(Id nextLinkId);
	
	
	


}
