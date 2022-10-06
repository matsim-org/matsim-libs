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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.contrib.locationchoice.utils.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.attributable.Attributes;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

//import org.matsim.contrib.locationchoice.utils.ActTypeConverter;

/**
 * @author nagel
 *
 */
class DestinationChoiceContext implements MatsimToplevelContainer {
	
	private static final Logger log = LogManager.getLogger(DestinationChoiceContext.class);
	
	public static final String ELEMENT_NAME = "DestinationChoiceBestResponseContext";
	
	private final Scenario scenario;
	private ScaleEpsilon scaleEpsilon;
//	private ActTypeConverter actTypeConverter;
	private HashSet<String> flexibleTypes;
	private ScoringParameters params;
	private FrozenTastesConfigGroup dccg;
	private int arekValsRead = 1;
	private ObjectAttributes personsBetas = new ObjectAttributes();
	private ObjectAttributes facilitiesAttributes = new ObjectAttributes();
	private ObjectAttributes prefsAttributes = new ObjectAttributes();
	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

	private double[] facilitiesKValuesArray;
	private double[] personsKValuesArray;
	private TObjectIntMap<Id<ActivityFacility>> facilityIndices;
	private Map<Id<ActivityFacility>, ActivityFacilityWithIndex> faciliesWithIndexMap;
	private TObjectIntMap<Id<Person>> personIndices;

	private Map<String, QuadTree<ActivityFacilityWithIndex>> quadTreesOfType = new HashMap<String, QuadTree<ActivityFacilityWithIndex>>();
	private TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	
	public DestinationChoiceContext(Scenario scenario) {
		this.scenario = scenario;	
		log.info("dc context created but not yet initialized");
		this.init(); // actually wanted to leave this away to be able to create but not yet fill the context.

		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		scenario.addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME , dcScore);
		// is ONLY there to make the personsMaxDCScoreUnscaled available, yyyy which would now be much better be done with person.getAttributes().putAttribute(...). kai,
		// mar'19

		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(this);

		computer.readOrCreateMaxDCScore( this.kValsAreRead() );
		// the k vals are read or computed in DestinationChoiceContext.  lcContest.kValsAreRead() is set accordingly.  If they were read, the method here attempts to also
		// read the max dc score values, otherwise it computes them.

