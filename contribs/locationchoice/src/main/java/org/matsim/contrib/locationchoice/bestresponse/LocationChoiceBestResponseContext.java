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

/**
 * 
 */
package org.matsim.contrib.locationchoice.bestresponse;

import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ComputeKValsAndMaxEpsilon;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author nagel
 *
 */
public class LocationChoiceBestResponseContext {
	
	private final Scenario scenario;
	private ObjectAttributes personsKValues;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsMaxEpsUnscaled;
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private HashSet<String> flexibleTypes;
	private CharyparNagelScoringParameters params;

	public LocationChoiceBestResponseContext(Scenario scenario) {
		this.scenario = scenario;
		
		this.params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore()) ;
		
		ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler(this.scenario.getConfig().locationchoice());
		this.scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
		this.actTypeConverter = defineFlexibleActivities.getConverter() ;
		this.flexibleTypes = defineFlexibleActivities.getFlexibleTypes() ;
		
		this.createObjectAttributes(Long.parseLong(this.scenario.getConfig().locationchoice().getRandomSeed())) ;
	}
	
	private void createObjectAttributes(long seed) {
		this.facilitiesKValues = new ObjectAttributes();
		this.personsKValues = new ObjectAttributes();
		
		String pkValues = this.scenario.getConfig().locationchoice().getpkValuesFile();
		if (!pkValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personsKValues);
			try {
				attributesReader.parse(pkValues);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				this.computeAttributes(seed);
				return ; // ??
			}
		}
		else {
			this.computeAttributes(seed);
			return ; // ??
		}
		String fkValues = this.scenario.getConfig().locationchoice().getfkValuesFile();
		if (!fkValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.facilitiesKValues);
			try {
				attributesReader.parse(fkValues);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				this.computeAttributes(seed);
				return ; // ??
			}
		}
		else {
			this.computeAttributes(seed);
			return ; // ??
		}
		String maxEpsValues = this.scenario.getConfig().locationchoice().getMaxEpsFile();

		if (!maxEpsValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personsMaxEpsUnscaled);
			try {
				attributesReader.parse(maxEpsValues);
			} catch  (UncheckedIOException e) {  // reading was not successful
				this.computeAttributes(seed);
				return ; // ??
			}
		}
		else {
			this.computeAttributes(seed);
			return ; // ??
		}
	}
	
	private void computeAttributes(long seed) {
		ComputeKValsAndMaxEpsilon computer = new ComputeKValsAndMaxEpsilon(
				seed, scenario, this.scaleEpsilon, this.actTypeConverter, this.flexibleTypes);
		computer.assignKValues();
				
		this.personsKValues = computer.getPersonsKValues();
		this.facilitiesKValues = computer.getFacilitiesKValues();
		this.personsMaxEpsUnscaled = computer.getPersonsMaxEpsUnscaled() ;
	}

	
	public Scenario getScenario() {
		return scenario;
	}

	public ObjectAttributes getPersonsKValues() {
		return personsKValues;
	}

	public ObjectAttributes getFacilitiesKValues() {
		return facilitiesKValues;
	}

	public ScaleEpsilon getScaleEpsilon() {
		return scaleEpsilon;
	}

	public ActTypeConverter getConverter() {
		return actTypeConverter;
	}

	public HashSet<String> getFlexibleTypes() {
		return flexibleTypes;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}


}
