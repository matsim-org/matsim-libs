/* *********************************************************************** *
 * project: org.matsim.*
 * FreightPopulationSampler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.population.freight;

import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.HouseholdSampler;
import playground.southafrica.utilities.Header;

/**
 * Since the commercial vehicle population is only addressed in a population
 * and population attribute file, we cannot use the {@link HouseholdSampler}.
 * This class samples a user-specified fraction of commercial vehicles.
 * 
 * @author jwjoubert
 */
public class FreightPopulationSampler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FreightPopulationSampler.class.toString(), args);
		
		String inputPopulation = args[0];
		String inputAttributes = args[1];
		double fraction = Double.parseDouble(args[2]);
		long seed = Long.parseLong(args[3]);
		String outputPopulation = args[4];
		String outputAttributes = args[5];
		
		Scenario inSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(inSc).parse(inputPopulation);
		new ObjectAttributesXmlReader(inSc.getPopulation().getPersonAttributes()).parse(inputAttributes);
		
		Scenario outSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimRandom.reset(seed);

		ObjectAttributes inAttr = inSc.getPopulation().getPersonAttributes();
		ObjectAttributes outAttr = outSc.getPopulation().getPersonAttributes();
		Iterator<? extends Person> iterator = inSc.getPopulation().getPersons().values().iterator();
		while(outSc.getPopulation().getPersons().size() < fraction*inSc.getPopulation().getPersons().size() &&
				iterator.hasNext()){
			double r = MatsimRandom.getLocalInstance().nextDouble();
			Person p = iterator.next();
			if(r <= fraction){
				/* Add the person. */
				outSc.getPopulation().addPerson(p);
				
				/* Add all its associated attributes. */
				outAttr.putAttribute(p.getId().toString(), "subpopulation", inAttr.getAttribute(p.getId().toString(), "subpopulation"));
			}
		}
		
		/* Write the output to file. */
		new PopulationWriter(outSc.getPopulation()).write(outputPopulation);
		new ObjectAttributesXmlWriter(outAttr).writeFile(outputAttributes);
		
		Header.printFooter();
	}

}
