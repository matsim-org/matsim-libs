/* *********************************************************************** *
 * project: org.matsim.*
 * FeedbackControler.java
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
 * Interface that defines an automatic feedback controller with a control signal that 
 * is bounded between -1 and 1.
 * 
 * @author dgrether
 * 
 */
public interface FeedbackControler {
	/**
	 * The control algorithm that transforms the output to an input
	 * 
	 * @param output
	 * 
	 * @return input
	 */
	public double control(double output);

}
