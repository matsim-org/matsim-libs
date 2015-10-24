/* *********************************************************************** *
 * project: org.matsim.*
 * Analyzer.java
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
package playground.johannes.studies.mz2005;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.mz2005.analysis.TrajectoryPlanBuilder;
import playground.johannes.mz2005.io.EscortData;
import playground.johannes.mz2005.utils.FacilityFromActivity;
import playground.johannes.mz2005.validate.RoundTrips;
import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * @author illenberger
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netreader = new MatsimNetworkReader(scenario);
		netreader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
//		reader.readFile("/Users/jillenberger/Desktop/run/plans.xml.gz");
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/07-09-2011/plans.sun.vcg.xml");
//		reader.readFile("/Users/jillenberger/Work/socialnets/locationChoice/horni/zh10PctEps.100.plans.xml");
		
		
		EscortData escortData = EscortData.read("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/07-09-2011/escort.sun.txt", scenario.getPopulation());
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader((ScenarioImpl) scenario);
		facReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.cg.xml");
		ActivityFacilities facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		
		FacilityFromActivity.createActivities(scenario.getPopulation(), facilities);
		
		TrajectoryPlanBuilder builder = new TrajectoryPlanBuilder();
		Set<Plan> plans = new HashSet<Plan>();
		
		SocialSparseGraphBuilder gbuilder = new SocialSparseGraphBuilder(CRSUtils.getCRS(21781));
		SocialSparseGraph graph = gbuilder.createGraph();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
			gbuilder.addVertex(graph, new SocialPerson(person), MatsimCoordUtils.coordToPoint(((Activity) person.getSelectedPlan().getPlanElements().get(0)).getCoord()));
		}
		
		
		
//		AllowLegMode remove = new AllowLegMode("car");
//		Set<Plan> planRemove = new HashSet<Plan>();
//		for(Plan plan : plans) {
//			if(!remove.validate(plan)) {
//				planRemove.add(plan);
//			}
//		}
//		
//		System.out.println("Removing " + planRemove.size() +" non-car plans of total " + plans.size() +" plans.");
//		for(Plan plan :planRemove) {
//			plans.remove(plan);
//		}
		
		RoundTrips rTrips = new RoundTrips();
		for(Plan plan : plans)
			rTrips.validate(plan);
		
		Set<Trajectory> trajectories = new HashSet<Trajectory>(builder.buildTrajectory(plans).values());
		
//		TrajectoryAnalyzerTask.overwriteStratification(20, 1);
		
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();
//		composite.addTask(new ActTypeShareTask());
//		composite.addTask(new PersonAgeTask());
//		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
//		composite.addTask(new TripDurationTask());
//		composite.addTask(new TripDistanceTask(facilities, WGS84DistanceCalculator.getInstance()));
//		composite.addTask(new TripDistanceTaskLeisure(facilities, WGS84DistanceCalculator.getInstance()));
////		composite.addTask(new TripDistanceTask(facilities, CartesianDistanceCalculator.getInstance()));
////		composite.addTask(new TripDistanceAccessibilityTask(graph, facilities));
//		composite.addTask(new ActivityChainsTask());
////		composite.addTask(new TripDistEscortTask(escortData, facilities, WGS84DistanceCalculator.getInstance()));
////		composite.addTask(new EscortsActivtyTypeTask(escortData));
//		composite.addTask(new DurationArrivalTimeTask());
//		composite.addTask(new ActivityLoadTask());
//		composite.addTask(new LegLoadTask());
//		composite.addTask(new DistanceArrivalTimeTask(new TripDistanceMean(null, facilities, WGS84DistanceCalculator.getInstance())));
////		composite.addTask(new TripAcceptanceProba(facilities, WGS84DistanceCalculator.getInstance()));
//		composite.addTask(new DepartureLoadTask());
//		composite.addTask(new TripDurationArrivalTime());
//		composite.addTask(new ModeShareArrivalTask());
				
//		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Desktop/run/");
		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/07-09-2011/sun/all/");
//		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Work/socialnets/locationChoice/horni/analysis/");
	}

}
