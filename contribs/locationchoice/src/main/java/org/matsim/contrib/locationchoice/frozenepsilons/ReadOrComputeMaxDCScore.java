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

package org.matsim.contrib.locationchoice.frozenepsilons;

import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.utils.ScaleEpsilon;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.*;

class ReadOrComputeMaxDCScore {

	private final static Logger log = LogManager.getLogger(ReadOrComputeMaxDCScore.class);

	private Scenario scenario;
	private FrozenTastesConfigGroup dccg;
	private DestinationChoiceContext lcContext;
	private ObjectAttributes personsMaxDCScoreUnscaled = new ObjectAttributes();
	private ScaleEpsilon scaleEpsilon;
	private HashSet<String> flexibleTypes;

	public ReadOrComputeMaxDCScore(DestinationChoiceContext lcContext) {
		this.scenario = lcContext.getScenario();
		this.dccg = (FrozenTastesConfigGroup) scenario.getConfig().getModule( FrozenTastesConfigGroup.GROUP_NAME );
		this.scaleEpsilon = lcContext.getScaleEpsilon();
		this.flexibleTypes = lcContext.getFlexibleTypes();
		this.lcContext = lcContext;
	}

	public void readOrCreateMaxDCScore( boolean arekValsRead ) {
		String maxEpsValuesFileName = this.dccg.getMaxEpsFile();
		if (existingFlexibleTypeValue()) {
			log.info("reading MaxDCScore from plans file");
			return;
		}
		log.info("at least one facility value is missing, start crating all values");
		if (maxEpsValuesFileName != null && arekValsRead) {
			try {
				ObjectAttributesXmlReader maxEpsReader = new ObjectAttributesXmlReader(this.personsMaxDCScoreUnscaled);
				maxEpsReader.readFile(maxEpsValuesFileName);
				for (Person p : this.scenario.getPopulation().getPersons().values()) {
					for (String flexibleType : this.flexibleTypes){
						double maxType = (Double) personsMaxDCScoreUnscaled.getAttribute(p.getId().toString(), flexibleType);
						if (maxType != 0) {
							p.getAttributes().putAttribute(flexibleType, maxType);
						}
					}
				}

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

	private boolean existingFlexibleTypeValue() {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<Activity> activities = TripStructureUtils.getActivities(plan, ExcludeStageActivities);
			for (String flexibleType : this.flexibleTypes) {
				for (Activity activity : activities) {
					if (activity.getType().equals(flexibleType)) {
						if (person.getAttributes().getAttribute(flexibleType) == null) {
							return false;
						}
					}
				}
			}
		}
		return true;
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
				if (maxType != 0.0) {
					person.getAttributes().putAttribute(flexibleType, maxType);
				}
			}
		}
	}

	public ObjectAttributes getPersonsMaxEpsUnscaled() {
		return personsMaxDCScoreUnscaled;
	}
}
