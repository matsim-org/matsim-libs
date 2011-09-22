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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ArrivalTimeTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripDistanceTask;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.mz2005.analysis.ActivityChainsTask;
import playground.johannes.mz2005.analysis.EscortsActivtyTypeTask;
import playground.johannes.mz2005.analysis.TrajectoryPlanBuilder;
import playground.johannes.mz2005.analysis.TripDistEscortTask;
import playground.johannes.mz2005.io.EscortData;
import playground.johannes.mz2005.utils.FacilityFromActivity;
import playground.johannes.mz2005.validate.AllowLegMode;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;


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
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
//		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/07-09-2011/plans.sun.xml");
		reader.readFile("/Users/jillenberger/Work/socialnets/locationChoice/output/10000/plans.xml.gz");
		
		
//		EscortData escortData = EscortData.read("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/07-09-2011/escort.sun.txt", scenario.getPopulation());
		
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		FacilityFromActivity.createActivities(scenario.getPopulation(), facilities);
		
		TrajectoryPlanBuilder builder = new TrajectoryPlanBuilder();
		Set<Plan> plans = new HashSet<Plan>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
		}
		
		
		
		AllowLegMode remove = new AllowLegMode("car");
		Set<Plan> planRemove = new HashSet<Plan>();
		for(Plan plan : plans) {
			if(!remove.validate(plan)) {
				planRemove.add(plan);
			}
		}
		for(Plan plan :planRemove) {
			plans.remove(plan);
		}
		
		Set<Trajectory> trajectories = new HashSet<Trajectory>(builder.buildTrajectory(plans).values());
		
		TrajectoryAnalyzerTask.overwriteStratification(30, 1);
		
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();
		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
		composite.addTask(new TripDistanceTask(facilities, WGS84DistanceCalculator.getInstance()));
//		composite.addTask(new TripDistanceAccessibilityTask(graph, facilities));
		composite.addTask(new ActivityChainsTask());
//		composite.addTask(new TripDistEscortTask(escortData, facilities, WGS84DistanceCalculator.getInstance()));
//		composite.addTask(new EscortsActivtyTypeTask(escortData));
		
//		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/07-09-2011/sun/car/");
		TrajectoryAnalyzer.analyze(trajectories, composite, "/Users/jillenberger/Work/socialnets/locationChoice/output/10000/");
	}

}
