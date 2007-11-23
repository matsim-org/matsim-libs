/* *********************************************************************** *
 * project: org.matsim.*
 * AgentContentmentI.java
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

package org.matsim.withinday.contentment;

/**
 * An instance of AgentContentmentI is an object that models the agent's
 * contentment. The contentment is represented by scalar value between -1 and 1,
 * while -1 means the agent is displeased, 0 means the agent is indifferent and
 * 1 means the agent is content.
 * 
 * @author illenberger
 * 
 */
public interface AgentContentmentI {

	/**
	 * Retruns a value that represents the agent's current contentment where
	 * positive values denote the agent is pleased and negative values denote
	 * the agent is displeased. Values should be in the range of -1 to 1.
	 * 
	 * @return A value representing the agent's current contentment.
	 */
	public double getContentment();

	/**
	 * Tells the module that the agent performed a replan.
	 * 
	 * @param modified
	 *            <tt>true</tt> if the plan was modified upon replanning,
	 *            <tt>false</tt> otherwise.
	 */
	public void didReplan(boolean modified);

}
