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
	
	public abstract CAAgent[] getParticles();

	public abstract double[] getTimes();
	
	public abstract void fireDownstreamEntered(CAAgent a, double time);
	
	public abstract void fireUpstreamEntered(CAAgent a, double time);

	public abstract void fireDownstreamLeft(CAAgent a, double time);
	
	public abstract void fireUpstreamLeft(CAAgent a, double time);
	
	//MATSim integration
	public abstract void letAgentDepart(CAVehicle veh);

	public abstract void reset();

}