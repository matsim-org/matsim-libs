/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoice.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.frozenepsilons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import java.io.UncheckedIOException;

class ReadOrCreateKVals {

	private static final Logger log = LogManager.getLogger(ReadOrCreateKVals.class);

	private Scenario scenario;
	private ObjectAttributes facilitiesKValues = new ObjectAttributes();
	private ObjectAttributes personsKValues = new ObjectAttributes();
	private RandomFromVarDistr rnd;

	public ReadOrCreateKVals(long seed, Scenario scenario) {
		this.scenario = scenario;
		this.rnd = new RandomFromVarDistr();
		this.rnd.setSeed(seed);
	}

	/*
	 * return 0 if files are read and 1 if k values are created.
	 * This is important to know for reading (case 0) or computation of maxDCScore (case 1)
	 */
	public int run() {
		FrozenTastesConfigGroup dccg = (FrozenTastesConfigGroup) scenario.getConfig().getModule( FrozenTastesConfigGroup.GROUP_NAME );
		String pkValuesFileName = dccg.getpkValuesFile();
		String fkValuesFileName = dccg.getfkValuesFile();
		String maxEpsValuesFileName = dccg.getMaxEpsFile();
		if (existingKValues()) {
			log.info("reading the kvals from the input plans file and facility file");
			return 1;
		}
		log.info("at least one facility kValue or person kValue is missing, start crating all values");
		if (pkValuesFileName != null && fkValuesFileName != null && maxEpsValuesFileName != null) {
			ObjectAttributesXmlReader persKValuesReader = new ObjectAttributesXmlReader(this.personsKValues);
			ObjectAttributesXmlReader facKValuesReader = new ObjectAttributesXmlReader(this.facilitiesKValues);
			try {
				persKValuesReader.readFile(pkValuesFileName);
				facKValuesReader.readFile(fkValuesFileName);
				log.info("reading kvals from files:\n"+ pkValuesFileName + "\n" + fkValuesFileName);
				for (Person p : this.scenario.getPopulation().getPersons().values()) {
					p.getAttributes().putAttribute( "k", personsKValues.getAttribute(p.getId().toString(), "k"));
				}
				for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
					facility.getAttributes().putAttribute("k", facilitiesKValues.getAttribute(facility.getId().toString(), "k"));
				}

				return 0;
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful reading kvals from files!\nThe values are now computed" +
				" and following files are not considered!:\n" + pkValuesFileName + "\n" + fkValuesFileName);
				this.assignKValues();
				return 1;
			}
		}
		else {
			this.assignKValues();
			return 1;
		}
	}

	 private boolean existingKValues() {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Object kAttribute = person.getAttributes().getAttribute("k");
			if (kAttribute == null) {
				return false;
			}
		}
		for (ActivityFacility activityFacility : scenario.getActivityFacilities().getFacilities().values()) {
			Object kAttribute = activityFacility.getAttributes().getAttribute("k");
			if (kAttribute == null) {
				return false;
			}
		}
		return true;
	 }

	 public void assignKValues() {
		log.info("generating kVals");
		this.assignKValuesPersons();
		this.assignKValuesAlternatives();
	}

	// does not matter which distribution is chosen here
	private void assignKValuesPersons() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			p.getAttributes().putAttribute( "k", rnd.getUniform(1.0));
		}
	}
	private void assignKValuesAlternatives() {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			facility.getAttributes().putAttribute("k", rnd.getUniform(1.0));
		}
	}

}
