/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2PopulationCreator.java
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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.population.Desires;

import playground.telaviv.facilities.Emme2FacilitiesCreator;
import playground.telaviv.zones.ZoneMapping;

public class Emme2PopulationCreator {

	private static final Logger log = Logger.getLogger(Emme2PopulationCreator.class);

	private String populationFile = "../../matsim/mysimulations/telaviv/population/PB1000_10.txt";
	private String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
	private String facilitiesFile = "../../matsim/mysimulations/telaviv/facilities/facilities.xml";
	private String outFile = "../../matsim/mysimulations/telaviv/population/internal_plans_10.xml.gz";

	private Scenario scenario;
	private ActivityFacilities activityFacilities;
	private ZoneMapping zoneMapping;
	private Emme2FacilitiesCreator facilitiesCreator;
	private Random random = new Random(123456);

	/*
	 * Opening Times:
	 * home 0..24
	 * leisure/other 6..22
	 * school 8..14
	 * university 9..18
	 * work 8..18
	 * shop 9..19
	 */

	public static void main(String[] args)
	{
		new Emme2PopulationCreator(new ScenarioImpl());
	}

	public Emme2PopulationCreator(Scenario scenario)
	{
		this.scenario = scenario;
		log.info("Read Network File...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("done.");
		
		log.info("Creating zone mapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84")); 
		log.info("done.");

		log.info("Creating FacilitiesCreator...");
		facilitiesCreator = new Emme2FacilitiesCreator(scenario, zoneMapping);
		log.info("done.");

		log.info("Reading facilities file...");
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
		log.info("done.");

		log.info("Parsing population file...");
		Map<Integer, Emme2Person> personMap = new Emme2PersonFileParser(populationFile).readFile();
		log.info("done.");

		log.info("Creating MATSim population...");
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		for (Entry<Integer, Emme2Person> entry : personMap.entrySet())
		{
			Id id = scenario.createId(String.valueOf(entry.getKey()));
			Emme2Person emme2Person = entry.getValue();

			PersonImpl person = (PersonImpl)populationFactory.createPerson(id);

			setBasicParameters(person, emme2Person);

			createAndAddInitialPlan(person, emme2Person);

			scenario.getPopulation().addPerson(person);
		}
		log.info("done.");

		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outFile);
		log.info("done.");
	}

	/*
	 * Set some basic person parameters like age, sex, license and car availability.
	 */
	private void setBasicParameters(PersonImpl person, Emme2Person emme2Person)
	{
		person.setAge(emme2Person.AGE);

		if (emme2Person.GENDER == 1) person.setSex("m");
		else person.setSex("f");

		if (emme2Person.LICENSE == 1)
		{
			person.setLicence("yes");
			if (emme2Person.NUMVEH == 0) person.setCarAvail("never");
			else if (emme2Person.NUMVEH >= emme2Person.HHLICENSES) person.setCarAvail("always");
			else person.setCarAvail("sometimes");
		}
		else
		{
			person.setLicence("no");
			person.setCarAvail("sometimes");
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
	public void createAndAddInitialPlan(PersonImpl person, Emme2Person emme2Person)
	{
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
//		RouteFactory routeFactory = new GenericRouteFactory();

		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		Desires desires = person.createDesires("");

		LegImpl leg;
		ActivityImpl activity;
//		Route route;
		String transportMode;
		ActivityFacility activityFacility;

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


		boolean hasPrimaryActivity = zoneMapping.zoneExists(primaryMainActivityZone);
		boolean hasSecondaryActivity = zoneMapping.zoneExists(secondaryMainActivityZone);

		Id homeLinkId;
		Id previousActivityLinkId;
		double time = 0.0;

		/*
		 * We always have at least a home activity.
		 *
		 * If the zone of the primary and secondary activity is
		 * not valid the persons stays at home the whole day.
		 */
		homeLinkId = selectLinkByZone(homeZone);
		activity = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLinkId);
		activity.setStartTime(0.0);

		if (hasPrimaryActivity)
		{
			activity.setDuration(emme2Person.START_1);
			activity.setEndTime(emme2Person.START_1);
			time = time + emme2Person.START_1;
		}
		else if (hasSecondaryActivity)
		{
			activity.setDuration(emme2Person.START_2);
			activity.setEndTime(emme2Person.START_2);
			time = time + emme2Person.START_2;
		}
		else
		{
			// It is the only Activity in the plan so we don't set an endtime.
		}

		activityFacility = activityFacilities.getFacilities().get(homeLinkId);
		activity.setFacilityId(activityFacility.getId());
		activity.setCoord(activityFacility.getCoord());
//		desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
		plan.addActivity(activity);

		// If we have no activity chains we have nothing left to do.
		if (!hasPrimaryActivity && !hasSecondaryActivity) return;

		previousActivityLinkId = homeLinkId;
		transportMode = getPrimaryTransportMode(primaryMainModePreActivity);

		/*
		 * Primary Activity
		 */
		if (hasPrimaryActivity)
		{
			/*
			 * If we have an Activity before the primary Activity
			 */
			if (primaryPreStop)
			{
				Id primaryPreLinkId = selectLinkByZone(primaryPreStopActivityZone);

				leg = (LegImpl)populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, primaryPreLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				activity = (ActivityImpl) populationFactory.createActivityFromLinkId(getActivityTypeString(primaryPreStopActivityType, getActivityFacilityByLinkId(primaryPreLinkId)), primaryPreLinkId);
				activity.setStartTime(time);
				activity.setDuration(emme2Person.DUR_1_BEF);
				activity.setEndTime(time + emme2Person.DUR_1_BEF);
				activityFacility = activityFacilities.getFacilities().get(primaryPreLinkId);
				activity.setFacilityId(activityFacility.getId());
				activity.setCoord(activityFacility.getCoord());
				desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
				plan.addActivity(activity);

				previousActivityLinkId = primaryPreLinkId;
				time = time + emme2Person.DUR_1_BEF;
			}

			/*
			 * Primary Activity
			 */
			Id primaryLinkId = selectLinkByZone(primaryMainActivityZone);

			leg = (LegImpl)populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, primaryLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			activity = (ActivityImpl) populationFactory.createActivityFromLinkId(getActivityTypeString(primaryMainActivityType, getActivityFacilityByLinkId(primaryLinkId)), primaryLinkId);
			activity.setStartTime(time);
			activity.setDuration(emme2Person.DUR_1_MAIN);
			activity.setEndTime(time + emme2Person.DUR_1_MAIN);
			activityFacility = activityFacilities.getFacilities().get(primaryLinkId);
			activity.setFacilityId(activityFacility.getId());
			activity.setCoord(activityFacility.getCoord());
			desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);

			previousActivityLinkId = primaryLinkId;
			transportMode = getSwitchedTransportMode(primaryMainModeSwitch, transportMode);
			time = time + emme2Person.DUR_1_MAIN;

			/*
			 * If we have an Activity after the primary Activity
			 */
			if (primaryPostStop)
			{
				Id primaryPostLinkId = selectLinkByZone(primaryPostStopActivityZone);

				leg = (LegImpl)populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, primaryPostLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				activity = (ActivityImpl) populationFactory.createActivityFromLinkId(getActivityTypeString(primaryPostStopActivityType, getActivityFacilityByLinkId(primaryPostLinkId)), primaryPostLinkId);
				activity.setStartTime(time);
				activity.setDuration(emme2Person.DUR_1_AFT);
				activity.setEndTime(time + emme2Person.DUR_1_AFT);
				activityFacility = activityFacilities.getFacilities().get(primaryPostLinkId);
				activity.setFacilityId(activityFacility.getId());
				activity.setCoord(activityFacility.getCoord());
				desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
				plan.addActivity(activity);

				previousActivityLinkId = primaryPostLinkId;
				time = time + emme2Person.DUR_1_AFT;
			}

			leg = (LegImpl)populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, homeLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			activity = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLinkId);
			activity.setStartTime(time);

			/*
			 * If we have a secondary activity we have to calculate and set
			 * the duration of the home activity at the end of the main
			 * activity trip.
			 */
			if (hasSecondaryActivity)
			{
				activity.setDuration(emme2Person.START_2 - time);
				activity.setEndTime(emme2Person.START_2);
			}
			activityFacility = activityFacilities.getFacilities().get(homeLinkId);
			activity.setFacilityId(activityFacility.getId());
			activity.setCoord(activityFacility.getCoord());
//			desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);
		}


		/*
		 * Secondary Activity
		 */
		if (hasSecondaryActivity)
		{
			time = emme2Person.START_2;
			homeLinkId = selectLinkByZone(homeZone);

			previousActivityLinkId = homeLinkId;
			transportMode = getSecondaryTransportMode(secondaryMainMode);

			/*
			 * If we have an Activity before the secondary Activity
			 */
			if (secondaryPreStop)
			{
				Id secondaryPreLinkId = selectLinkByZone(secondaryPreStopActivityZone);

				leg = (LegImpl)populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, secondaryPreLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				activity = (ActivityImpl) populationFactory.createActivityFromLinkId(getActivityTypeString(secondaryPreStopActivityType, getActivityFacilityByLinkId(secondaryPreLinkId)), secondaryPreLinkId);
				activity.setStartTime(time);
				activity.setDuration(emme2Person.DUR_2_BEF);
				activity.setEndTime(time + emme2Person.DUR_2_BEF);
				activityFacility = activityFacilities.getFacilities().get(secondaryPreLinkId);
				activity.setFacilityId(activityFacility.getId());
				activity.setCoord(activityFacility.getCoord());
				desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
				plan.addActivity(activity);

				previousActivityLinkId = secondaryPreLinkId;
				time = time + emme2Person.DUR_2_BEF;
			}

			/*
			 * Secondary Activity
			 */
			Id secondaryLinkId = selectLinkByZone(secondaryMainActivityZone);

			leg = (LegImpl)populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, secondaryLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			activity = (ActivityImpl) populationFactory.createActivityFromLinkId(getActivityTypeString(secondaryMainActivityType, getActivityFacilityByLinkId(secondaryLinkId)), secondaryLinkId);
			activity.setStartTime(time);
			activity.setDuration(emme2Person.DUR_2_MAIN);
			activity.setEndTime(time + emme2Person.DUR_2_MAIN);
			activityFacility = activityFacilities.getFacilities().get(secondaryLinkId);
			activity.setFacilityId(activityFacility.getId());
			activity.setCoord(activityFacility.getCoord());
			desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);

			previousActivityLinkId = secondaryLinkId;
			time = time + emme2Person.DUR_2_MAIN;

			/*
			 * If we have an Activity after the secondary Activity
			 */
			if (secondaryPostStop)
			{
				Id secondaryPostLinkId = selectLinkByZone(secondaryPostStopActivityZone);

				leg = (LegImpl)populationFactory.createLeg(transportMode);
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
//				route = routeFactory.createRoute(previousActivityLinkId, secondaryPostLinkId);
//				leg.setRoute(route);
				plan.addLeg(leg);

				activity = (ActivityImpl) populationFactory.createActivityFromLinkId(getActivityTypeString(secondaryPostStopActivityType, getActivityFacilityByLinkId(secondaryPostLinkId)), secondaryPostLinkId);
				activity.setStartTime(time);
				activity.setDuration(emme2Person.DUR_2_AFT);
				activity.setEndTime(time + emme2Person.DUR_2_AFT);
				activityFacility = activityFacilities.getFacilities().get(secondaryPostLinkId);
				activity.setFacilityId(activityFacility.getId());
				activity.setCoord(activityFacility.getCoord());
				desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
				plan.addActivity(activity);

				previousActivityLinkId = secondaryPostLinkId;
				time = time + emme2Person.DUR_2_AFT;
			}

			leg = (LegImpl)populationFactory.createLeg(transportMode);
			leg.setDepartureTime(time);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(time);
