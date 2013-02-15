/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.LocationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class ReadOrComputeMaxDCScore {	
	private final static Logger log = Logger.getLogger(ReadOrComputeMaxDCScore.class);
	private ScenarioImpl scenario;	
	private Config config;	
	private LocationChoiceBestResponseContext lcContext;
	private ObjectAttributes personsMaxDCScoreUnscaled = new ObjectAttributes();
	
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	
	private DestinationSampler sampler;
	
	private HashSet<String> flexibleTypes;
	
	public ReadOrComputeMaxDCScore(Scenario scenario, ScaleEpsilon scaleEpsilon, 
			ActTypeConverter actTypeConverter, HashSet<String> flexibleTypes, LocationChoiceBestResponseContext lcContext) {
		this.scenario = (ScenarioImpl) scenario;
		this.config = this.scenario.getConfig() ;
		this.scaleEpsilon = scaleEpsilon;
		this.actTypeConverter = actTypeConverter;
		this.flexibleTypes = flexibleTypes;
		this.lcContext = lcContext;
	}
				
	public void readOrCreateMaxDCScore(Controler controler, boolean arekValsRead) {		 		
  		String maxEpsValuesFileName = controler.getConfig().locationchoice().getMaxEpsFile();
		if (!maxEpsValuesFileName.equals("null") && arekValsRead) {			
			ObjectAttributesXmlReader maxEpsReader = new ObjectAttributesXmlReader(this.personsMaxDCScoreUnscaled);
			try {
				maxEpsReader.parse(maxEpsValuesFileName);
				log.info("reading maxEpsilons from file:\n"+ maxEpsValuesFileName);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful reading of maxDCScore from file!\nThe values are now computed" +
				" and following files are not considered!:\n" + maxEpsValuesFileName);
				this.computeMaxDCScore();
			}
		}
		else {
			log.info("computing maxDCScore");
			this.computeMaxDCScore();
		}
	}
	
	private void computeMaxDCScore() {			
		this.sampler = new DestinationSampler(this.lcContext.getPersonsKValues(), this.lcContext.getFacilitiesKValues(), this.config.locationchoice());
				
		log.info("Computing max epsilon ... for " + this.scenario.getPopulation().getPersons().size() + " persons");
		for (String actType : this.scaleEpsilon.getFlexibleTypes()) {
			log.info("Computing max epsilon for activity type " + actType);
			ComputeMaxDCScoreMultiThreatedModule maxEpsilonComputer = new ComputeMaxDCScoreMultiThreatedModule(
					this.scenario, actType, config, this.lcContext, this.sampler);
			maxEpsilonComputer.prepareReplanning(null);
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
				this.personsMaxDCScoreUnscaled.putAttribute(person.getId().toString(), flexibleType, maxType);
				i++;
			}	
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsMaxDCScoreUnscaled);
		attributesWriter.writeFile(this.config.controler().getOutputDirectory() + "personsMaxDCScoreUnscaled.xml");
	}
	

	public ObjectAttributes getPersonsMaxEpsUnscaled() {
		return personsMaxDCScoreUnscaled;
	}
}
