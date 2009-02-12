/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.jdeqsim;

import org.matsim.gbl.Gbl;

public class Scheduler {
	private double simTime = 0;
	private MessageQueue queue = new MessageQueue();
	private double simulationStartTime = System.currentTimeMillis();
	private double hourlyLogTime = 3600;

	public void schedule(Message m) {
		queue.putMessage(m);
	}

	public void unschedule(Message m) {
		queue.removeMessage(m);
	}

	public void startSimulation() {
		Message m;
		while (!queue.isEmpty() && simTime < SimulationParameters.getSimulationEndTime()) {
			m = queue.getNextMessage();
			if (m != null) {
				simTime = m.getMessageArrivalTime();
				m.processEvent();
				m.handleMessage();
			}
			printLog();
		}
	}

	public double getSimTime() {
		return simTime;
	}

	private void printLog() {

		// print output each hour
		if (simTime / hourlyLogTime > 1) {
			hourlyLogTime = simTime + 3600;
			System.out.print("Simulation at " + simTime / 3600 + "[h]; ");
			System.out.println("s/r:" + simTime / (System.currentTimeMillis() - simulationStartTime) * 1000);
			Gbl.printMemoryUsage();
		}
	}

}
