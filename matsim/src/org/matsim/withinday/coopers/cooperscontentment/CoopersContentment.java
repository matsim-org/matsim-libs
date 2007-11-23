/* *********************************************************************** *
 * project: org.matsim.*
 * CoopersContentment.java
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

package org.matsim.withinday.coopers.cooperscontentment;

import org.matsim.gbl.Gbl;
import org.matsim.withinday.contentment.AgentContentmentI;


/**
 * @author dgrether
 *
 */
public class CoopersContentment implements AgentContentmentI {

	/**
	 * @see org.matsim.withinday.contentment.AgentContentmentI#didReplan(boolean)
	 */
	public void didReplan(final boolean modified) {
		
	}

	/**
	 * @see org.matsim.withinday.contentment.AgentContentmentI#getContentment()
	 */
	public double getContentment() {
		return Gbl.random.nextDouble();
	}

}
