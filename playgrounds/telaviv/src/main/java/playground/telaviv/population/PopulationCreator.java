/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.*;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.telaviv.config.XMLParameterParser;
import playground.telaviv.facilities.FacilitiesCreator;
import playground.telaviv.zones.Emme2Zone;
import playground.telaviv.zones.Emme2ZonesFileParser;

import javax.inject.Provider;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * Creates the internal population of the Tel Aviv model. Input data is taken
 * from the activity-based model. For a description of the data fields look at the
 * "final report" pdf, pages 2-4 and 2-8. In addition, information about the activity
 * timings (start time and duration) is included in the file. 
 * </p>
 * 
 * @author cdobler
 */
public class PopulationCreator {

	private static final Logger log = Logger.getLogger(PopulationCreator.class);

	private String basePath = "";
	private String populationFile = "";
	private String networkFile = "";
	private String facilitiesFile = "";
	private String facilitiesAttributesFile = "";
	private String zonalAttributesFile = "";
	private String outputFile = "";

	private String separatorZonalFile = ",";
	private String separatorPopulationFile = ",";
	private Random random = MatsimRandom.getLocalInstance();

	/*
	 * The +/- range within an activity's departure time is randomly shifted.
	 */
	private double timeMutationRange = 0.0;
	
	/*
	 * Opening Times:
	 * home 0..24
	 * leisure/other 6..22
	 * school 8..14
	 * university 9..18
	 * work 8..18
	 * shop 9..19
	 */
	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				String file = args[0];
				new PopulationCreator(file);
			} else {
				log.error("No input config file was given. Therefore cannot proceed. Aborting!");
				return;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// NOT a MATSim config file!
	public PopulationCreator(String configurationFile) throws Exception {
		
		Map<String, String> parameterMap = new XMLParameterParser().parseFile(configurationFile);
		String value;
		
		value = parameterMap.remove("basePath");
		if (value != null) basePath = value;
		
		value = parameterMap.remove("populationFile");
		if (value != null) populationFile = value;

		value = parameterMap.remove("networkFile");
		if (value != null) networkFile = value;
		
		value = parameterMap.remove("separatorZonalFile");
		if (value != null) separatorZonalFile = value;
		
		value = parameterMap.remove("separatorPopulationFile");
		if (value != null) separatorPopulationFile = value;
		
		value = parameterMap.remove("facilitiesFile");
		if (value != null) facilitiesFile = value;
		
		value = parameterMap.remove("facilitiesAttributesFile");
		if (value != null) facilitiesAttributesFile = value;
		
		value = parameterMap.remove("zonalAttributesFile");
		if (value != null) zonalAttributesFile = value;

		value = parameterMap.remove("timeMutationRange");
		if (value != null) timeMutationRange = Double.parseDouble(value);
		
		value = parameterMap.remove("outputFile");
		if (value != null) outputFile = value;

		for (String key : parameterMap.keySet()) log.warn("Found parameter " + key + " which is not handled!");
				
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(basePath + networkFile);
		config.facilities().setInputFile(basePath + facilitiesFile);
		config.facilities().setInputFacilitiesAttributesFile(basePath + facilitiesAttributesFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		log.info("loading zonal attributes ...");
		boolean skipHeader = true;
		Map<Integer, Emme2Zone> zonalAttributes = new Emme2ZonesFileParser(basePath + zonalAttributesFile, 
				separatorZonalFile).readFile(skipHeader);
		log.info("done.\n");

		log.info("Creating mapping from facilities to zones...");
		Map<Integer, List<ActivityFacility>> facilitiesToZoneMap = createFacilityToZoneMapping(scenario);
		log.info("done.");
		
		log.info("Parsing population file...");
		Map<Integer, ParsedPerson> personMap = new PersonFileParser(basePath + populationFile, 
				separatorPopulationFile, true).readFile();
		log.info(personMap.size());
		log.info("done.");

		log.info("Creating MATSim population...");
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		Counter counter = new Counter ("People skipped because invalid plan information was found: #");
		for (Entry<Integer, ParsedPerson> entry : personMap.entrySet()) {
			Id<Person> id = Id.create(String.valueOf(entry.getKey()), Person.class);
			ParsedPerson emme2Person = entry.getValue();

			PersonImpl person = (PersonImpl) populationFactory.createPerson(id);
			
			setBasicParameters(person, emme2Person);

			/*
			 * Some entries in the input file might be wrong (e.g. persons which want to perform
			 * an activity from type a in a zone which does not offer that type of activity).
			 */
			boolean vaildPerson = createAndAddInitialPlan(person, emme2Person, scenario, facilitiesToZoneMap, zonalAttributes);

			if (vaildPerson) scenario.getPopulation().addPerson(person);
			else counter.incCounter();
		}
		counter.printCounter();
		log.info("done.");

		log.info("Mutating populations activity times...");
		this.mutateActivityTimings(scenario);
		log.info("done.");
		
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(basePath + outputFile);
		log.info("done.");
	}
	
	private void mutateActivityTimings(Scenario scenario) {
		
		Config config = scenario.getConfig();
		
		config.global().setNumberOfThreads(8);
		TimeAllocationMutator timeAllocationMutator = new TimeAllocationMutator(config, null, timeMutationRange, true);
		
		final TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutilityFactory travelDisutilityFactory = new Builder( TransportMode.car, config.planCalcScore() );
		final TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);
		ReplanningContext context = new ReplanningContext() {
			@Override
			public int getIteration() {
				return 0;
			}
		};
		timeAllocationMutator.prepareReplanning(context);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) timeAllocationMutator.handlePlan(plan);
		}
		timeAllocationMutator.finishReplanning();
	}

	private Map<Integer, List<ActivityFacility>> createFacilityToZoneMapping(Scenario scenario) {
		
		Map<Integer, List<ActivityFacility>> facilitiesToZoneMap = new HashMap<Integer, List<ActivityFacility>>();
		ObjectAttributes objectAttributes = scenario.getActivityFacilities().getFacilityAttributes();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Object tazObject = objectAttributes.getAttribute(facility.getId().toString(), FacilitiesCreator.TAZObjectAttributesName);
			if (tazObject != null) {
				int taz = (Integer) tazObject;
				List<ActivityFacility> list = facilitiesToZoneMap.get(taz);
				if (list == null) {
					list = new ArrayList<ActivityFacility>();
					facilitiesToZoneMap.put(taz, list);
				}
				list.add(facility);
			}
		}
		return facilitiesToZoneMap;
	}
	
	/*
	 * Set some basic person parameters like age, sex, license and car availability.
	 */
	private void setBasicParameters(Person person, ParsedPerson emme2Person) {
		PersonUtils.setAge(person, emme2Person.AGE);

		if (emme2Person.GENDER == 1) PersonUtils.setSex(person, "m");
		else PersonUtils.setSex(person, "f");

		if (emme2Person.LICENSE == 1) {
			PersonUtils.setLicence(person, "yes");
			if (emme2Person.NUMVEH == 0) PersonUtils.setCarAvail(person, "never");
			else if (emme2Person.NUMVEH >= emme2Person.HHLICENSES) PersonUtils.setCarAvail(person, "always");
			else PersonUtils.setCarAvail(person, "sometimes");
		}
		else {
			PersonUtils.setLicence(person, "no");
			PersonUtils.setCarAvail(person, "sometimes");
		}
	}

	/*
	 * Create initial plan.
	 *
	 * Activity Coding:
	 * 0 - no (home)
	 * 1 - work
	 * 2 - study (education type depending on type of zone!)
	 * 3 - shopping
	 * 4 - other (leisure)
	 */
	private boolean createAndAddInitialPlan(PersonImpl person, ParsedPerson emme2Person, Scenario scenario,
			Map<Integer, List<ActivityFacility>> facilitiesToZoneMap, Map<Integer, Emme2Zone> zonalAttributes) {
		if ( true ) throw new RuntimeException( "desires do not exist anymore. Please find a way to do another way or contact the core team." );
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		//Desires desires = person.createDesires("");

		LegImpl leg;
		ActivityImpl activity;
//		Route route;
		String transportMode;
		ActivityFacility activityFacility;
		String activityType;

		boolean fullTimeJob = (emme2Person.WORKSTA != 2);

		int homeZone = emme2Person.TAZH;

		/*
		 * Primary Activity
		 */
		int primaryMainActivityType = emme2Person.MAINACTPRI;
		int primaryMainActivityZone = emme2Person.TAZDPR;
		int primaryMainModePreActivity = emme2Person.MAINMODPR;

//		boolean primaryPreStop = (emme2Person.INTSTOPPR == 1 || emme2Person.INTSTOPPR == 3);
		boolean primaryPreStop = (emme2Person.INTSTOPPR == 2 || emme2Person.INTSTOPPR == 4);
		int primaryPreStopActivityType = emme2Person.INTACTBP + 1;	// index is shifted by one!
		int primaryPreStopActivityZone = emme2Person.TAZBPR;

//		boolean primaryPostStop = (emme2Person.INTSTOPPR == 2 || emme2Person.INTSTOPPR == 3);
		boolean primaryPostStop = (emme2Person.INTSTOPPR == 3 || emme2Person.INTSTOPPR == 4);
		int primaryPostStopActivityType = emme2Person.INTACTAP + 1; // index is shifted by one!
		int primaryPostStopActivityZone = emme2Person.TAZAPR;

		int primaryMainModeSwitch = emme2Person.SWMODPR;


		/*
		 * Secondary Activity
		 */
		int secondaryMainActivityType = emme2Person.MAINACTSEC;
		int secondaryMainActivityZone = emme2Person.TAZDSEC;
		int secondaryMainMode = emme2Person.MAINMODSE;

//		boolean secondaryPreStop = (emme2Person.INTSTOPSEC == 1 || emme2Person.INTSTOPSEC == 3);
		boolean secondaryPreStop = (emme2Person.INTSTOPSEC == 2 || emme2Person.INTSTOPSEC == 4);
//		int secondaryPreStopActivityType = emme2Person.MAINACTSEC; // use same as secondary main activity
		int secondaryPreStopActivityType = 5; // use "other" Activity
		int secondaryPreStopActivityZone = emme2Person.TAZBSEC;

//		boolean secondaryPostStop = (emme2Person.INTSTOPSEC == 2 || emme2Person.INTSTOPSEC == 3);
		boolean secondaryPostStop = (emme2Person.INTSTOPSEC == 3 || emme2Person.INTSTOPSEC == 4);
//		int secondaryPostStopActivityType = emme2Person.MAINACTSEC; // use same as secondary main activity
		int secondaryPostStopActivityType = 5; // use "other" Activity
		int secondaryPostStopActivityZone = emme2Person.TAZASEC;


		boolean hasPrimaryActivity = zonalAttributes.containsKey(primaryMainActivityZone);
		boolean hasSecondaryActivity = zonalAttributes.containsKey(secondaryMainActivityZone);

		ActivityFacility homeFacility;
		ActivityFacility previousFacility;
		double time = 0.0;

		/*
		 * We always have at least a home activity.
		 *
		 * If the zone of the primary and secondary activity is
		 * not valid the persons stays at home the whole day.
		 */
		homeFacility = selectFacilityByZone(homeZone, facilitiesToZoneMap);
		activity = (ActivityImpl) populationFactory.createActivityFromCoord("home", homeFacility.getCoord());
		activity.setFacilityId(homeFacility.getId());
		activity.setLinkId(homeFacility.getLinkId());
		activity.setStartTime(0.0);

		if (hasPrimaryActivity) {
			activity.setMaximumDuration(emme2Person.START_1);
			activity.setEndTime(emme2Person.START_1);
			time = time + emme2Person.START_1;
		}
		else if (hasSecondaryActivity) {
			activity.setMaximumDuration(emme2Person.START_2);
			activity.setEndTime(emme2Person.START_2);
			time = time + emme2Person.START_2;
		}
		else {
			// It is the only Activity in the plan so we don't set an endtime.
		}
		
//		desires.accumulateActivityDuration(activity.getType(), activity.getMaximumDuration());
		plan.addActivity(activity);

		// If we have no activity chains we have nothing left to do.
		if (!hasPrimaryActivity && !hasSecondaryActivity) {
			//desires.accumulateActivityDuration("home", 86400);
			return true;
		}

		previousFacility = homeFacility;
		transportMode = getPrimaryTransportMode(primaryMainModePreActivity);

		/*
		 * Primary Activity
		 */
		if (hasPrimaryActivity) {
			/*
			 * If we have an Activity before the primary Activity
			 */
			if (primaryPreStop) {
				ActivityFacility primaryPreFacility = selectFacilityByZone(primaryPreStopActivityZone, facilitiesToZoneMap);

				leg = (LegImpl) populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, primaryPreLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				/*
				 * If null is returned, the found activity type cannot be performed in the found
				 * facility. Skip those persons.
				 */
				activityType = getActivityTypeString(primaryPreStopActivityType, primaryPreFacility);
				if (activityType == null) return false;
				
				activity = (ActivityImpl) populationFactory.createActivityFromCoord(activityType, primaryPreFacility.getCoord());
				activity.setFacilityId(primaryPreFacility.getId());
				activity.setLinkId(primaryPreFacility.getLinkId());
				activity.setStartTime(time);
				activity.setMaximumDuration(emme2Person.DUR_1_BEF);
				activity.setEndTime(time + emme2Person.DUR_1_BEF);
				/*
				 * Ensure that typical duration is at least one hour. Very few agents have a duration of
				 * 0.0 seconds, which results in crashes in the scoring function (typical duration is 0.0
				 * which leads to a divide by zero problem). 
				 */
				//desires.accumulateActivityDuration(activity.getType(), Math.max(3600.0, activity.getMaximumDuration()));
				plan.addActivity(activity);

				previousFacility = primaryPreFacility;
				time = time + emme2Person.DUR_1_BEF;
			}

			/*
			 * Primary Activity
			 */
			ActivityFacility primaryFacility = selectFacilityByZone(primaryMainActivityZone, facilitiesToZoneMap);

			leg = (LegImpl) populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, primaryLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			/*
			 * If null is returned, the found activity type cannot be performed in the found
			 * facility. Skip those persons.
			 */
			activityType = getActivityTypeString(primaryMainActivityType, primaryFacility);
			if (activityType == null) return false;
			
			activity = (ActivityImpl) populationFactory.createActivityFromCoord(activityType, primaryFacility.getCoord());
			activity.setFacilityId(primaryFacility.getId());
			activity.setLinkId(primaryFacility.getLinkId());
			activity.setStartTime(time);
			activity.setMaximumDuration(emme2Person.DUR_1_MAIN);
			activity.setEndTime(time + emme2Person.DUR_1_MAIN);
			//desires.accumulateActivityDuration(activity.getType(), Math.max(3600.0, activity.getMaximumDuration()));
			plan.addActivity(activity);

			previousFacility = primaryFacility;
			transportMode = getSwitchedTransportMode(primaryMainModeSwitch, transportMode);
			time = time + emme2Person.DUR_1_MAIN;

			/*
			 * If we have an Activity after the primary Activity
			 */
			if (primaryPostStop) {
				ActivityFacility primaryPostFacility = selectFacilityByZone(primaryPostStopActivityZone, facilitiesToZoneMap);

				leg = (LegImpl) populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, primaryPostLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				/*
				 * If null is returned, the found activity type cannot be performed in the found
				 * facility. Skip those persons.
				 */
				activityType = getActivityTypeString(primaryPostStopActivityType, primaryPostFacility);
				if (activityType == null) return false;

				activity = (ActivityImpl) populationFactory.createActivityFromCoord(activityType, primaryPostFacility.getCoord());
				activity.setFacilityId(primaryPostFacility.getId());
				activity.setLinkId(primaryPostFacility.getLinkId());
				activity.setStartTime(time);
				activity.setMaximumDuration(emme2Person.DUR_1_AFT);
				activity.setEndTime(time + emme2Person.DUR_1_AFT);
				//desires.accumulateActivityDuration(activity.getType(), Math.max(3600.0, activity.getMaximumDuration()));
				plan.addActivity(activity);

				previousFacility = primaryPostFacility;
				time = time + emme2Person.DUR_1_AFT;
			}

			leg = (LegImpl) populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, homeLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			activity = (ActivityImpl) populationFactory.createActivityFromCoord("home", homeFacility.getCoord());
			activity.setFacilityId(homeFacility.getId());
			activity.setLinkId(homeFacility.getLinkId());
			activity.setStartTime(time);

			/*
			 * If we have a secondary activity we have to calculate and set
			 * the duration of the home activity at the end of the main
			 * activity trip.
			 */
			if (hasSecondaryActivity) {
				activity.setMaximumDuration(emme2Person.START_2 - time);
				activity.setEndTime(emme2Person.START_2);
			}
//			desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);
		}


		/*
		 * Secondary Activity
		 */
		if (hasSecondaryActivity) {
			time = emme2Person.START_2;

			previousFacility = homeFacility;
			transportMode = getSecondaryTransportMode(secondaryMainMode);

			/*
			 * If we have an Activity before the secondary Activity
			 */
			if (secondaryPreStop) {
				ActivityFacility secondaryPreFacility = selectFacilityByZone(secondaryPreStopActivityZone, facilitiesToZoneMap);

				leg = (LegImpl) populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, secondaryPreLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				/*
				 * If null is returned, the found activity type cannot be performed in the found
				 * facility. Skip those persons.
				 */
				activityType = getActivityTypeString(secondaryPreStopActivityType, secondaryPreFacility);
				if (activityType == null) return false;
				
				activity = (ActivityImpl) populationFactory.createActivityFromCoord(activityType, secondaryPreFacility.getCoord());
				activity.setFacilityId(secondaryPreFacility.getId());
				activity.setLinkId(secondaryPreFacility.getLinkId());
				activity.setStartTime(time);
				activity.setMaximumDuration(emme2Person.DUR_2_BEF);
				activity.setEndTime(time + emme2Person.DUR_2_BEF);
				//desires.accumulateActivityDuration(activity.getType(), Math.max(3600.0, activity.getMaximumDuration()));
				plan.addActivity(activity);

				previousFacility = secondaryPreFacility;
				time = time + emme2Person.DUR_2_BEF;
			}

			/*
			 * Secondary Activity
			 */
			ActivityFacility secondaryFacility = selectFacilityByZone(secondaryMainActivityZone, facilitiesToZoneMap);

			leg = (LegImpl) populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, secondaryLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			/*
			 * If null is returned, the found activity type cannot be performed in the found
			 * facility. Skip those persons.
			 */
			activityType = getActivityTypeString(secondaryMainActivityType, secondaryFacility);
			if (activityType == null) return false;
			
			activity = (ActivityImpl) populationFactory.createActivityFromCoord(activityType, secondaryFacility.getCoord());
			activity.setFacilityId(secondaryFacility.getId());
			activity.setLinkId(secondaryFacility.getLinkId());
			activity.setStartTime(time);
			activity.setMaximumDuration(emme2Person.DUR_2_MAIN);
			activity.setEndTime(time + emme2Person.DUR_2_MAIN);
			//desires.accumulateActivityDuration(activity.getType(), Math.max(3600.0, activity.getMaximumDuration()));
			plan.addActivity(activity);

			previousFacility = secondaryFacility;
			time = time + emme2Person.DUR_2_MAIN;

			/*
			 * If we have an Activity after the secondary Activity
			 */
			if (secondaryPostStop) {
				ActivityFacility secondaryPostFacility = selectFacilityByZone(secondaryPostStopActivityZone, facilitiesToZoneMap);

				leg = (LegImpl) populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, secondaryPostLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				/*
				 * If null is returned, the found activity type cannot be performed in the found
				 * facility. Skip those persons.
				 */
				activityType = getActivityTypeString(secondaryPostStopActivityType, secondaryPostFacility);
				if (activityType == null) return false;
				
				activity = (ActivityImpl) populationFactory.createActivityFromCoord(activityType, secondaryPostFacility.getCoord());
				activity.setFacilityId(secondaryPostFacility.getId());
				activity.setLinkId(secondaryPostFacility.getLinkId());
				activity.setStartTime(time);
				activity.setMaximumDuration(emme2Person.DUR_2_AFT);
				activity.setEndTime(time + emme2Person.DUR_2_AFT);
				//desires.accumulateActivityDuration(activity.getType(), Math.max(3600.0, activity.getMaximumDuration()));
				plan.addActivity(activity);

				previousFacility = secondaryPostFacility;
				time = time + emme2Person.DUR_2_AFT;
			}

			leg = (LegImpl) populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, homeLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			/*
			 * It is the last Activity of the plan so we don't set an end time.
			 */
			activity = (ActivityImpl) populationFactory.createActivityFromCoord("home", homeFacility.getCoord());
			activity.setFacilityId(homeFacility.getId());
			activity.setLinkId(homeFacility.getLinkId());
			activity.setStartTime(time);
//			desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);
		}
		
		/*
		 * Finally add a home desire that has a duration of 86400 - all other activities.
		 */
		double otherDurations = 0.0;
		//for (double duration : desires.getActivityDurations().values()) {
		//	otherDurations = otherDurations + duration;
		//}
		//if (otherDurations < 86400) {
		//	// make desired home duration not longer than 12 hours
		//	double homeDuration = Math.min(12*3600, 86400 - otherDurations);
		//	desires.accumulateActivityDuration("home", homeDuration);
		//}
		
		// no errors have been found, so return true
		return true;
	}

	private ActivityFacility selectFacilityByZone(int TAZ, Map<Integer, List<ActivityFacility>> facilitiesToZoneMap) {
		List<ActivityFacility> list = facilitiesToZoneMap.get(TAZ);
		
		if (list == null || list.size() == 0) {
			throw new RuntimeException("No facilities have been mapped to zone " + TAZ + ". Aborting!");
		}
		
		return list.get(this.random.nextInt(list.size()));
	}

	private String getPrimaryTransportMode(int code) {
		String transportMode = null;
		switch (code) {
//			case 1: transportMode = TransportMode.train; break; // Rail Park & Ride
			case 1: transportMode = TransportMode.pt; break; // Rail Park & Ride
//			case 2: transportMode = TransportMode.train; break; // Rail Bus Assess
			case 2: transportMode = TransportMode.pt; break; // Rail Bus Assess
//			case 3: transportMode = TransportMode.train; break; // Rail Kiss & Ride
			case 3: transportMode = TransportMode.pt; break; // Rail Kiss & Ride
//			case 4: transportMode = TransportMode.train; break; // Rail Walk Access
			case 4: transportMode = TransportMode.pt; break; // Rail Walk Access
//			case 5: transportMode = TransportMode.bus; break; // Bus Kiss & Ride
			case 5: transportMode = TransportMode.pt; break; // Bus Kiss & Ride
//			case 6: transportMode = TransportMode.bus; break; // Bus Walk Access
			case 6: transportMode = TransportMode.pt; break; // Bus Walk Access
//			case 7: transportMode = TransportMode.other; break; // Taxi
			case 7: transportMode = "undefined"; break; // Taxi
//			case 8: transportMode = TransportMode.other; break; // Car Passenger
			case 8: transportMode = "undefined"; break; // Car Passenger
			case 9: transportMode = TransportMode.car; break; // Car Driver
//			case 10: transportMode = TransportMode.bus; break; // Bus Park & Ride
			case 10: transportMode = TransportMode.pt; break; // Bus Park & Ride
//			case 11: transportMode = TransportMode.bus; break; // Mass Transit Walk Access
			case 11: transportMode = TransportMode.pt; break; // Mass Transit Walk Access
			case 12: transportMode = TransportMode.pt; break; // Mass Transit Kiss & Ride Access
			case 13: transportMode = TransportMode.pt;  break; // Mass Transit Park & Ride Access
			case 14: transportMode = TransportMode.pt;  break; // Mass Transit Bus Access

//			default: log.warn("Unknown Primary Transport Mode Code!"); transportMode = TransportMode.other; break;
			default: log.warn("Unknown Primary Transport Mode Code!"); transportMode = "undefined"; break;
		}

		return transportMode;
	}

	private String getSwitchedTransportMode(int code, String currentTransportMode) {
		switch (code) {
			case 1: break; // no switch
//			case 2: currentTransportMode = TransportMode.train; break; // to rail
			case 2: currentTransportMode = TransportMode.pt; break; // to rail
//			case 3: currentTransportMode = TransportMode.bus; break; // to bus
			case 3: currentTransportMode = TransportMode.pt; break; // to bus
//			case 4: currentTransportMode = TransportMode.other; break; // to taxi
			case 4: currentTransportMode = "undefined"; break; // to taxi
			case 5: currentTransportMode = TransportMode.car; break; // to driver
//			case 6: currentTransportMode = TransportMode.other; break; // to passenger
			case 6: currentTransportMode = "undefined"; break; // to passenger

			default:
				log.warn("Unknown Transport Mode Change Code! " + code + ", use pt as TransportMode.");
				currentTransportMode = TransportMode.pt; break;
		}

		return currentTransportMode;
	}

	private String getSecondaryTransportMode(int code) {
		String transportMode = null;
		switch (code) {
//			case 1: transportMode = TransportMode.bus; break; // Bus
			case 1: transportMode = TransportMode.pt; break; // Bus
			case 2: transportMode = TransportMode.car; break; // Car Driver
//			case 3: transportMode = TransportMode.other; break; // Car Passenger
			case 3: transportMode = "undefined"; break; // Car Passenger

//			default: log.warn("Unknown Secondary Transport Mode Code!"); transportMode = TransportMode.other; break;
			default: log.warn("Unknown Secondary Transport Mode Code!"); transportMode = "undefined"; break;
		}

		return transportMode;
	}

	private static int activityNotSupportedCounter = 0;
	private String getActivityTypeString(int code, ActivityFacility activityFacility) {
		String string = null;

		switch (code) {
//			case 0: string = "h"; break;	// no -> home
//			case 1: string = "w"; break;	// work -> work
//			case 2: string = "e"; break;	// study -> education
//			case 3: string = "s"; break;	// shopping -> shopping
//			case 4: string = "l"; break;	// other -> leisure

			case 1: // no -> home
				if (facilityContainsActivityType("home", activityFacility)) {
					string = "home";
					break;
				}
				else {
					log.warn("No home ActivityOption found! #" + ++activityNotSupportedCounter);
					string = "home";
					break;
				}

			case 2: // work -> work
				if (facilityContainsActivityType("work", activityFacility)) {
					string = "work";
					break;
				}
				else {
					log.warn("No work ActivityOption found! #" + ++activityNotSupportedCounter);
//					string = "work";
					break;
				}

			case 3: // study -> education
				if (facilityContainsActivityType("education_university", activityFacility)) {
					string = "education_university";
					break;
				}
				else if (facilityContainsActivityType("education_highschool", activityFacility)) {
					string = "education_highschool";
					break;
				}
				else if (facilityContainsActivityType("education_elementaryschool", activityFacility)) 	{
					string = "education_elementaryschool";
					break;
				}
				else {
					log.warn("No education ActivityOption found! #" + ++activityNotSupportedCounter);
//					string = "education";
					break;
				}

			case 4: // shopping -> shopping
				if (facilityContainsActivityType("shopping", activityFacility)) {
					string = "shopping";
					break;
				}
				else {
					log.warn("No shopping ActivityOption found! #" + ++activityNotSupportedCounter);
//					string = "shopping";
					break;
				}

			case 5: // other -> leisure
				if (facilityContainsActivityType("leisure", activityFacility)) {
					string = "leisure";
					break;
				}
				else {
					log.warn("No leisure ActivityOption found! #" + ++activityNotSupportedCounter);
//					string = "leisure";
					break;
				}

			default: log.warn("Unknown Activity Type Code! " + code); string = "";
		}

		return string;
	}

	private boolean facilityContainsActivityType(String string, ActivityFacility activityFacility) {
		for (ActivityOption activityOption : activityFacility.getActivityOptions().values()) {
			if(activityOption.getType().equalsIgnoreCase(string)) return true;
		}

		return false;
	}

	private int[] CTODCalculator(int CTOD) {
		int start = 0;
		int end = 0;

		switch (CTOD) {
			case 1: start = 1; end = 1; break;
			case 2: start = 1; end = 2; break;
			case 3: start = 1; end = 3; break;
			case 4: start = 1; end = 4; break;
			case 5: start = 1; end = 5; break;
			case 6: start = 2; end = 2; break;
			case 7: start = 2; end = 3; break;
			case 8: start = 2; end = 4; break;
			case 9: start = 2; end = 5; break;
			case 10: start = 3; end = 3; break;
			case 11: start = 3; end = 4; break;
			case 12: start = 3; end = 5; break;
			case 13: start = 4; end = 4; break;
			case 14: start = 4; end = 5; break;
			case 15: start = 5; end = 5; break;

			default: log.warn("Unknown CTOD Code!"); break;
		}

		return new int[]{start, end};
	}

	private double[] getCTODTimeWindow(int CTOD) {
//		MO	3:00 to 6:30
//		AM	6:30 to 8:30
//		MD	8:30 to 15:00
//		PM	15:00 to 20:00
//		EV	20:00 to 03:00

		double[] window = new double[2];

		switch (CTOD) {
			case 1: window = new double[]{3, 6.5}; break;	// MO - morning
			case 2: window = new double[]{6.5, 8.5}; break;	// AM - am peak
			case 3: window = new double[]{8.5, 15}; break;	// MD - midday
			case 4: window = new double[]{15, 20}; break;	// PM - pm peak
			case 5: window = new double[]{20, 3}; break;	// EV - evening
			default: log.warn("Unknown CTOD Code!"); window = new double[]{0, 24}; break;
		}

		double secsPerHour = 3600;
		window[0] = window[0] * secsPerHour;
		window[1] = window[1] * secsPerHour;
		return window;
	}
}