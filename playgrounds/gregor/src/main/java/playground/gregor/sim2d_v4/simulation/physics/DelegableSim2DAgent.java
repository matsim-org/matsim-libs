/* *********************************************************************** *
 * project: org.matsim.*
 * DelegableSim2DAgent.java
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

import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirection;

public interface DelegableSim2DAgent extends Sim2DAgent{

	public void setDesiredDirectionCalculator(DesiredDirection dd);
	
	@Override
	public boolean equals(Object obj);;
}
