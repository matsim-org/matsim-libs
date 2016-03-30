/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public final class FIFOVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle>  {
	
	private final LinkedList<QVehicle> vehicleQueue = new LinkedList<>();

	@Override
	public boolean offer(QVehicle e) {
		return vehicleQueue.offer(e);
	}

	@Override
	public QVehicle peek() {
		return vehicleQueue.peek();
	}

	@Override
	public QVehicle poll() {
		return vehicleQueue.poll();
	}

	@Override
	public Iterator<QVehicle> iterator() {
		return vehicleQueue.iterator();
	}

	@Override
	public int size() {
		return vehicleQueue.size();
	}

	@Override
	public void addFirst(QVehicle e) {
		vehicleQueue.addFirst(e);
	}

}
