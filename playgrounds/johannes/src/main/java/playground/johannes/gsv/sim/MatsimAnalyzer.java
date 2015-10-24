/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import playground.johannes.coopsim.analysis.PkmRouteTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.gsv.analysis.PkmGeoTask;
import playground.johannes.mz2005.analysis.TrajectoryPlanBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class MatsimAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		String popFile = args[0];
		String netFile = args[1];
		String facFile = args[2];
		String outFile = args[3];
		
		PopulationReader reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(popFile);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(netFile);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(facFile);
		
		Set<Plan> plans = new HashSet<>(scenario.getPopulation().getPersons().size());
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
		}
		
		TrajectoryPlanBuilder builder = new TrajectoryPlanBuilder();
		Map<Plan, Trajectory> trajectories = builder.buildTrajectory(plans);
		
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();
		composite.addTask(new PkmGeoTask(scenario.getActivityFacilities()));
		composite.addTask(new PkmRouteTask(scenario.getNetwork(), 0));
		composite.addTask(new PkmRouteTask(scenario.getNetwork(), 0.5));
		composite.addTask(new PkmRouteTask(scenario.getNetwork(), 1));
		
		Set<Trajectory> ts = new HashSet<>(trajectories.values());
		TrajectoryAnalyzer.analyze(ts, composite, outFile);
	}

}