//			route = routeFactory.createRoute(previousActivityLinkId, homeLinkId);
//			leg.setRoute(route);
			plan.addLeg(leg);

			/*
			 * It is the last Activity of the plan so we don't set an end time.
			 */
			activity = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLinkId);
			activity.setStartTime(time);
			activityFacility = activityFacilities.getFacilities().get(homeLinkId);
			activity.setFacilityId(activityFacility.getId());
			activity.setCoord(activityFacility.getCoord());
//			desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);


			/*
			 * Finally add a home desire that has a duration of 86400 - all other activities.
			 */
			double otherDurations = 0.0;
			for (double duration : desires.getActivityDurations().values())
			{
				otherDurations = otherDurations + duration;
			}
			if (otherDurations < 86400) desires.accumulateActivityDuration("home", 86400 - otherDurations);
		}
	}

	/*
	 * The link is selected randomly but the length of the links
	 * is used to weight the probability.
	 */
	private Id selectLinkByZone(int TAZ)
	{
		List<Id> linkIds = facilitiesCreator.getLinkIdsInZoneForFacilites(TAZ);

		if (linkIds == null)
		{
			log.warn("Zone " + TAZ + " has no mapped Links!");
			return null;
		}

		double totalLength = 0;
		for (Id id : linkIds)
		{
			Link link = zoneMapping.getNetwork().getLinks().get(id);
			totalLength = totalLength + link.getLength();
		}

		double[] probabilities = new double[linkIds.size()];
		double sumProbability = 0.0;
		for (int i = 0; i < linkIds.size(); i++)
		{
			Link link = zoneMapping.getNetwork().getLinks().get(linkIds.get(i));
			double probability = link.getLength() / totalLength;
			probabilities[i] = sumProbability + probability;
			sumProbability = probabilities[i];
		}

		double randomProbability = random.nextDouble();
		for (int i = 0; i < linkIds.size() - 1; i++)
		{
			if (randomProbability <= probabilities[i + 1]) return linkIds.get(i);
		}
		return null;
	}

	/*
	 * We have one ActivityFacility per Links that has the same
	 * Id as the Link itself.
	 */
	private ActivityFacility getActivityFacilityByLinkId(Id id)
	{
		return activityFacilities.getFacilities().get(id);
	}

	private String getPrimaryTransportMode(int code)
	{
		String transportMode = null;
		switch (code)
		{
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

	private String getSwitchedTransportMode(int code, String currentTransportMode)
	{
		switch (code)
		{
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
				log.warn("Unknown Transport Mode Change Code! " + code + ", use bus as TransportMode.");
//				currentTransportMode = TransportMode.bus; break;
				currentTransportMode = TransportMode.pt; break;
		}

		return currentTransportMode;
	}

	private String getSecondaryTransportMode(int code)
	{
		String transportMode = null;
		switch (code)
		{
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
	private String getActivityTypeString(int code, ActivityFacility activityFacility)
	{
		String string;

		switch (code)
		{
//			case 0: string = "h"; break;	// no -> home
//			case 1: string = "w"; break;	// work -> work
//			case 2: string = "e"; break;	// study -> education
//			case 3: string = "s"; break;	// shopping -> shopping
//			case 4: string = "l"; break;	// other -> leisure

			case 1: // no -> home
				if (facilityContainsActivityType("home", activityFacility))
				{
					string = "home";
					break;
				}
				else
				{
					log.warn("No home ActivityOption found!" + ++activityNotSupportedCounter);
					string = "home";
					break;
				}

			case 2: // work -> work
				if (facilityContainsActivityType("work", activityFacility))
				{
					string = "work";
					break;
				}
				else
				{
					log.warn("No work ActivityOption found!" + ++activityNotSupportedCounter);
					string = "work";
					break;
				}

			case 3: // study -> education
				if (facilityContainsActivityType("education_university", activityFacility))
				{
					string = "education_university";
					break;
				}
				else if (facilityContainsActivityType("education_highschool", activityFacility))
				{
					string = "education_highschool";
					break;
				}
				else if (facilityContainsActivityType("education_elementaryschool", activityFacility))
				{
					string = "education_elementaryschool";
					break;
				}
				else
				{
					log.warn("No education ActivityOption found!" + ++activityNotSupportedCounter);
					string = "education";
					break;
				}

			case 4: // shopping -> shopping
				if (facilityContainsActivityType("shopping", activityFacility))
				{
					string = "shopping";
					break;
				}
				else
				{
					log.warn("No shopping ActivityOption found!" + ++activityNotSupportedCounter);
					string = "shopping";
					break;
				}

			case 5: // other -> leisure
				if (facilityContainsActivityType("leisure", activityFacility))
				{
					string = "leisure";
					break;
				}
				else
				{
					log.warn("No leisure ActivityOption found!" + ++activityNotSupportedCounter);
					string = "leisure";
					break;
				}

			default: log.warn("Unknown Activity Type Code! " + code); string = "";
		}

		return string;
	}

	private boolean facilityContainsActivityType(String string, ActivityFacility activityFacility)
	{
		for (ActivityOption activityOption : activityFacility.getActivityOptions().values())
		{
			if(activityOption.getType().equalsIgnoreCase(string)) return true;
		}

		return false;
	}

	private int[] CTODCalculator(int CTOD)
	{
		int start = 0;
		int end = 0;

		switch (CTOD)
		{
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

	private double[] getCTODTimeWindow(int CTOD)
	{
//		MO	3:00 to 6:30
//		AM	6:30 to 8:30
//		MD	8:30 to 15:00
//		PM	15:00 to 20:00
//		EV	20:00 to 03:00

		double[] window = new double[2];

		switch (CTOD)
		{
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
