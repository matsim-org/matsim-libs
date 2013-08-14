/* *********************************************************************** *
 * project: org.matsim.*
 * BiPedQ.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.core.mobsim.framework.MobsimTimer;

public class BiPedQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {

	private BiPedQ revReq;

	private final LinkedList<QVehicle> qvis = new LinkedList<QVehicle>();
	private final LinkedList<int []> revQStates = new LinkedList<int []>();//not the cleanest bit of code but this does what I need (modifiable integer)
 	
	private int entered = 0;

	private final MobsimTimer timer;

	private final double delay;

	
	public BiPedQ(MobsimTimer timer, double delay) {
		this.timer = timer;
		this.delay = delay;
	}

	@Override
	public boolean offer(QVehicle arg0) {
		int oncomming = this.revReq.size();
		double delay = computeDelay(oncomming);
		arg0.setEarliestLinkExitTime(delay+arg0.getEarliestLinkExitTime());
		this.qvis.offer(arg0);
		this.revQStates.offer(new int []{this.revReq.getNrTotalAgentsEntered()});
		this.entered++;
		return true;
	}

	/*package*/ int getNrTotalAgentsEntered() {
		return this.entered;
	}
	
	@Override
	public QVehicle peek() {
		QVehicle veh = this.qvis.peek();
		
		if (veh == null) {
			return null;
		}
		
		
		if (veh.getEarliestLinkExitTime() <= this.timer.getTimeOfDay()) {
			int[] revQState = this.revQStates.peek();
			int newAgents = this.revReq.getNrTotalAgentsEntered() - revQState[0];
			double additionalDelay = computeDelay(newAgents);
			veh.setEarliestLinkExitTime(veh.getEarliestLinkExitTime()+additionalDelay);
			revQState[0] = this.revReq.getNrTotalAgentsEntered();
		}
		return veh;
	}

	@Override
	public QVehicle poll() {
		this.revQStates.poll();
		return this.qvis.poll();
	}

	@Override
	public void addFirst(QVehicle previous) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Iterator<QVehicle> iterator() {
		return this.qvis.iterator();
	}

	@Override
	public int size() {
		return this.qvis.size();
	}

	public void setRevQ(BiPedQ q) {
		this.revReq = q;
		
	}
	
	private double computeDelay(int interactions) {
//		if (this.sqr) {
//			return Math.pow(interactions*0.018,2);
//		} else {
			return interactions*this.delay;
//		}
//		return 0;
	}

}