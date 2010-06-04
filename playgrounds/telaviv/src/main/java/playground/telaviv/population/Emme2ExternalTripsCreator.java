/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2ExternalTripsCreator.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.Desires;

public class Emme2ExternalTripsCreator {

	private static final Logger log = Logger.getLogger(Emme2ExternalTripsCreator.class);
	
	private String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
	private String facilitiesFile = "../../matsim/mysimulations/telaviv/facilities/facilities.xml";
	private String externalTripsFile = "../../matsim/mysimulations/telaviv/population/external_trips.csv";
	private String outFile = "../../matsim/mysimulations/telaviv/population/external_plans_10.xml.gz";
	
	private Scenario scenario;
//	private ZoneMapping zoneMapping;
	private Random random = new Random(123456);
	private Network network;
	private ActivityFacilities activityFacilities; 
	
	/*
	 * We got Trips per Hour in the input file.
	 * The Period lasts from 06:00 to 09:00 therefore
	 * we have to multiply the number of trips by 3.
	 */
	private double periodDuration = 3;
	
	/*
	 * We use a 10% Scenario therefore we have to reduce
	 * the created Trips by a Factor of 10.
	 */
	private double scaleFactor = 0.1; 
	
	/*
	 * Trips:
	 * 0600 .. 0900
	 * 
	 * Distribution of the departure Times of the Trips:
	 * 0600 - 0630: 1/12
	 * 0630 - 0700: 2/12
	 * 0700 - 0730: 3/12
	 * 0730 - 0800: 3/12
	 * 0800 - 0830: 2/12
	 * 0830 - 0900:	1/12
	 */
	
	public static void main(String[] args)
	{
		new Emme2ExternalTripsCreator(new ScenarioImpl());
	}
	
	public Emme2ExternalTripsCreator(Scenario scenario)
	{		
		this.scenario = scenario;
		
		new MatsimNetworkReader(scenario).readFile(networkFile);
		network = scenario.getNetwork();
		log.info("Loading Network ... done");
		
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
		log.info("Loading Facilities ... done");
		
		log.info("Parsing external trips file...");
		List<Emme2ExternalTrip> externalTrips = new Emme2ExternalTripFileParser(externalTripsFile).readFile();
		log.info("done.");
		
		log.info("Creating MATSim external Trips...");
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		int idCounter = 0;
		for (Emme2ExternalTrip externalTrip : externalTrips)
		{
			int numOfTrips = (int)(periodDuration * Double.valueOf(externalTrip.numOfTrips));
			
			for (int i = 0; i < numOfTrips; i++)
			{
				Id id = scenario.createId("tta_" + String.valueOf(idCounter++));
				PersonImpl person = (PersonImpl)populationFactory.createPerson(id);
				
				setBasicParameters(person);		
				
				createAndAddInitialPlan(person, externalTrip);
				
				scenario.getPopulation().addPerson(person);
			}
		}	
		log.info("done.");
		
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), scaleFactor).write(outFile);
		System.out.println("done.");
	}
	
	/*
	 * Set some basic person parameters like age, sex, license and car availability.
	 */
	private void setBasicParameters(PersonImpl person)
	{
		person.setAge(100);
		person.setSex("m");
		person.setLicence("yes");
		person.setCarAvail("always");		
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
	public void createAndAddInitialPlan(PersonImpl person, Emme2ExternalTrip externalTrip)
	{
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		Desires desires = person.createDesires("");
		
		LegImpl leg;
		ActivityImpl activity;
		ActivityFacility activityFacility;

		int originNode = externalTrip.originNodeId;
		int destionationNode = externalTrip.destinationNodeId;
		
		Id originNodeId = scenario.createId(String.valueOf(originNode));
		Id destinationNodeId = scenario.createId(String.valueOf(destionationNode));
		
		Id originLinkId = selectLinkByStartNode(originNodeId);
		Id destinationLinkId = selectLinkByStartNode(destinationNodeId);
		
		double departureTime = getDepartureTime();
		
		/*
		 * create tta activity in origin zone
		 * create car leg from origin zone to destination zone
		 * create tta activity in destination zone 
		 */
		activity = (ActivityImpl) populationFactory.createActivityFromLinkId("tta", originLinkId);
		activity.setStartTime(0.0);
		activity.setDuration(departureTime);
		activity.setEndTime(departureTime);
		activityFacility = getActivityFacilityByLinkId(originLinkId);
		activity.setFacilityId(activityFacility.getId());
		activity.setCoord(activityFacility.getCoord());
		plan.addActivity(activity);
		
		leg = (LegImpl)populationFactory.createLeg(TransportMode.car);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(departureTime);
		plan.addLeg(leg);
		
		activity = (ActivityImpl) populationFactory.createActivityFromLinkId("tta", destinationLinkId);
		activity.setStartTime(departureTime);
		activityFacility = getActivityFacilityByLinkId(destinationLinkId);
		activity.setFacilityId(activityFacility.getId());
		activity.setCoord(activityFacility.getCoord());
		plan.addActivity(activity);
		
		/*
		 * Finally add a tta desire that has a duration of 86400 - all other activities.
		 */
		desires.accumulateActivityDuration("tta", 86400);
	}
	
	/*
	 * The link is selected randomly but the length of the links 
	 * is used to weight the probability.
	 */
	private Id selectLinkByStartNode(Id nodeId)
	{		
		Node startNode = network.getNodes().get(nodeId);
		List<Id> linkIds = new ArrayList<Id>();
		
		for (Link link : startNode.getOutLinks().values()) linkIds.add(link.getId());
		
		if (linkIds == null)
		{
			log.warn("startNode " + startNode.getId() + " has no outgoing Links!");
			return null;
		}
		
		double totalLength = 0;
		for (Id id : linkIds)
		{
			Link link = network.getLinks().get(id);
			totalLength = totalLength + link.getLength();
		}
		
		double[] probabilities = new double[linkIds.size()];
		double sumProbability = 0.0;
		for (int i = 0; i < linkIds.size(); i++)
		{
			Link link = network.getLinks().get(linkIds.get(i));
			double probability = link.getLength() / totalLength;
			probabilities[i] = sumProbability + probability;
			sumProbability = probabilities[i];
		}
		
		// if we have only one link we can return that one
		if (linkIds.size() == 1) return linkIds.get(0);
		
		// else find the right one
		double randomProbability = random.nextDouble();
		for (int i = 0; i <= linkIds.size(); i++)
		{
			if (randomProbability <= probabilities[i]) return linkIds.get(i);
		}
		return null;
	}
		
	private double getDepartureTime()
	{
		double d = random.nextDouble();
		
		d = d * 12;
		
		if (d < 1)       return 6.0 * 3600 + Math.round(d * 30 * 60);
		else if (d < 3)  return 6.5 * 3600 + Math.round((d - 1)/2 * 30 * 60);
		else if (d < 6)  return 7.0 * 3600 + Math.round((d - 3)/3 * 30 * 60);
		else if (d < 9)  return 7.5 * 3600 + Math.round((d - 6)/3 * 30 * 60);
		else if (d < 11) return 8.0 * 3600 + Math.round((d - 9)/2 * 30 * 60);
		else             return 8.5 * 3600 + Math.round((d - 11)/2 * 30 * 60);
	}
	
	/*
	 * We get the Id of a Link that is connected to an external Node.
	 */
	private ActivityFacility getActivityFacilityByLinkId(Id id)
	{	
		return activityFacilities.getFacilities().get(id);
	}
}
