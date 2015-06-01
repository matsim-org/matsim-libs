/* *********************************************************************** *
 * project: org.matsim.*
 * RunPersonAttributesExample.java
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
package tutorial.programming.personAttributes;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * Short example to show how one can add custom attributes to a {@link Person}.
 * 
 * @author jwjoubert
 */
public class RunPersonAttributesExample {
	final private static Logger LOG = Logger.getLogger(RunPersonAttributesExample.class);

	/**
	 * Running the example where we show how to add three different attribute
	 * types: {@link Boolean}, {@link Integer} and {@link String}.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("Adding a single person to the population with the following attributes:");
		LOG.info("   |_ gender: male");
		LOG.info("   |_ age: 35");
		LOG.info("   |_ employed: true");
		
		/* Set up the scenario. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sc.getPopulation().getFactory();
		
		/* Create the single person, with id "1". */
		Person person = pf.createPerson(Id.createPersonId("1"));
		
		/* Add custom attributes */
		ObjectAttributes personAttributes = sc.getPopulation().getPersonAttributes();
		personAttributes.putAttribute(person.getId().toString(), "gender", "male");
		personAttributes.putAttribute(person.getId().toString(), "age", 35);
		personAttributes.putAttribute(person.getId().toString(), "employed", true);
		
		/* Report the attributes to the console. */
		LOG.info("Attributes reported from `ObjectAttributes`");
		LOG.info("   |_ gender: " + personAttributes.getAttribute(person.getId().toString(), "gender"));
		LOG.info("   |_ age: " + personAttributes.getAttribute(person.getId().toString(), "age"));
		LOG.info("   |_ employed: " + personAttributes.getAttribute(person.getId().toString(), "employed"));
	}

}
