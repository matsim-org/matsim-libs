/* *********************************************************************** *
 * project: org.matsim.*
 * BangBangControler.java
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

package org.matsim.withinday.trafficmanagement.controltheorycontroler;


/**
 * Implementation of a bang-bang controller with a control signal that is
 * bounded between -1 and 1.
 * 
 */
public class BangBangControler implements FeedbackControler {

	/**
	 * The control algorithm
	 * 
	 * @param output
	 * 
	 * @return input
	 */
	public double control(double output) {
		if (output > 0)
			return -1;
		else if (output < 0)
			return 1;
		else
			return 0;
	}
}