		dcScore.setPersonsMaxDCScoreUnscaled( computer.getPersonsMaxEpsUnscaled() );


	}
	
	public void init() {
		if ( params==null ){
			this.params = new ScoringParameters.Builder( scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters( null ),
				  scenario.getConfig().scenario() ).build();
			this.dccg = ConfigUtils.addOrGetModule( this.scenario.getConfig(), FrozenTastesConfigGroup.class );
			ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler( this.dccg );
			this.scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
//			this.actTypeConverter = defineFlexibleActivities.getConverter();
			this.flexibleTypes = defineFlexibleActivities.getFlexibleTypes();

			this.readOrCreateKVals( dccg.getRandomSeed() );
			this.readFacilitesAttributesAndBetas();
			this.readPrefs();

			log.info( "dc context initialized" );
		}
	}
	
	private void readOrCreateKVals(long seed) {
		ReadOrCreateKVals computer = new ReadOrCreateKVals(seed, this.scenario);
		this.arekValsRead = computer.run();

		this.personIndices = new TObjectIntHashMap<>();
		this.personsKValuesArray = new double[this.scenario.getPopulation().getPersons().size()];
		int personIndex = 0;
		for (Id<Person> personId : this.scenario.getPopulation().getPersons().keySet()) {
			this.personIndices.put(personId, personIndex);
			this.personsKValuesArray[personIndex] = (Double) scenario.getPopulation().getPersons().get(personId).getAttributes().getAttribute("k");
			personIndex++;
		}		
		
		this.facilityIndices = new TObjectIntHashMap<>();
		this.faciliesWithIndexMap = new HashMap<>();
		this.facilitiesKValuesArray = new double[this.scenario.getActivityFacilities().getFacilities().size()];
		int facilityIndex = 0;
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			Id<ActivityFacility> facilityId = facility.getId();
			
			this.facilityIndices.put(facilityId, facilityIndex);
			this.facilitiesKValuesArray[facilityIndex] = (Double) facility.getAttributes().getAttribute("k");
			this.faciliesWithIndexMap.put(facilityId, new ActivityFacilityWithIndex(facility, facilityIndex));
			
			facilityIndex++;
		}
	}
	
	private void readFacilitesAttributesAndBetas() {
		String pBetasFileName = this.dccg.getpBetasFile();
		String fAttributesFileName = this.dccg.getfAttributesFile();
		if (pBetasFileName != null && fAttributesFileName!= null) {			
			ObjectAttributesXmlReader personsBetasReader = new ObjectAttributesXmlReader(this.personsBetas);
			ObjectAttributesXmlReader facilitiesAttributesReader = new ObjectAttributesXmlReader(this.facilitiesAttributes);
			try {
				personsBetasReader.readFile(pBetasFileName);
				facilitiesAttributesReader.readFile(fAttributesFileName);
				log.info("reading betas and facilities attributes from: \n"+ pBetasFileName + "\n" + fAttributesFileName);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful betas and facilities attributes from files!\n" + pBetasFileName + "\n" + fAttributesFileName);
			}
		}
	}
	
	private void readPrefs() {
		String prefsFileName = this.dccg.getPrefsFile();
		if (prefsFileName != null) {			
			ObjectAttributesXmlReader prefsReader = new ObjectAttributesXmlReader(this.prefsAttributes);
			try {
				prefsReader.readFile(prefsFileName);
				log.info("reading prefs attributes from: \n"+ prefsFileName);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful prefs reading from files!\n" + prefsFileName);
			}
		} else {
			log.warn("prefs are taken from the config and if available from the desires as there is no preferences file specified \n");
			for (ActivityParams activityParams : this.scenario.getConfig().planCalcScore().getActivityParams()) {				
				for (Person p : this.scenario.getPopulation().getPersons().values()) {
					prefsAttributes.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getActivityType(),
							activityParams.getTypicalDuration());
					prefsAttributes.putAttribute(p.getId().toString(), "latestStartTime_" + activityParams.getActivityType(),
							activityParams.getLatestStartTime());
					prefsAttributes.putAttribute(p.getId().toString(), "earliestEndTime_" + activityParams.getActivityType(),
							activityParams.getEarliestEndTime());
					prefsAttributes.putAttribute(p.getId().toString(), "minimalDuration_" + activityParams.getActivityType(),
							activityParams.getMinimalDuration());
				}
			}
		}
	}
	
