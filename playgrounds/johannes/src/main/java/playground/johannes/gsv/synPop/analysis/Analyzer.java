/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.gsv.synPop.io.XMLParser;

/**
 * @author johannes
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String output = args[2];
//		String output = "/home/johannes/gsv/mid2008/analysis/all/";
		String personFile = args[0];
//		String personFile = "/home/johannes/gsv/mid2008/pop/pop.xml";
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		
		parser.parse(personFile);

		
//		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/synpop/data/gis/nuts/pop.nuts3.shp");
//		Set<Geometry> geometries = new HashSet<Geometry>();
// 		for(SimpleFeature feature : features) {
//			geometries.add((Geometry) feature.getDefaultGeometry());
//		}
//		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(args[1]);
		ActivityFacilities facilities = scenario.getActivityFacilities();
	
	
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
//		task.addTask(new ActivityChainTask());
		task.addTask(new LegTargetDistanceTask("car"));
		task.addTask(new ActivityDistanceTask(facilities));
//		task.addTask(new SpeedFactorAnalyzer());
//		task.addTask(new SeasonsTask());
//		task.addTask(new PkmTask());
//		task.addTask(new PopulationDensityTask(geometries, facilities, output));
		
		task.setOutputDirectory(output);
		ProxyAnalyzer.analyze(parser.getPersons(), task, output);
		
//		Set<Trajectory> trajectories = TrajectoryProxyBuilder.buildTrajectories(parser.getPersons());
//		TrajectoryAnalyzerTaskComposite ttask = new TrajectoryAnalyzerTaskComposite();
//		ttask.addTask(new ActivityDurationTask());
//		ttask.addTask(new ActivityLoadTask());
//		ttask.addTask(new ActTypeShareTask());
//		ttask.addTask(new ArrivalLoadTask());
//		ttask.addTask(new DepartureLoadTask());
//		ttask.addTask(new LegLoadTask());
//		ttask.addTask(new TripDurationTask());
//		ttask.addTask(new TripPurposeShareTask());
//		ttask.addTask(new LegFrequencyTask());
//		
//		TrajectoryAnalyzer.setAppend(true);
//		TrajectoryAnalyzer.analyze(trajectories, ttask, output);

	}

}
