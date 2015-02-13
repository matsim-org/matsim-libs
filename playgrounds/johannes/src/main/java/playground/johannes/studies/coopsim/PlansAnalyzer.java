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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.johannes.coopsim.analysis.ActTypeShareTask;
import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ActivityLoadTask;
import playground.johannes.coopsim.analysis.AgeTripCorrelationTask;
import playground.johannes.coopsim.analysis.ArrivalTimeTask;
import playground.johannes.coopsim.analysis.CoordinationComplexityTask;
import playground.johannes.coopsim.analysis.DesiredTimeDiffTask;
import playground.johannes.coopsim.analysis.DistanceArrivalTimeTask;
import playground.johannes.coopsim.analysis.DistanceVisitorsTask;
import playground.johannes.coopsim.analysis.DurationArrivalTimeTask;
import playground.johannes.coopsim.analysis.GenderTripCorrelationTask;
import playground.johannes.coopsim.analysis.JointActivityTask;
import playground.johannes.coopsim.analysis.LegLoadTask;
import playground.johannes.coopsim.analysis.ScoreTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripDistanceAccessibilityTask;
import playground.johannes.coopsim.analysis.TripDistanceDegreeTask;
import playground.johannes.coopsim.analysis.TripDistanceMean;
import playground.johannes.coopsim.analysis.TripGeoDistanceTask;
import playground.johannes.coopsim.analysis.TripDurationArrivalTime;
import playground.johannes.coopsim.analysis.TripDurationTask;
import playground.johannes.coopsim.analysis.TripPurposeShareTask;
import playground.johannes.coopsim.analysis.VisitorsAccessibilityTask;
import playground.johannes.coopsim.eval.ActivityEvaluator2;
import playground.johannes.coopsim.eval.EvalEngine;
import playground.johannes.coopsim.eval.EvaluatorComposite;
import playground.johannes.coopsim.eval.JointActivityEvaluator2;
import playground.johannes.coopsim.eval.LegEvaluator;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.ParallelPseudoSim;
import playground.johannes.coopsim.pysical.PhysicalEngine;
import playground.johannes.coopsim.pysical.PseudoSim;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;
import playground.johannes.studies.mz2005.TripAcceptanceProba;

/**
 * @author illenberger
 *
 */
public class PlansAnalyzer {

	public static void main(String args[]) throws Throwable {
		Config config = ConfigUtils.createConfig();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run267/tasks/5/output/5200000/plans.xml.gz");
		
		SocialSparseGraphMLReader reader2 = new SocialSparseGraphMLReader();
		SocialGraph graph = reader2.readGraph("/Users/jillenberger/Work/socialnets/locationChoice/mcmc.backup/run340/output/60000000000/graph.graphml", scenario.getPopulation());
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.cg.xml");
		
//		Map<SocialVertex, ActivityDesires> vertexDesires = ActivityDesires.read(graph, "/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/leisure/runs/run246/desires.xml");
//		Map<Person, ActivityDesires> personDesires = new HashMap<Person, ActivityDesires>();
//		for(Entry<SocialVertex, ActivityDesires> entry : vertexDesires.entrySet()) {
//			personDesires.put(entry.getKey().getPerson().getPerson(), entry.getValue());
//		}
		
		FacilityValidator.generate(scenario.getActivityFacilities(), (NetworkImpl) scenario.getNetwork(), graph);
		
//		ParallelPseudoSim sim = new ParallelPseudoSim(1);
		PhysicalEngine engine = new PhysicalEngine(scenario.getNetwork(), 3.0);
		
		Set<Plan> plans = new HashSet<Plan>();
		Set<Person> persons = new HashSet<Person>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
			persons.add(person);
		}
		
		EventsManager eventManager =  EventsUtils.createEventsManager();
		
		TrajectoryEventsBuilder builder = new TrajectoryEventsBuilder(persons);
		eventManager.addHandler(builder);
		
//		TravelTime travelTime = new TravelTimeCalculator(scenario.getNetwork(), 900, 86400, new TravelTimeCalculatorConfigGroup());
		
		VisitorTracker tracker = engine.getVisitorTracker();
//		eventManager.addHandler(tracker);
//		tracker.reset(0);
		
//		sim.run(plans, scenario.getNetwork(), travelTime, eventManager);
		engine.run(plans, eventManager);
		
		Set<Trajectory> trajectories = builder.trajectories();
		
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();

		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
		composite.addTask(new TripGeoDistanceTask(scenario.getActivityFacilities()));
		composite.addTask(new TripDurationTask());
		composite.addTask(new ActTypeShareTask());
		composite.addTask(new JointActivityTask(graph, tracker));
		composite.addTask(new ActivityLoadTask());
		composite.addTask(new DurationArrivalTimeTask());
		composite.addTask(new LegLoadTask());
//		composite.addTask(new ScoreTask());
//		composite.addTask(new VisitorsAccessibilityTask(tracker, graph));
		composite.addTask(new DistanceVisitorsTask(tracker, graph, scenario.getActivityFacilities()));
//		composite.addTask(new CoordinationComplexityTask(tracker, personDesires, graph));
//		composite.addTask(new TripAcceptanceProba(scenario.getActivityFacilities(), CartesianDistanceCalculator.getInstance()));
		composite.addTask(new TripDistanceDegreeTask(graph, scenario.getActivityFacilities()));
		composite.addTask(new AgeTripCorrelationTask(graph, tracker));
		composite.addTask(new GenderTripCorrelationTask(graph, tracker));
//		composite.addTask(new TripDistanceAccessibilityTask(graph, scenario.getActivityFacilities()));
		composite.addTask(new TripPurposeShareTask());
		composite.addTask(new DistanceArrivalTimeTask(new TripDistanceMean(null, scenario.getActivityFacilities(), CartesianDistanceCalculator.getInstance())));
		composite.addTask(new TripDurationArrivalTime());
		
//		EvaluatorComposite evaluator = new EvaluatorComposite();
//		evaluator.addComponent(new JointActivityEvaluator2(10, tracker, graph, 0.2, 1, 0.2, 0));
//		JointActivityEvaluator2.startLogging();
//		EvalEngine eval = new EvalEngine(evaluator);
//		eval.evaluate(trajectories);
		
		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Work/socialnets/locationChoice/analysis/run267/5/");
		
		engine.finalize();
	}
}
