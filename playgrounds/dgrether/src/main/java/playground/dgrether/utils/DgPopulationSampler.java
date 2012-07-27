/* *********************************************************************** *
 * project: org.matsim.*
 * DgPopSampler
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
package playground.dgrether.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;


/**
 * @author dgrether
 *
 */
public class DgPopulationSampler {

	private static final Logger log = Logger.getLogger(DgPopulationSampler.class);
	
	public void samplePopulation(Population population, double popSampleSize) {
		log.info("Original population size: " + population.getPersons().size());
		Random random = MatsimRandom.getLocalInstance();
		List<Id> ids = new ArrayList<Id>(population.getPersons().size());
		ids.addAll(population.getPersons().keySet());
		for (Id id : ids){
			double r = random.nextDouble();
			if (r >= popSampleSize){
				population.getPersons().remove(id);
			}
		}
		log.info("Samled population size: " + population.getPersons().size());
	}

}
