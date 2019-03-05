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
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class ReadOrComputeMaxDCScore {

	private final static Logger log = Logger.getLogger(ReadOrComputeMaxDCScore.class);

	public static String maxEpsFile = "personsMaxDCScoreUnscaled.xml";

	private Config config;
	private Scenario scenario;
	private DestinationChoiceConfigGroup dccg;
	private DestinationChoiceContext lcContext;
	private ObjectAttributes personsMaxDCScoreUnscaled = new ObjectAttributes();
	private ScaleEpsilon scaleEpsilon;
	private HashSet<String> flexibleTypes;

	public ReadOrComputeMaxDCScore(DestinationChoiceContext lcContext) {
		this.scenario = lcContext.getScenario();
		this.config = this.scenario.getConfig();
		this.dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.scaleEpsilon = lcContext.getScaleEpsilon();
		this.flexibleTypes = lcContext.getFlexibleTypes();
		this.lcContext = lcContext;
	}

	public void readOrCreateMaxDCScore( boolean arekValsRead ) {
		String maxEpsValuesFileName = this.dccg.getMaxEpsFile();
		if (maxEpsValuesFileName != null && arekValsRead) {
			ObjectAttributesXmlReader maxEpsReader = new ObjectAttributesXmlReader(this.personsMaxDCScoreUnscaled);
			try {
				maxEpsReader.readFile(maxEpsValuesFileName);
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
		DestinationSampler sampler = new DestinationSampler(this.lcContext.getPersonsKValuesArray(),
			  this.lcContext.getFacilitiesKValuesArray(),
			  this.dccg);

		log.info("Computing max epsilon ... for " + this.scenario.getPopulation().getPersons().size() + " persons");
		for (String actType : this.scaleEpsilon.getFlexibleTypes()) {
			log.info("Computing max epsilon for activity type " + actType);
			ComputeMaxDCScoreMultiThreatedModule maxEpsilonComputer = new ComputeMaxDCScoreMultiThreatedModule(
				  actType, this.lcContext, sampler);
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
			for (String flexibleType : this.flexibleTypes) {
				double maxType = (Double)person.getCustomAttributes().get(flexibleType);
				this.personsMaxDCScoreUnscaled.putAttribute(person.getId().toString(), flexibleType, maxType);
			}
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.personsMaxDCScoreUnscaled);
		attributesWriter.writeFile(this.config.controler().getOutputDirectory() + maxEpsFile);
	}

	public ObjectAttributes getPersonsMaxEpsUnscaled() {
		return personsMaxDCScoreUnscaled;
	}
}
