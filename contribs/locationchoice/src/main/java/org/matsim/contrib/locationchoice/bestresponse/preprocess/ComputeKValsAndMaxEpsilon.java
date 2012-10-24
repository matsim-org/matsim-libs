/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.locationchoice.bestresponse.preprocess;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.RandomFromVarDistr;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class ComputeKValsAndMaxEpsilon {	
	private final static Logger log = Logger.getLogger(ComputeKValsAndMaxEpsilon.class);
	private ScenarioImpl scenario;	
	private Config config;	
	private RandomFromVarDistr rnd;
	
	private ObjectAttributes facilitiesKValues = new ObjectAttributes();
	private ObjectAttributes personsKValues = new ObjectAttributes();
	private ObjectAttributes personsMaxEpsUnscaled = new ObjectAttributes();
	
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	
	private DestinationSampler sampler;
	
	private HashSet<String> flexibleTypes;
	
	public ComputeKValsAndMaxEpsilon(long seed, ScenarioImpl scenario, Config config, 
			ScaleEpsilon scaleEpsilon, ActTypeConverter actTypeConverter, HashSet<String> flexibleTypes) {
		rnd = new RandomFromVarDistr();
		rnd.setSeed(seed);
		this.scenario = scenario;
		this.config = config;
		this.scaleEpsilon = scaleEpsilon;
		this.actTypeConverter = actTypeConverter;
		this.flexibleTypes = flexibleTypes;
	}
	
	public void assignKValues() {				
		this.assignKValuesPersons();
		this.assignKValuesAlternatives();	
		this.sampler = new DestinationSampler(this.personsKValues, this.facilitiesKValues, this.config.locationchoice());
	}
		
	// does not matter which distribution is chosen here
	private void assignKValuesPersons() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			personsKValues.putAttribute(p.getId().toString(), "k", rnd.getUniform(1.0));
		}
		// write person k values
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsKValues);
		attributesWriter.writeFile(config.controler().getOutputDirectory() + "personsKValues.xml");
	}	
	private void assignKValuesAlternatives() {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			facilitiesKValues.putAttribute(facility.getId().toString(), "k", rnd.getUniform(1.0));
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.facilitiesKValues);
		attributesWriter.writeFile(config.controler().getOutputDirectory() + "facilitiesKValues.xml");
	}
	
	public void run() {			
		log.info("Assigning k values ...");				
		this.assignKValues(); 
				
		log.info("Computing max epsilon ... for " + this.scenario.getPopulation().getPersons().size() + " persons");
		for (String actType : this.scaleEpsilon.getFlexibleTypes()) {
			log.info("Computing max epsilon for activity type " + actType);
			ComputeMaxEpsilons maxEpsilonComputer = new ComputeMaxEpsilons(
					this.scenario, actType, config, this.facilitiesKValues, this.personsKValues, 
					this.scaleEpsilon, this.actTypeConverter, this.sampler);
			maxEpsilonComputer.prepareReplanning();
			for (Person p : this.scenario.getPopulation().getPersons().values()) {
				maxEpsilonComputer.handlePlan(p.getSelectedPlan());
			}
			maxEpsilonComputer.finishReplanning();
		}		
		this.writeMaxEps();
	}
	
	private void writeMaxEps() {
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			int i = 0;
			for (String flexibleType : this.flexibleTypes) {
				double maxType = Double.parseDouble(((PersonImpl)person).getDesires().getDesc().split("_")[i]);
				this.personsMaxEpsUnscaled.putAttribute(person.getId().toString(), flexibleType, maxType);
				i++;
			}	
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsMaxEpsUnscaled);
		attributesWriter.writeFile(this.config.controler().getOutputDirectory() + "personsMaxEpsUnscaled.xml");
	}

	public ObjectAttributes getFacilitiesKValues() {
		return facilitiesKValues;
	}
	public void setFacilitiesKValues(ObjectAttributes facilitiesKValues) {
		this.facilitiesKValues = facilitiesKValues;
	}
	public ObjectAttributes getPersonsKValues() {
		return personsKValues;
	}
	public void setPersonsKValues(ObjectAttributes personsKValues) {
		this.personsKValues = personsKValues;
	}
	public ObjectAttributes getPersonsMaxEpsUnscaled() {
		return personsMaxEpsUnscaled;
	}
	public void setPersonsMaxEpsUnscaled(ObjectAttributes personsMaxEpsUnscaled) {
		this.personsMaxEpsUnscaled = personsMaxEpsUnscaled;
	}
}
