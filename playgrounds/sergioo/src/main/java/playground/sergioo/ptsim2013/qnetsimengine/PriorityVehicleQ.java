
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
package playground.sergioo.ptsim2013.qnetsimengine;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.PriorityQueue;

public class PriorityVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle>  {
	
	private PriorityQueue<QVehicle> vehicleQueue = new PriorityQueue<QVehicle>(5, new QVehicleEarliestLinkExitTimeComparator());

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
		if(vehicleQueue.peek()!=null)
			e.setEarliestLinkExitTime(vehicleQueue.peek().getEarliestLinkExitTime()-1);
		vehicleQueue.add(e);
	}

}