//	public boolean cacheQuadTrees() {
//		return this.cacheQuadTrees;
//	}
//
//	public void cacheQuadTrees(boolean cacheQuadTrees) {
//		this.cacheQuadTrees = cacheQuadTrees;
//		if (!cacheQuadTrees) {
//			this.quadTreesOfType.clear();
//			this.facilitiesOfType.clear();
//		}
//	}
	
	Tuple<QuadTree<ActivityFacilityWithIndex>, ActivityFacilityImpl[]> getQuadTreeAndFacilities( String activityType ) {
		/**
		 * If this is set to true, QuadTrees are stored in memory.
		 * As a result...
		 * - It is ensured that identical QuadTrees are not created multiple times.
		 * - Code is sped up if QuadTrees are used multiple times (e.g. once in every MATSim iteration).
		 * - Memory consumption is increased since the QuadTrees remain in memory!
		 * This feature was added in nov'14. Its default value is false which was the value before
		 * introduction the feature.
		 */
		boolean cacheQuadTrees = false;
		if ( cacheQuadTrees ) {
			QuadTree<ActivityFacilityWithIndex> quadTree = this.quadTreesOfType.get(activityType);
			ActivityFacilityImpl[] facilities = this.facilitiesOfType.get(activityType);
			if (quadTree == null || facilities == null) {
				Tuple<QuadTree<ActivityFacilityWithIndex>, ActivityFacilityImpl[]> tuple = getTuple(activityType);
				this.quadTreesOfType.put(activityType, tuple.getFirst());
				this.facilitiesOfType.put(activityType, tuple.getSecond());

				return tuple;
			} else return new Tuple<>( quadTree, facilities );
		} else return getTuple(activityType);
	}
	
	private Tuple<QuadTree<ActivityFacilityWithIndex>, ActivityFacilityImpl[]> getTuple(String activityType) {

		TreesBuilder treesBuilder = new TreesBuilder(CollectionUtils.stringToSet(activityType), this.scenario.getNetwork(), this.dccg);
//		treesBuilder.setActTypeConverter(this.getConverter());
		treesBuilder.createTrees(scenario.getActivityFacilities());
		
		ActivityFacilityImpl[] facilities = treesBuilder.getFacilitiesOfType().get(activityType);
		
		/*
		 * Create a copy of the treesBuilder.getQuadTreesOfType() outcome where the
		 * ActivityFacility objects are replaced by ActivityFacilityWithIndex objects.
		 * TODO: let the TreeBuilder use ActivityFacilityWithIndex objects directly?
		 */
		QuadTree<ActivityFacilityWithIndex> quadTree = null;
		
		QuadTree<ActivityFacility> qt = treesBuilder.getQuadTreesOfType().get(activityType);
		if (qt != null) {
			double minX = qt.getMinEasting();
			double maxX = qt.getMaxEasting();
			double minY = qt.getMinNorthing();
			double maxY = qt.getMaxNorthing();
			quadTree = new QuadTree<ActivityFacilityWithIndex>(minX, minY, maxX, maxY);
			for (ActivityFacility activityFacility : qt.values()) {
				quadTree.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), this.faciliesWithIndexMap.get(activityFacility.getId()));
			}			
		}
		
		return new Tuple<QuadTree<ActivityFacilityWithIndex>, ActivityFacilityImpl[]>(quadTree, facilities);
	}
	
	public Scenario getScenario() {
		return scenario;
	}

	public ScaleEpsilon getScaleEpsilon() {
		return scaleEpsilon;
	}

//	public ActTypeConverter getConverter() {
//		return actTypeConverter;
//	}

	public HashSet<String> getFlexibleTypes() {
		return flexibleTypes;
	}

	public ScoringParameters getParams() {
		return params;
	}

	private boolean kValsAreRead() {
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

//	public Map<Id<Person>, Integer> getPersonIndices() {
//		return Collections.unmodifiableMap(this.personIndices);
//	}
	
	public int getPersonIndex(Id<Person> id) {
		return this.personIndices.get(id);
	}
	
//	public Map<Id<ActivityFacility>, Integer> getFacilityIndices() {
//		return Collections.unmodifiableMap(this.facilityIndices);
//	}
	
	public int getFacilityIndex(Id<ActivityFacility> id) {
		return this.facilityIndices.get(id);
	}
	
	ObjectAttributes getPersonsBetas() {
		return personsBetas;
	}

	ObjectAttributes getFacilitiesAttributes() {
		return facilitiesAttributes;
	}

	@Override
	public MatsimFactory getFactory() {
		return null;
	}

	ObjectAttributes getPrefsAttributes() {
		return prefsAttributes;
	}

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return facilityPenalties;
	}
	
	public static final class ActivityFacilityWithIndex implements ActivityFacility, HasIndex {

		private final ActivityFacility activityFacility;
		private final int index;
		
		ActivityFacilityWithIndex( ActivityFacility activityFacility, int index ) {
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

		@Override
		public void setCoord(Coord coord) {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Attributes getAttributes() {
			return new Attributes();
		}
	}
}
