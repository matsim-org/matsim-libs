
/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleQ.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq;

import java.util.Queue;



public interface VehicleQ<E> extends Queue<E> {

	// For transit, which inserts its vehicles "in front of" the queue.
	void addFirst(E previous);

	interface Factory<E> {
		VehicleQ<E> createVehicleQ() ;
	}

}
