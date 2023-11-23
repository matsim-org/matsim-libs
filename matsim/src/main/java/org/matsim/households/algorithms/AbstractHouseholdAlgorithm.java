/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractHouseholdAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.households.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;

public abstract class AbstractHouseholdAlgorithm implements HouseholdAlgorithm{

	private final static Logger log = LogManager.getLogger(AbstractHouseholdAlgorithm.class);
	
	public final void run(final Households households) {
		log.info("Running " + this.getClass().getName() + " algorithm...");
		Counter counter = new Counter(" household # ");
		
		for(Household h : households.getHouseholds().values()) {
			this.run(h);
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Done running algorithm.");
	}
	
	
}

