/* *********************************************************************** *
 * project: org.matsim.*
 * CANetworkEntity.java
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

public interface CANetworkEntity {

	public void handleEvent(CAEvent e);

	public void lock();

	public void unlock();

	public boolean tryLock();

	public boolean isLocked();

	public int threadNR();

	public double getX();

	public double getY();

	public int getNrLanes();
	
}
