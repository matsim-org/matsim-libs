/* *********************************************************************** *
 * project: org.matsim.*
 * CALink.java
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

package playground.gregor.casim.simulation.physics;

import org.matsim.api.core.v01.network.Link;

public interface CALink extends CANetworkEntity{

	@Override
	public abstract void handleEvent(CAEvent e);

	public abstract CANode getUpstreamCANode();

	public abstract CANode getDownstreamCANode();

	public abstract Link getLink();

	public abstract Link getUpstreamLink();

	public abstract int getNumOfCells();
	
	public abstract CAMoveableEntity[] getParticles();

	public abstract double[] getTimes();
	
	public abstract void fireDownstreamEntered(CAMoveableEntity a, double time);
	
	public abstract void fireUpstreamEntered(CAMoveableEntity a, double time);

	public abstract void fireDownstreamLeft(CAMoveableEntity a, double time);
	
	public abstract void fireUpstreamLeft(CAMoveableEntity a, double time);
	
	public abstract void reset();

	public abstract void letAgentDepart(CAVehicle veh);

}