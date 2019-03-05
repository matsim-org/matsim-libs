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

package org.matsim.contrib.locationchoice.bestresponse;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.utils.RandomFromVarDistr;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

 class ReadOrCreateKVals {
	
	private static final Logger log = Logger.getLogger(ReadOrCreateKVals.class);
	
	public static String fkValuesFile = "facilitiesKValues.xml";
	public static String pkValuesFile = "personsKValues.xml";
	
	private Scenario scenario;	
	private ObjectAttributes facilitiesKValues = new ObjectAttributes();
	private ObjectAttributes personsKValues = new ObjectAttributes();
	private Config config;	
	private RandomFromVarDistr rnd;
	
	public ReadOrCreateKVals(long seed, Scenario scenario) {
		this.scenario = scenario;
		this.config = scenario.getConfig();
		this.rnd = new RandomFromVarDistr();
		this.rnd.setSeed(seed);
	}
	
	/*
	 * return 0 if files are read and 1 if k values are created. 
	 * This is important to know for reading (case 0) or computation of maxDCScore (case 1)
	 */
	public int run() {
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		String pkValuesFileName = dccg.getpkValuesFile();
		String fkValuesFileName = dccg.getfkValuesFile();
		String maxEpsValuesFileName = dccg.getMaxEpsFile();
		if (pkValuesFileName != null && fkValuesFileName != null && maxEpsValuesFileName != null) {			
			ObjectAttributesXmlReader persKValuesReader = new ObjectAttributesXmlReader(this.personsKValues);
			ObjectAttributesXmlReader facKValuesReader = new ObjectAttributesXmlReader(this.facilitiesKValues);
			try {
				persKValuesReader.readFile(pkValuesFileName);
				facKValuesReader.readFile(fkValuesFileName);
				log.info("reading kvals from files:\n"+ pkValuesFileName + "\n" + fkValuesFileName);
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
		
	public void assignKValues() {
		log.info("generating kVals");
		this.assignKValuesPersons();
		this.assignKValuesAlternatives();
	}
		
	// does not matter which distribution is chosen here
	private void assignKValuesPersons() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			this.personsKValues.putAttribute(p.getId().toString(), "k", rnd.getUniform(1.0));
		}
		// write person k values
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsKValues);
		attributesWriter.writeFile(config.controler().getOutputDirectory() + pkValuesFile);
	}	
	private void assignKValuesAlternatives() {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			this.facilitiesKValues.putAttribute(facility.getId().toString(), "k", rnd.getUniform(1.0));
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.facilitiesKValues);
		attributesWriter.writeFile(config.controler().getOutputDirectory() + fkValuesFile);
	}
	
	public ObjectAttributes getFacilitiesKValues() {
		return this.facilitiesKValues;
	}
	
	public ObjectAttributes getPersonsKValues() {
		return this.personsKValues;
	}
}
