/* *********************************************************************** *
 * project: org.matsim.*
 * AnA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.coopsim;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.coopsim.analysis.ActTypeShareTask;
import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ArrivalTimeTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripDistanceTask;
import playground.johannes.coopsim.pysical.PseudoSim;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class PlansAnalyzer {

	public static void main(String args[]) throws IOException {
		Config config = ConfigUtils.createConfig();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run34/output/6000000/plans.xml.gz");
		
		SocialSparseGraphMLReader reader2 = new SocialSparseGraphMLReader();
		SocialGraph graph = reader2.readGraph("/Users/jillenberger/Work/socialnets/locationChoice/mcmc.backup/run336/output/20000000000/graph.graphml");
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.leisure.xml");
		
		FacilityValidator.generate(scenario.getActivityFacilities(), scenario.getNetwork(), graph);
		
		PseudoSim sim = new PseudoSim();
		
		Set<Plan> plans = new HashSet<Plan>();
		Set<Person> persons = new HashSet<Person>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
			persons.add(person);
		}
		
		EventsManager eventManager =  EventsUtils.createEventsManager();
		
		TrajectoryEventsBuilder builder = new TrajectoryEventsBuilder(persons);
		eventManager.addHandler(builder);
		
		TravelTime travelTime = new TravelTimeCalculator(scenario.getNetwork(), 900, 86400, new TravelTimeCalculatorConfigGroup());
		
		sim.run(plans, scenario.getNetwork(), travelTime, eventManager);
		
		Set<Trajectory> trajectories = builder.trajectories();
		
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();

		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
		composite.addTask(new TripDistanceTask(scenario.getActivityFacilities()));
		composite.addTask(new ActTypeShareTask());
		
		TrajectoryAnalyzerTask.overwriteStratification(30, 1);
		
		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Work/socialnets/locationChoice/analysis/run34/");
	}
}
