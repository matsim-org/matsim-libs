/* *********************************************************************** *
 * project: org.matsim.*
 * AlbatrossPopulationCreator.java
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

package playground.christoph.netherlands.population;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.Desires;

import playground.christoph.netherlands.zones.GetZoneConnectors;

public class AlbatrossPopulationCreator {

	private static final Logger log = Logger.getLogger(AlbatrossPopulationCreator.class);

	
	private static String populationFile = "../../matsim/mysimulations/netherlands/population/export-thursday2000-plans-clean.txt";
//	private static String populationFile = "../../matsim/mysimulations/netherlands/population/example-plans-Albatross.txt";
	private static String networkFile = "../../matsim/mysimulations/netherlands/network/network_with_connectors.xml.gz";
	private static String facilitiesFile = "../../matsim/mysimulations/netherlands/facilities/facilities.xml";
	private static String outFile = "../../matsim/mysimulations/netherlands/population/plans.xml.gz";

	private Scenario scenario;
	private ActivityFacilities activityFacilities;
	private Map<Integer, List<Id>> connectorLinksMapping;
//	private Emme2FacilitiesCreator facilitiesCreator;
	private Random random = new Random(123456);

	public static void main(String[] args) throws Exception {
		new AlbatrossPopulationCreator(new ScenarioImpl());
	}

	public AlbatrossPopulationCreator(Scenario scenario) throws Exception {
		this.scenario = scenario;
		log.info("Read Network File...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("done.");
		
		log.info("Getting Connector Links...");
		connectorLinksMapping = new GetZoneConnectors(scenario.getNetwork()).getMapping(); 
		log.info("done.");

		log.info("Reading facilities file...");
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
		log.info("done.");

		log.info("Parsing population file...");
		Map<String, AlbatrossPerson> personMap = new AlbatrossPersonFileParser(populationFile).readFile();
		log.info("done.");

		log.info("Creating MATSim population...");
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		for (Entry<String, AlbatrossPerson> entry : personMap.entrySet()) {
			Id id = scenario.createId(entry.getKey());
			AlbatrossPerson albatrossPerson = entry.getValue();
			
			PersonImpl person = (PersonImpl)populationFactory.createPerson(id);

			setBasicParameters(person, albatrossPerson);

			createAndAddInitialPlan(person, albatrossPerson);

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
	private void setBasicParameters(PersonImpl person, AlbatrossPerson albatrossPerson) {
		
		if (albatrossPerson.GEND == 0) person.setSex("m");
		else person.setSex("f");

		if (albatrossPerson.WSTAT == 0) person.setEmployed(false);
		else person.setEmployed(true);
		
		person.setAge(calcAge(albatrossPerson));
	}
	
	/*
	 * Create initial plan.
	 */
	public void createAndAddInitialPlan(PersonImpl person, AlbatrossPerson albatrossPerson) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		Desires desires = person.createDesires("");

		LegImpl leg;
		ActivityImpl activity;

		int homeZone = albatrossPerson.PPC.get(0);
		Id homeLinkId = this.selectLinkByZone(homeZone);
		Facility homeFacility = getActivityFacilityByLinkId(homeLinkId);

		/*
		 * We always have at least a home activity.
		 */
		activity = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLinkId);
		activity.setStartTime(0.0);
		// if the activity ends before "27:00" (End of the day in Albatross) there is a next activity
		double endTime = getTime(albatrossPerson.ET.get(0));
		if (endTime != 27 * 3600){
			activity.setDuration(endTime);
			activity.setEndTime(endTime);
		}
		activity.setFacilityId(homeFacility.getId());
		activity.setCoord(homeFacility.getCoord());
		plan.addActivity(activity);
		
		/*
		 * We start with the second entry - the first is being at home.
		 */
		for (int i = 1; i < albatrossPerson.ATYPE.size(); i++) {
			// Leg
			String transportMode = getTransportMode(albatrossPerson.MODE.get(i));
			leg = (LegImpl)populationFactory.createLeg(transportMode);
			leg.setDepartureTime(getTime(albatrossPerson.ET.get(i-1)));
			leg.setArrivalTime(getTime(albatrossPerson.BT.get(i)));
			leg.setTravelTime(leg.getArrivalTime() - leg.getDepartureTime());
			plan.addLeg(leg);
			
			// Activity
			int zone = albatrossPerson.PPC.get(i);
			Id linkId = null;
			Facility facility = null;
			// if it is a home activity we already know the home facility
			if (albatrossPerson.ATYPE.get(i) == 9) {
				linkId = homeLinkId;
				facility = homeFacility;
			} else {
				linkId = this.selectLinkByZone(zone);
				facility = getActivityFacilityByLinkId(linkId);				
			}
 
			String activityType = getActivityType(albatrossPerson.ATYPE.get(i));
			activity = (ActivityImpl) populationFactory.createActivityFromLinkId(activityType, linkId);
			activity.setStartTime(getTime(albatrossPerson.BT.get(i)));
			// if the activity ends before "27:00" (End of the day in Albatross) there is a next activity
			double et = getTime(albatrossPerson.ET.get(i));
			if (et != 27 * 3600){
				activity.setDuration(et - activity.getStartTime());
				activity.setEndTime(et);
			}
			activity.setFacilityId(facility.getId());
			activity.setCoord(facility.getCoord());
			// if it is not a home activity
			if (albatrossPerson.ATYPE.get(i) != 9 && activity.getDuration() > 0.0) desires.accumulateActivityDuration(activity.getType(), activity.getDuration());
			plan.addActivity(activity);
		}	

		/*
		 * Finally add a home desire that has a duration of 86400 - all other activities.
		 * If only home activities are performed, we add a 24h at home desire
		 */
