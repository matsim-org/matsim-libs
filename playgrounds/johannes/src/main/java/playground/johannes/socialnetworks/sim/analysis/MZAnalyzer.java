/* *********************************************************************** *
 * project: org.matsim.*
 * MZAnalyzer.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.sim.gis.ActDistCoordCalculator;
import playground.johannes.socialnetworks.sim.gis.ActDistFacilityCalculator;
import playground.johannes.socialnetworks.sim.gis.ActivityDistanceCalculator;
import playground.johannes.socialnetworks.sim.gis.MatsimCoordUtils;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;

/**
 * @author illenberger
 *
 */
public class MZAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/plans/plans.sun.car.xml");
//		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/plans/plans.wday.aggacts.xml");
//		reader.readFile("/Users/jillenberger/Work/socialnets/locationChoice/data/plans.sun.14473.xml");
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader((ScenarioImpl) scenario);
		facReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.leisure.xml");
		
		PlansAnalyzerTaskComposite composite = new PlansAnalyzerTaskComposite();
		composite.addTask(new ActivityChainsTask());
		
		TrajectoryPlanBuilder builder = new TrajectoryPlanBuilder();
		Set<Plan> plans = new HashSet<Plan>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
		}
		Map<Plan, Trajectory> trajectories = builder.buildTrajectory(plans);
		
		TrajectoryAnalyzerTaskComposite tComposite = new TrajectoryAnalyzerTaskComposite();
		tComposite.addTask(new LegDurationTask());
//		tComposite.addTask(new ActivityDistanceTask(scenario.getNetwork(), 21781, new CartesianDistanceCalculator()));
//		tComposite.addTask(new ActivityDistanceTask(null, 4326, new OrthodromicDistanceCalculator()));
		tComposite.addTask(new ActivityLoadTask());
		tComposite.addTask(new ActivityDurationTask());
		tComposite.addTask(new ActivityStartTimeTask());
		tComposite.addTask(new LegLoadTask());
//		tComposite.addTask(new TripAcceptanceTask(((ScenarioImpl) scenario).getActivityFacilities()));
		System.out.println("Num persons: " + scenario.getPopulation().getPersons().size());
//		PlansAnalyzer.analyzeSelectedPlans(scenario.getPopulation(), trajectories, composite, "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/aggacts/sun/");
		
//		PlansAnalyzer.analyzeSelectedPlans(scenario.getPopulation(), composite, "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/aggacts/wday/");
//		PlansAnalyzer.analyzeSelectedPlans(scenario.getPopulation(), trajectories, composite, "/Users/jillenberger/Work/socialnets/locationChoice/data/plans.init/");
		
		SocialSparseGraphBuilder graphbuilder = new SocialSparseGraphBuilder(CRSUtils.getCRS(4326));
		SocialSparseGraph graph = graphbuilder.createGraph();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			Coord c = ((Activity)person.getSelectedPlan().getPlanElements().get(0)).getCoord();
			graphbuilder.addVertex(graph, new SocialPerson((PersonImpl) person), MatsimCoordUtils.coordToPoint(c));
		}
		ActivityDistanceCalculator actDistCalc = new ActDistCoordCalculator(new WGS84DistanceCalculator());
//		TripDistanceAccessibilityTask tripTask = new TripDistanceAccessibilityTask(actDistCalc);
//		tripTask.setTrajectories(trajectories);
//		tripTask.setOutputDirectoy("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/aggacts/sun/");
//		GraphAnalyzer.analyze(graph, tripTask);
		
//		TrajectoryAnalyzer.analyze(new HashSet<Trajectory>(trajectories.values()), tComposite, "/Users/jillenberger/Work/socialnets/locationChoice/data/plans.sun.14473/");

	}

}
