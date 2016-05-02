/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;

public class GfipVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {

	public GfipVehicleQ() {} /* Just to find calls. */
	private final Queue<QVehicle> delegate = new PriorityQueue<QVehicle>(11, new Comparator<QVehicle>() {

		@Override
		public int compare(QVehicle arg0, QVehicle arg1) {
			// TODO Auto-generated method stub
			return 0;
		}
	});
	
	@Override
	public boolean offer(QVehicle arg0) {
		return delegate.offer(arg0);
	}

	@Override
	public QVehicle peek() {
		return delegate.peek();
	}

	@Override
	public QVehicle poll() {
		return delegate.poll();
	}

	@Override
	public Iterator<QVehicle> iterator() {
		return delegate.iterator();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public void addFirst(QVehicle vehicle) {
		vehicle.setEarliestLinkExitTime(Double.NEGATIVE_INFINITY);
		this.add(vehicle);
	}

}
