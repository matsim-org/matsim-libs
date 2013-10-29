/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivControlerListener.java
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

package playground.telaviv.controler;

import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.telaviv.config.TelAvivConfig;
import playground.telaviv.core.mobsim.qsim.agents.FilteredPopulation;

public class TelAvivControlerListener implements StartupListener {

	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "multimodalLegDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		Scenario scenario = controler.getScenario();
		Config config = controler.getConfig();
		
		// connect facilities to links
		new WorldConnectLocations(config).connectFacilitiesWithLinks(scenario.getActivityFacilities(), 
				(NetworkImpl) scenario.getNetwork());
		
		// analysis stuff
		
		// analyze only non-transit agents
		Set<Id> observedAgents = new HashSet<Id>();
		ObjectAttributes attributes = scenario.getPopulation().getPersonAttributes();
		Population observedPopulation = new FilteredPopulation();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Object attribute = attributes.getAttribute(person.getId().toString(), TelAvivConfig.externalTripType);
			if (attribute == null) {
				observedAgents.add(person.getId());
				observedPopulation.addPerson(person);
			}
		}
		
		CalcLegTimesHerbieListener calcLegTimes = new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, 
				LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME, observedPopulation);
		calcLegTimes.notifyStartup(event);
		controler.addControlerListener(calcLegTimes);

		LegDistanceDistributionWriter legDistanceDistribution = new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, 
				scenario.getNetwork(), observedPopulation);
		controler.addControlerListener(legDistanceDistribution);
		
		boolean createGraphs = true;
		
		String activitiesFileName = ActivitiesAnalyzer.defaultActivitiesFileName;
		Set<String> activityTypes = new TreeSet<String>(config.planCalcScore().getActivityTypes());
		ActivitiesAnalyzer activitiesAnalyzer = new ActivitiesAnalyzer(activitiesFileName, activityTypes, observedAgents,  createGraphs);
		activitiesAnalyzer.notifyStartup(event);
		controler.addControlerListener(activitiesAnalyzer);
		controler.getEvents().addHandler(activitiesAnalyzer);

		String tripsFileName = TripsAnalyzer.defaultTripsFileName;
		String durationsFileName = TripsAnalyzer.defaultDurationsFileName;
		Set<String> modes = new TreeSet<String>();
		modes.add(TransportMode.bike);
		modes.add(TransportMode.car);
		modes.add(TransportMode.pt);
		modes.add(TransportMode.ride);
		modes.add(TransportMode.walk);
		TripsAnalyzer tripsAnalyzer = new TripsAnalyzer(tripsFileName, durationsFileName, modes, observedAgents, createGraphs);
		tripsAnalyzer.notifyStartup(event);
		controler.addControlerListener(tripsAnalyzer);
		controler.getEvents().addHandler(tripsAnalyzer);
	}

}
