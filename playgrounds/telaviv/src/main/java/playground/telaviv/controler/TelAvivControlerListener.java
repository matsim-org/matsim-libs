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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.DistanceDistribution;
import org.matsim.contrib.analysis.christoph.DistanceDistribution.DistributionClass;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.telaviv.config.TelAvivConfig;
import playground.telaviv.core.mobsim.qsim.agents.FilteredPopulation;

public class TelAvivControlerListener implements StartupListener {

	private final static Logger log = Logger.getLogger(TelAvivControlerListener.class);
	
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
		
		log.info("Total population size:\t" + scenario.getPopulation().getPersons().size());
		log.info("observed population size:\t" + observedPopulation.getPersons().size());
		
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

		String tripsFileName = controler.getControlerIO().getOutputFilename(TripsAnalyzer.defaultTripsFileName);
		String durationsFileName = controler.getControlerIO().getOutputFilename(TripsAnalyzer.defaultDurationsFileName);
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
		
		DistanceDistribution distanceDistribution;
		DistributionClass distributionClass;
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-home-work");
		distanceDistribution.addActivityCombination(distributionClass, "home", "work");
		distanceDistribution.addActivityCombination(distributionClass, "work", "home");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 100.0, 0);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 100.0, 200.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 200.0, 500.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, 200000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 200000.0, 500000.0, 10);
//		distanceDistribution.createAndAddDistanceBin(distributionClass, 500000.0, Double.MAX_VALUE, 10);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0009);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0012);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0072);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.0438);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.1006);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.2632);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.5150);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0681);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-shopping");
		distanceDistribution.addActivityCombination(distributionClass, "home", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "home");
		distanceDistribution.addActivityCombination(distributionClass, "work", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "work");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-leisure");
		distanceDistribution.addActivityCombination(distributionClass, "home", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "home");
		distanceDistribution.addActivityCombination(distributionClass, "work", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "work");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-overall");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0009);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0012);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0072);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.0438);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.1006);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.2632);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.5150);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0681);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), scenario.getPopulation(), 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("total_population-home-work");
		distanceDistribution.addActivityCombination(distributionClass, "home", "work");
		distanceDistribution.addActivityCombination(distributionClass, "work", "home");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0009);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0012);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0072);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.0438);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.1006);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.2632);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.5150);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0681);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);

		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), scenario.getPopulation(), 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("total_population-overall");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0009);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0012);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0072);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.0438);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.1006);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.2632);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.5150);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0681);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
	}

}
