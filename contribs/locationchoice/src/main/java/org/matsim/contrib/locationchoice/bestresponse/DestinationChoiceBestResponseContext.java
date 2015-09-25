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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrCreateKVals;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
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
	private ObjectAttributes personsBetas = new ObjectAttributes();
	private ObjectAttributes facilitiesAttributes = new ObjectAttributes();
	private ObjectAttributes prefsAttributes = new ObjectAttributes();
	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

	private double[] facilitiesKValuesArray;
	private double[] personsKValuesArray;
	private Map<Id<ActivityFacility>, Integer> facilityIndices;
	private Map<Id<ActivityFacility>, ActivityFacilityWithIndex> faciliesWithIndexMap;
	private Map<Id<Person>, Integer> personIndices;
		
	/**
	 * If this is set to true, QuadTrees are stored in memory.
	 * As a result...
	 * - It is ensured that identical QuadTrees are not created multiple times.
	 * - Code is sped up if QuadTrees are used multiple times (e.g. once in every MATSim iteration).
	 * - Memory consumption is increased since the QuadTrees remain in memory!
	 * This feature was added in nov'14. Its default value is false which was the value before
	 * introduction the feature.
	 */
	private boolean cacheQuadTrees = false;
	private Map<String, QuadTreeRing<ActivityFacilityWithIndex>> quadTreesOfType = new HashMap<String, QuadTreeRing<ActivityFacilityWithIndex>>();
	private TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	
	public DestinationChoiceBestResponseContext(Scenario scenario) {
		this.scenario = scenario;	
		log.info("dc context created but not yet initialized");
		//this.init(); // actually wanted to leave this away to be able to create but not yet fill the context.
	}
	
	public void init() {
		this.params = CharyparNagelScoringParameters.getBuilder(
				scenario.getConfig().planCalcScore(),
				scenario.getConfig().planCalcScore().getScoringParameters( null ), // global, so use default subpop
				scenario.getConfig().scenario()).create();
		ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler((DestinationChoiceConfigGroup) this.scenario.getConfig().getModule("locationchoice"));
		this.scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
		this.actTypeConverter = defineFlexibleActivities.getConverter();
		this.flexibleTypes = defineFlexibleActivities.getFlexibleTypes();
		
		this.readOrCreateKVals(Long.parseLong(this.scenario.getConfig().findParam("locationchoice", "randomSeed")));
		this.readFacilitesAttributesAndBetas();
		this.readPrefs();
		
		log.info("dc context initialized");
	}
	
	private void readOrCreateKVals(long seed) {
		ReadOrCreateKVals computer = new ReadOrCreateKVals(seed, (ScenarioImpl) this.scenario);
		this.arekValsRead = computer.run();
		ObjectAttributes personsKValues = computer.getPersonsKValues();
		ObjectAttributes facilitiesKValues = computer.getFacilitiesKValues();
		
		this.personIndices = new HashMap<Id<Person>, Integer>();
		this.personsKValuesArray = new double[this.scenario.getPopulation().getPersons().size()];
		int personIndex = 0;
		for (Id<Person> personId : this.scenario.getPopulation().getPersons().keySet()) {
			this.personIndices.put(personId, personIndex);
			this.personsKValuesArray[personIndex] = (Double) personsKValues.getAttribute(personId.toString(), "k");
			personIndex++;
		}		
		
		this.facilityIndices = new HashMap<>();
		this.faciliesWithIndexMap = new HashMap<>();
		this.facilitiesKValuesArray = new double[this.scenario.getActivityFacilities().getFacilities().size()];
		int facilityIndex = 0;
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			Id<ActivityFacility> facilityId = facility.getId();
			
			this.facilityIndices.put(facilityId, facilityIndex);
			this.facilitiesKValuesArray[facilityIndex] = (Double) facilitiesKValues.getAttribute(facilityId.toString(), "k");
			this.faciliesWithIndexMap.put(facilityId, new ActivityFacilityWithIndex(facility, facilityIndex));
			
			facilityIndex++;
		}
	}
	
	private void readFacilitesAttributesAndBetas() {
		String pBetasFileName = this.scenario.getConfig().findParam("locationchoice", "pBetasFileName");
		String fAttributesFileName = this.scenario.getConfig().findParam("locationchoice", "fAttributesFileName");
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
		String prefsFileName = this.scenario.getConfig().findParam("locationchoice", "prefsFile");
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
                    prefsAttributes.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getActivityType(), activityParams.getTypicalDuration());
					prefsAttributes.putAttribute(p.getId().toString(), "latestStartTime_" + activityParams.getActivityType(), activityParams.getLatestStartTime());
					prefsAttributes.putAttribute(p.getId().toString(), "earliestEndTime_" + activityParams.getActivityType(), activityParams.getEarliestEndTime());
					prefsAttributes.putAttribute(p.getId().toString(), "minimalDuration_" + activityParams.getActivityType(), activityParams.getMinimalDuration());
				}
			}
		}
	}
	
	public boolean cacheQuadTrees() {
		return this.cacheQuadTrees;
	}
	
	public void cacheQuadTrees(boolean cacheQuadTrees) {
		this.cacheQuadTrees = cacheQuadTrees;
		if (!cacheQuadTrees) {
			this.quadTreesOfType.clear();
			this.facilitiesOfType.clear();
		}
	}
	
	public Tuple<QuadTreeRing<ActivityFacilityWithIndex>, ActivityFacilityImpl[]> getQuadTreeAndFacilities(String activityType) {
		if (this.cacheQuadTrees) {
			QuadTreeRing<ActivityFacilityWithIndex> quadTree = this.quadTreesOfType.get(activityType);
			ActivityFacilityImpl[] facilities = this.facilitiesOfType.get(activityType);
			if (quadTree == null || facilities == null) {				
				Tuple<QuadTreeRing<ActivityFacilityWithIndex>, ActivityFacilityImpl[]> tuple = getTuple(activityType);
				this.quadTreesOfType.put(activityType, tuple.getFirst());
				this.facilitiesOfType.put(activityType, tuple.getSecond());
				
				return tuple;
			} else return new Tuple<QuadTreeRing<ActivityFacilityWithIndex>, ActivityFacilityImpl[]>(quadTree, facilities);
		} else return getTuple(activityType);
	}
	
	private Tuple<QuadTreeRing<ActivityFacilityWithIndex>, ActivityFacilityImpl[]> getTuple(String activityType) {

		TreesBuilder treesBuilder = new TreesBuilder(CollectionUtils.stringToSet(activityType), this.scenario.getNetwork(), (DestinationChoiceConfigGroup) this.scenario.getConfig().getModule("locationchoice"));
		treesBuilder.setActTypeConverter(this.getConverter());
		treesBuilder.createTrees(scenario.getActivityFacilities());
		
		ActivityFacilityImpl[] facilities = treesBuilder.getFacilitiesOfType().get(activityType);
		
		/*
		 * Create a copy of the treesBuilder.getQuadTreesOfType() outcome where the
		 * ActivityFacility objects are replaced by ActivityFacilityWithIndex objects.
		 * TODO: let the TreeBuilder use ActivityFacilityWithIndex objects directly?
		 */
		QuadTreeRing<ActivityFacilityWithIndex> quadTree = null;
		
		QuadTreeRing<ActivityFacility> qt = treesBuilder.getQuadTreesOfType().get(activityType);
		if (qt != null) {
			double minX = qt.getMinEasting();
			double maxX = qt.getMaxEasting();
			double minY = qt.getMinNorthing();
			double maxY = qt.getMaxNorthing();
			quadTree = new QuadTreeRing<ActivityFacilityWithIndex>(minX, minY, maxX, maxY);
			for (ActivityFacility activityFacility : qt.values()) {
				quadTree.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), this.faciliesWithIndexMap.get(activityFacility.getId()));
			}			
		}
		
		return new Tuple<QuadTreeRing<ActivityFacilityWithIndex>, ActivityFacilityImpl[]>(quadTree, facilities);
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

	public Map<Id<Person>, Integer> getPersonIndices() {
		return Collections.unmodifiableMap(this.personIndices);
	}
	
	public int getPersonIndex(Id<Person> id) {
		return this.personIndices.get(id);
	}
	
	public Map<Id<ActivityFacility>, Integer> getFacilityIndices() {
		return Collections.unmodifiableMap(this.facilityIndices);
	}
	
	public int getFacilityIndex(Id<ActivityFacility> id) {
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
	
	public static final class ActivityFacilityWithIndex implements ActivityFacility, HasIndex {

		private final ActivityFacility activityFacility;
		private final int index;
		
		public ActivityFacilityWithIndex(ActivityFacility activityFacility, int index) {
			this.activityFacility = activityFacility;
			this.index = index;
		}
		
		@Override
		public Id<Link> getLinkId() {
			return this.activityFacility.getLinkId();
		}

		@Override
		public Coord getCoord() {
			return this.activityFacility.getCoord();
		}

		@Override
		public Id<ActivityFacility> getId() {
			return this.activityFacility.getId();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return this.activityFacility.getCustomAttributes();
		}

		@Override
		public int getArrayIndex() {
			return this.index;
		}

		@Override
		public Map<String, ActivityOption> getActivityOptions() {
			return this.activityFacility.getActivityOptions();
		}

		@Override
		public void addActivityOption(ActivityOption option) {
			this.activityFacility.addActivityOption(option);
		}
	}
}
