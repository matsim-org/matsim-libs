/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPersonAlgorithm.java
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

package org.matsim.core.population.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Counter;

public abstract class AbstractPersonAlgorithm implements PersonAlgorithm {
	// this is ok as non-final since methods that contain code are final. kai, may'17

	private final static Logger log = LogManager.getLogger(AbstractPersonAlgorithm.class);

	public final void run(final Population plans) {
		log.info("running " + this.getClass().getName() + " algorithm...");
		Counter counter = new Counter(" person # ");

		for (Person p : plans.getPersons().values()) {
			counter.incCounter();
			this.run(p);
		}
		counter.printCounter();
		log.info("done running algorithm.");
	}

	@Override
	public abstract void run(Person person);
}
