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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrCreateKVals;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author nagel
 *
 */
public class DestinationChoiceBestResponseContext implements MatsimToplevelContainer {	
	private final Scenario scenario;
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private HashSet<String> flexibleTypes;
	private CharyparNagelScoringParameters params;
	private static final Logger log = Logger.getLogger(DestinationChoiceBestResponseContext.class);
	private int arekValsRead = 1;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
	private ObjectAttributes personsBetas = new ObjectAttributes();
	private ObjectAttributes facilitiesAttributes = new ObjectAttributes();

	public DestinationChoiceBestResponseContext(Scenario scenario) {
		this.scenario = scenario;	
		log.info("dc context created but not yet initialized");
		//this.init(); // actually wanted to leave this away to be able to create but not yet fill the context.
	}
	
	public void init() {
		this.params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore()) ;		
		ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler(this.scenario.getConfig().locationchoice());
		this.scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
		this.actTypeConverter = defineFlexibleActivities.getConverter() ;
		this.flexibleTypes = defineFlexibleActivities.getFlexibleTypes() ;
		
		this.readOrCreateKVals(Long.parseLong(this.scenario.getConfig().locationchoice().getRandomSeed()));
		this.readFacilitesAttributesAndBetas();
		log.info("dc context initialized");
	}
	
	private void readOrCreateKVals(long seed) {
		ReadOrCreateKVals computer = new ReadOrCreateKVals(seed, (ScenarioImpl) this.scenario);
		this.arekValsRead = computer.run();
		this.personsKValues = computer.getPersonsKValues();
		this.facilitiesKValues = computer.getFacilitiesKValues();
	}
	
	private void readFacilitesAttributesAndBetas() {
		String pBetasFileName = this.scenario.getConfig().locationchoice().getpBetasFile();
		String fAttributesFileName = this.scenario.getConfig().locationchoice().getfAttributesFile();
		if (!pBetasFileName.equals("null") && !fAttributesFileName.equals("null")) {			
			ObjectAttributesXmlReader personsBetasReader = new ObjectAttributesXmlReader(this.personsBetas);
			ObjectAttributesXmlReader facilitiesAttributesReader = new ObjectAttributesXmlReader(this.facilitiesAttributes);
			try {
				personsBetasReader.parse(pBetasFileName);
				facilitiesAttributesReader.parse(fAttributesFileName);
				log.info("reading betas and facilities attributes from: \n"+ pBetasFileName + "\n" + fAttributesFileName);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful betas and facilities attributes from files!\n" + pBetasFileName + "\n" + fAttributesFileName);
			}
		}
	}
		
	public Scenario getScenario() {
		return scenario;
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

	public boolean kValsAreRead() {
		return (this.arekValsRead == 0);
	}

	public ObjectAttributes getPersonsKValues() {
		return personsKValues;
	}
	
	public ObjectAttributes getFacilitiesKValues() {
		return facilitiesKValues;
	}

	public ObjectAttributes getPersonsBetas() {
		return personsBetas;
	}

	public ObjectAttributes getFacilitiesAttributes() {
		return facilitiesAttributes;
	}

	@Override
	public MatsimFactory getFactory() {
		return null;
	}
}
