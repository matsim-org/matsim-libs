/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim.util;

/**
 * A simple timer implementation.
 *
 * @author rashid_waraich
 */
public class Timer {
	private long startTime = 0;
	private long endTime = 0;

	public void startTimer() {
		startTime = System.currentTimeMillis();
	}

	public void endTimer() {
		endTime = System.currentTimeMillis();
	}

	public void resetTimer() {
		startTime = 0;
		endTime = 0;
	}

	public long getMeasuredTime() {
		return endTime - startTime;
	}

	public void printMeasuredTime(String label) {
		System.out.println(label + getMeasuredTime());
	}
}
