/* *********************************************************************** *
 * project: org.matsim.*
 * ConstantControler.java
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
 * Implementation of a controller that controls at a constant pre-defined rate between
 * -1 and 1
 *
 */
public class ConstantControler implements FeedbackControler {

	//	----------------------- Instance variable ------------------------------

	private double input;

	/**
	 * Constructor
	 * 
	 * @param input
	 * 					The constant control signal
	 */
	public ConstantControler(double input) {
		this.input = input;
	}

	/**
	 * The method that actually controls
	 */
	public double control(double output) {
		return input;
	}
}
