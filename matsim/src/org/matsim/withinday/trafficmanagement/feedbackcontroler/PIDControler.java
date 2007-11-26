/* *********************************************************************** *
 * project: org.matsim.*
 * PIDControler.java
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
 * Implementation of a general PID-controller with differntiated data and a
 * control signal that is bounded between -1 and 1.
 * 
 */
public class PIDControler implements FeedbackControler {

	// --------------------------- Instance variables -----------------

	// Control parameters
	private double K;

	private double Ti;

	private double Td;

	double[] states;

	double lastInput;

	/**
	 * Constructor
	 * 
	 * @param K
	 * @param Ti
	 * @param Td
	 *            The control parameters
	 */
	public PIDControler(double K, double Ti, double Td) {
		this.K = K;
		this.Ti = Ti;
		this.Td = Td;
		double[] s = { 0, 0, 0 };
		this.states = s;
		this.lastInput = 0;
	}

	/**
	 * Sets the control parameters
	 * 
	 * @param K
	 * @param Ti
	 * @param Td
	 */
	public void setParameters(double K, double Ti, double Td) {
		this.K = K;
		this.Ti = Ti;
		this.Td = Td;
	}

	/**
	 * The control algorithm
	 * 
	 * @param output
	 *            The system output
	 * 
	 * @return input The control signal
	 */
	public double control(double output) {

		double input;

		// Updates the states

		states[2] = states[1];
		states[1] = states[0];
		states[0] = output;

		// PID calculations

		double dU = -1
				* K
				* ((states[0] - states[1]) + (1 / Ti) * states[0] + Td
						* (states[0] - 2 * states[1] + states[2]));

		// Boundary cuts

		if (lastInput + dU <= 1 && lastInput + dU >= -1) {
			lastInput = lastInput + dU;
			input = lastInput;
		} else if (lastInput + dU > 1)
			input = 1;
		else
			input = -1;

		return input;
	}
}
