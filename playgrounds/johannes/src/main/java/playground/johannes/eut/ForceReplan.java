/* *********************************************************************** *
 * project: org.matsim.*
 * ForceReplan.java
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

/**
 * 
 */
package playground.johannes.eut;

import org.matsim.withinday.contentment.AgentContentment;

/**
 * @author illenberger
 *
 */
public class ForceReplan implements AgentContentment {

	/* (non-Javadoc)
	 * @see org.matsim.withinday.contentment.AgentContentment#didReplan(boolean)
	 */
	public void didReplan(boolean modified) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.matsim.withinday.contentment.AgentContentment#getContentment()
	 */
	public double getContentment(final double time) {
		return -1;
	}

}
