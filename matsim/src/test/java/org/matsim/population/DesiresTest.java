/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTest.java
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

package org.matsim.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author balmermi
 */
public class DesiresTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(DesiresTest.class);

	public void testReadWriteDesires() {
		loadConfig(null);
		log.info("running testReadWriteDesires()... ");

		log.info("  creating single person with desires... ");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop = scenario.getPopulation();
		PersonImpl p = new PersonImpl(Id.create(0, Person.class));
		pop.addPerson(p);
		Desires d = p.createDesires("created by 'DesiresTest.testReadWriteDesires'");
		if (!d.putActivityDuration("home","16:00:00")) throw new RuntimeException("'home' actDur not added to the desires.");
		if (!d.putActivityDuration("work",8*3600)) throw new RuntimeException("'work' actDur not added to the desires.");
		if (!d.removeActivityDuration("home")) throw new RuntimeException("'home' actDur not removed from the desires.");
		if (d.removeActivityDuration("home")) throw new RuntimeException("non extisting 'home' actDur removed from the desires.");
		if (!d.putActivityDuration("home",16*3600)) throw new RuntimeException("'home' actDur not added to the desires.");
		log.info("  done.");

		log.info("  writing population file...");
		new PopulationWriter(pop, scenario.getNetwork()).write(super.getOutputDirectory()+"plans.xml");
		log.info("  done.");

		log.info("  clean up population...");
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		pop = scenario.getPopulation();
		log.info("  done.");

		log.info("  reading in created population file...");
		new MatsimPopulationReader(scenario).readFile(super.getOutputDirectory()+"plans.xml");
		log.info("  done.");

		log.info("  writing population file again...");
		new PopulationWriter(pop, scenario.getNetwork()).write(super.getOutputDirectory()+"plans.equal.xml");
		log.info("  done.");

		log.info("  check for identity ofthe two population...");
		long checksum_ref = CRCChecksum.getCRCFromFile(super.getOutputDirectory()+"plans.xml");
		long checksum_run = CRCChecksum.getCRCFromFile(super.getOutputDirectory()+"plans.equal.xml");
		assertEquals("different population files",checksum_ref,checksum_run);
		log.info("  done.");

		log.info("done.");
	}
}
