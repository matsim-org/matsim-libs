/* *********************************************************************** *
 * project: org.matsim.*
 * PControler.java
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

package org.matsim.withinday.trafficmanagement.feedbackcontroler;



/**
 * Implementation of a proportional controller with a control signal that s bounded 
 * between -1 and 1
 *
 */
public class PControler implements FeedbackControler {

	// ---------------------- Instance variable (control paramter) ------------------

	private double K;

	/**
	 * Constructor
	 * 
	 * @param K
	 * 				The control parameter
	 */
	public PControler(double K) {
		this.K = K;
	}

	/**
	 * Sets the control parameter
	 * 
	 * @param K
	 * 				The control parameter
	 */
	public void setParameters(double K) {
		this.K = K;
	}

	/**
	 * The control algorithm
	 */
	public double control(double output) {

		double input;

		//		PID calculations

		input = -1 * K * output;

		//		Boundary cuts

		if (input <= 1 && input >= -1) {
			return input;
		} else if (input > 1)
			return input = 1;
		else
			return input = -1;
	}
}
