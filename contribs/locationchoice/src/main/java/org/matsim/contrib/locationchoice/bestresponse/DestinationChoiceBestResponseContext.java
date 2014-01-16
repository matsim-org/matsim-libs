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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrCreateKVals;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author nagel
 *
 */
public class DestinationChoiceBestResponseContext implements MatsimToplevelContainer {	
	public static final String ELEMENT_NAME = "DestinationChoiceBestResponseContext";
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
	private ObjectAttributes prefsAttributes = new ObjectAttributes();
	TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

	private double[] facilitiesKValuesArray;
	private double[] personsKValuesArray;
	private Map<Id, Integer> facilityIndices;
	private Map<Id, Integer> personIndices;
		
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
		this.readPrefs();
		
		log.info("dc context initialized");
	}
	
	private void readOrCreateKVals(long seed) {
		ReadOrCreateKVals computer = new ReadOrCreateKVals(seed, (ScenarioImpl) this.scenario);
		this.arekValsRead = computer.run();
		this.personsKValues = computer.getPersonsKValues();
		this.facilitiesKValues = computer.getFacilitiesKValues();
		
		this.personIndices = new HashMap<Id, Integer>();
		this.personsKValuesArray = new double[this.scenario.getPopulation().getPersons().size()];
		int personIndex = 0;
		for (Id personId : this.scenario.getPopulation().getPersons().keySet()) {
			this.personIndices.put(personId, personIndex);
			this.personsKValuesArray[personIndex] = (Double) this.personsKValues.getAttribute(personId.toString(), "k");
			personIndex++;
		}		
		
		this.facilityIndices = new HashMap<Id, Integer>();
		this.facilitiesKValuesArray = new double[this.scenario.getActivityFacilities().getFacilities().size()];
		int facilityIndex = 0;
		for (Id facilityId : this.scenario.getActivityFacilities().getFacilities().keySet()) {
			this.facilityIndices.put(facilityId, facilityIndex);
			this.facilitiesKValuesArray[facilityIndex] = (Double) this.facilitiesKValues.getAttribute(facilityId.toString(), "k");
			facilityIndex++;
		}
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
	
	private void readPrefs() {
		String prefsFileName = this.scenario.getConfig().locationchoice().getPrefsFile();
		if (!prefsFileName.equals("null")) {			
			ObjectAttributesXmlReader prefsReader = new ObjectAttributesXmlReader(this.prefsAttributes);
			try {
				prefsReader.parse(prefsFileName);
				log.info("reading prefs attributes from: \n"+ prefsFileName);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful prefs reading from files!\n" + prefsFileName);
			}
		} else {
			log.warn("prefs are taken from the config and if available from the desires as there is no preferences file specified \n");
			for (ActivityParams activityParams : this.scenario.getConfig().planCalcScore().getActivityParams()) {				
				for (Person p : this.scenario.getPopulation().getPersons().values()) {
					PersonImpl person = (PersonImpl)p;
					Desires desires = person.getDesires();					
					if (desires != null) {
						// hä? in the desires, only the typical duration can be specified. need to get the rest from the config anyway, or from where else?
						prefsAttributes.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getType(), desires.getActivityDuration(activityParams.getType()));
					} else {				
						prefsAttributes.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getType(), activityParams.getTypicalDuration());
						
					}
					prefsAttributes.putAttribute(p.getId().toString(), "latestStartTime_" + activityParams.getType(), activityParams.getLatestStartTime());
					prefsAttributes.putAttribute(p.getId().toString(), "earliestEndTime_" + activityParams.getType(), activityParams.getEarliestEndTime());
					prefsAttributes.putAttribute(p.getId().toString(), "minimalDuration_" + activityParams.getType(), activityParams.getMinimalDuration());
				}
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

//	public ObjectAttributes getPersonsKValues() {
//		return personsKValues;
//	}
	
//	public ObjectAttributes getFacilitiesKValues() {
//		return facilitiesKValues;
//	}

	public double[] getPersonsKValuesArray() {
		return personsKValuesArray;
	}
	
	public double[] getFacilitiesKValuesArray() {
		return facilitiesKValuesArray;
	}

	public Map<Id, Integer> getPersonIndices() {
		return Collections.unmodifiableMap(this.personIndices);
	}
	
	public int getPersonIndex(Id id) {
		return this.personIndices.get(id);
	}
	
	public Map<Id, Integer> getFacilityIndices() {
		return Collections.unmodifiableMap(this.facilityIndices);
	}
	
	public int getFacilityIndex(Id id) {
		return this.facilityIndices.get(id);
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

	public ObjectAttributes getPrefsAttributes() {
		return prefsAttributes;
	}

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return facilityPenalties;
	}
}
