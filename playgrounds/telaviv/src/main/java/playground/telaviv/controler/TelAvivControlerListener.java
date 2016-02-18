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

import java.io.File;
import java.io.IOException;
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
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.telaviv.analysis.ComparingBins;
import playground.telaviv.analysis.ComparingDistanceStats;
import playground.telaviv.config.TelAvivConfig;
import playground.telaviv.core.mobsim.qsim.agents.FilteredPopulation;
import playground.telaviv.counts.CountsCompareToCSV;

public class TelAvivControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(TelAvivControlerListener.class);
	
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "multimodalLegDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		MatsimServices controler = event.getServices();
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
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-to-work");
		distanceDistribution.addActivityCombination(distributionClass, "home", "work");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "work");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "work");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "work");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "work");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "work");
		distanceDistribution.addActivityCombination(distributionClass, "work", "work");
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
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.02716);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.01455);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.07104);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.24257);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.23367);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.23827);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.16873);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.00401);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.00000);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-to-home");
		distanceDistribution.addActivityCombination(distributionClass, "home", "home");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "home");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "home");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "home");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "home");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "home");
		distanceDistribution.addActivityCombination(distributionClass, "work", "home");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.03677);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.01760);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.08730);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.26805);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.24190);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.21389);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.13252);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.00197);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.00000);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-to-shopping");
		distanceDistribution.addActivityCombination(distributionClass, "home", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "shopping");
		distanceDistribution.addActivityCombination(distributionClass, "work", "shopping");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.09898);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.03182);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.13291);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.30254);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.19505);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.14969);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.08749);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.00152);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.00000);

		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-to-leisure");
		distanceDistribution.addActivityCombination(distributionClass, "home", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "leisure");
		distanceDistribution.addActivityCombination(distributionClass, "work", "leisure");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.09050);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.02717);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.10738);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.27293);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.19568);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.17923);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.12343);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.00368);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.00000);

		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-to-education");
		distanceDistribution.addActivityCombination(distributionClass, "home", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "home", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "home", "education_elementaryschool");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "leisure", "education_elementaryschool");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "education_university", "education_elementaryschool");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "education_highschool", "education_elementaryschool");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "education_elementaryschool", "education_elementaryschool");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "shopping", "education_elementaryschool");
		distanceDistribution.addActivityCombination(distributionClass, "work", "education_university");
		distanceDistribution.addActivityCombination(distributionClass, "work", "education_highschool");
		distanceDistribution.addActivityCombination(distributionClass, "work", "education_elementaryschool");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.04723);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.01432);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.07594);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.27513);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.24428);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.20137);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.13893);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.00281);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.00000);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), observedPopulation, 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("observed_population-overall");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0483);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0187);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0852);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.2660);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.2250);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.2133);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.1402);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0034);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
		
		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), scenario.getPopulation(), 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("total_population-home-work");
		distanceDistribution.addActivityCombination(distributionClass, "home", "work");
		distanceDistribution.addActivityCombination(distributionClass, "work", "home");
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

		distanceDistribution = new DistanceDistribution(scenario.getNetwork(), scenario.getPopulation(), 
				new MainModeIdentifierImpl(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
		controler.addControlerListener(distanceDistribution);
		distributionClass = distanceDistribution.createAndAddDistributionClass("total_population-overall");
		distanceDistribution.addMainMode(distributionClass, TransportMode.car);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 0.0, 500.0, 0.0483);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 500.0, 1000.0, 0.0187);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 1000.0, 2000.0, 0.0852);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 2000.0, 5000.0, 0.2660);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 5000.0, 10000.0, 0.2250);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 10000.0, 20000.0, 0.2133);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 20000.0, 50000.0, 0.1402);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 50000.0, 100000.0, 0.0034);
		distanceDistribution.createAndAddDistanceBin(distributionClass, 100000.0, Double.MAX_VALUE, 0.0);
		
		
		/*
		 * further analysis stuff for location choice
		 */
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		double analysisBinSize = dccg.getAnalysisBinSize();
		double analysisBoundary = dccg.getAnalysisBoundary();
		String idExclusion = dccg.getIdExclusion().toString();
		ActTypeConverter converter = new ActTypeConverter(false);
		Set<String> flexibleTypes = CollectionUtils.stringToSet(dccg.getFlexibleTypes());
  		for (String actType : flexibleTypes) {
  			String filename = TelAvivConfig.basePath + "/locationchoice/" + actType + "ReferenceShares.txt";
  			if (!new File(filename).exists()) {
  				log.warn("Input file containing reference shares for activity type " + actType + " was not found. Skipping activity type!");
  				continue;
  			}
  			
  			double[] referenceShares = ComparingBins.readReferenceShares(filename);
  			controler.addControlerListener(new ComparingDistanceStats(analysisBinSize, analysisBoundary, idExclusion, "best", actType, 
  					converter, TransportMode.car, referenceShares));
  			controler.addControlerListener(new ComparingDistanceStats(analysisBinSize, analysisBoundary, idExclusion, "selected", actType, 
  					converter, TransportMode.car, referenceShares));
  		}
  		
		analysisBinSize = 1000.0;
		analysisBoundary = 90000.0;
  		for (String actType : flexibleTypes) {
  			String filename = TelAvivConfig.basePath + "/locationchoice/" + actType + "ReferenceShares1000m.txt";
  			if (!new File(filename).exists()) {
  				log.warn("Input file containing reference shares for activity type " + actType + " was not found. Skipping activity type!");
  				continue;
  			}
  			
  			double[] referenceShares = ComparingBins.readReferenceShares(filename);
  			controler.addControlerListener(new ComparingDistanceStats(analysisBinSize, analysisBoundary, idExclusion, "best", actType, 
  					converter, TransportMode.car, referenceShares));
  			controler.addControlerListener(new ComparingDistanceStats(analysisBinSize, analysisBoundary, idExclusion, "selected", actType, 
  					converter, TransportMode.car, referenceShares));
  		}	
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
	
		String inputFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "countscompare.txt");
		String outputFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "countscompare.csv");
		
		runCountsCompare(inputFile, outputFile);
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String runId = event.getServices().getConfig().controler().getRunId();
		if (runId == null) runId = "";
		else if (runId.length() > 0) runId = runId + ".";
		
		int lastIter = event.getServices().getConfig().controler().getLastIteration();
		
		String inputFile = event.getServices().getControlerIO().getIterationFilename(lastIter, "countscompare.txt");
		String outputFile = event.getServices().getControlerIO().getIterationFilename(lastIter, "countscompare.csv");
		
		runCountsCompare(inputFile, outputFile);
	}

	private void runCountsCompare(String inputFile, String outputFile) {
		try {
			if (new File(inputFile).exists()) new CountsCompareToCSV(inputFile, outputFile);
			else log.warn("Counts input file countscompare.txt does not extist: " + inputFile + ". Cannot create output file countscompare.csv");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}