/* *********************************************************************** *
 * project: org.matsim.*
 * ForceUpdater.java
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

package playground.gregor.sim2d_v3.simulation.floor;

import playground.gregor.sim2d_v3.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.ForceModule;

public interface ForceUpdater {
	
	public boolean addForceModule(ForceModule forceModule);
	
	public boolean addDynamicForceModule(DynamicForceModule dynamicForceModule);
	
	public void init();
	
	public void updateForces(double time);
	
	public void updateForces(Agent2D agent, double time);
	
	public void updateDynamicForces(double time);
	
	public void updateDynamicForces();
	
	public void afterSim();
}