//		if (plan.getPlanElements().size() == 1) desires.accumulateActivityDuration("home", 86400.0);
		if (desires.getActivityDurations() == null) desires.accumulateActivityDuration("home", 86400.0);
		else {
			double otherDurations = 0.0;
			for (double duration : desires.getActivityDurations().values()) {
				otherDurations = otherDurations + duration;
			}
			if (otherDurations < 86400) desires.accumulateActivityDuration("home", 86400 - otherDurations);			
		}
	}

	/*
	 * The link is selected randomly but the length of the links
	 * is used to weight the probability.
	 */
	private Id selectLinkByZone(int TAZ) {
//		return new IdImpl("0");
//		List<Id> linkIds = facilitiesCreator.getLinkIdsInZoneForFacilites(TAZ);
		List<Id> linkIds = connectorLinksMapping.get(TAZ);
		
		if (linkIds == null) {
			log.warn("Zone " + TAZ + " has no mapped Links!");
			return null;
		}

		int rand = random.nextInt(linkIds.size());
		return linkIds.get(rand);
		
		/*
		 * Selection depending on the length of the links
		 */
//		double totalLength = 0;
//		for (Id id : linkIds) {
//			Link link = zoneMapping.getNetwork().getLinks().get(id);
//			totalLength = totalLength + link.getLength();
//		}
//
//		double[] probabilities = new double[linkIds.size()];
//		double sumProbability = 0.0;
//		for (int i = 0; i < linkIds.size(); i++) {
//			Link link = zoneMapping.getNetwork().getLinks().get(linkIds.get(i));
//			double probability = link.getLength() / totalLength;
//			probabilities[i] = sumProbability + probability;
//			sumProbability = probabilities[i];
//		}
//
//		double randomProbability = random.nextDouble();
//		for (int i = 0; i < linkIds.size() - 1; i++) {
//			if (randomProbability <= probabilities[i + 1]) return linkIds.get(i);
//		}
//		return null;
	}

	/*
	 * We have one ActivityFacility per Links that has the same
	 * Id as the Link itself.
	 */
	private Facility getActivityFacilityByLinkId(Id id) {
		return activityFacilities.getFacilities().get(id);
	}

	private int calcAge(AlbatrossPerson albatrossPerson) {
		
		int code = albatrossPerson.AGEP;
		
		int min = 0;
		int max = 0;
		
		if (code == 0) {
			min = 18;
			max = 35;
		} else if (code == 1) {
			min = 35;
			max = 55;
		} else if (code == 2) {
			min = 55;
			max = 65;
		} else if (code == 3) {
			min = 65;
			max = 75;
		} else if (code == 4) {
			min = 75;
			max = 100;
		}
		
		int age = min + random.nextInt(max - min);
		
		return age;
	}
	
	private String getActivityType(int code) {
		switch (code) {
			case 0: return "work";
			case 1: return "business";
			case 2: return "brget";
			case 3: return "shop1";
			case 4: return "shopn";
			case 5: return "service";
			case 6: return "social";
			case 7: return "leisure";
			case 8: return "touring";
			case 9: return "home";
			default: log.warn("Unknown Activity Type Code! " + code); return "unknown";
		}
	}
	
	/*
	 * Coding is like:
	 * 300 means 3:00
	 * 1200 means 12:00
	 */
	private double getTime(int time) {
		int hours = (int)Math.floor(time / 100);
		int minutes = time - hours * 100;
		
		return hours * 3600 + minutes * 60;
	}
	
	private String getTransportMode(int code) {
		switch (code) {
			case 0: return TransportMode.car;
			case 1: 
				// 50:50 chance for walk / bike
				double rand = random.nextDouble();
				if (rand < 0.5) return TransportMode.walk;
				else return TransportMode.bike;
			case 2: return TransportMode.pt;
			case 3: return TransportMode.ride;
			default: log.warn("Unknown Transport Mode Code! " + code ); return null;
		}
	}
}
