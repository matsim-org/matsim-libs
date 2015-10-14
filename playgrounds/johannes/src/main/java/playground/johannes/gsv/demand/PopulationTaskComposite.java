/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.demand;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.collections.Composite;

/**
 * @author johannes
 *
 */
public class PopulationTaskComposite extends Composite<PopulationTask> implements PopulationTask {

	private static final Logger logger = Logger.getLogger(PopulationTaskComposite.class);
	@Override
	public void apply(Population pop) {
		for(PopulationTask task : components) {
			logger.debug(String.format("Executing task %s", task.getClass().getName()));
			task.apply(pop);
		}
	}

}
