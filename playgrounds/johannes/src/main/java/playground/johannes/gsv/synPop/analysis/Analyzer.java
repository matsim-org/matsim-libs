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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import playground.johannes.coopsim.analysis.ActTypeShareTask;
import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ActivityLoadTask;
import playground.johannes.coopsim.analysis.ArrivalLoadTask;
import playground.johannes.coopsim.analysis.DepartureLoadTask;
import playground.johannes.coopsim.analysis.LegFrequencyTask;
import playground.johannes.coopsim.analysis.LegLoadTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripDurationTask;
import playground.johannes.coopsim.analysis.TripPurposeShareTask;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.analysis.MonthTask;
import playground.johannes.gsv.synPop.mid.analysis.SeasonsTask;

/**
 * @author johannes
 *
 */
public class Analyzer {

	private static final Logger logger = Logger.getLogger(Analyzer.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		String output = args[2];
		String output = "/home/johannes/gsv/mid2008/analysis/car.trips.3-1000km/";
//		String personFile = args[0];
		String personFile = "/home/johannes/gsv/mid2008/pop/pop.car.3-1000km.trips.xml";
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		
		parser.parse(personFile);

		Set<ProxyPerson> persons = parser.getPersons();
		
		Set<ProxyPerson> remove = new HashSet<>();
		for(ProxyPerson person : persons) {
			if(person.getId().startsWith("foreign")) {
				remove.add(person);
			}
		}
		
		for(ProxyPerson person : remove) {
			persons.add(person);
		}
		logger.info(String.format("Removed %s foreign persons.", remove.size()));
		
//		logger.info("Cloning persons...");
//		Random random = new XORShiftRandom();
//		persons = PersonCloner.weightedClones(persons, 200000, random);
//		new ApplySampleProbas(82000000).apply(persons);
//		logger.info(String.format("Generated %s persons.", persons.size()));
		
//		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/synpop/data/gis/nuts/pop.nuts3.shp");
//		Set<Geometry> geometries = new HashSet<Geometry>();
// 		for(SimpleFeature feature : features) {
//			geometries.add((Geometry) feature.getDefaultGeometry());
//		}
//		
//		Config config = ConfigUtils.createConfig();
//		Scenario scenario = ScenarioUtils.createScenario(config);
//		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
//		facReader.readFile(args[1]);
//		ActivityFacilities facilities = scenario.getActivityFacilities();
	
	
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
//		task.addTask(new ActivityChainTask());
		task.addTask(new LegGeoDistanceTask("car"));
		task.addTask(new LegGeoDistanceTask("car", 100000));
		task.addTask(new LegRouteDistanceTask("car"));
		task.addTask(new LegRouteDistanceTask("car", 100000));
//		task.addTask(new ActivityDistanceTask(facilities, "car"));
//		task.addTask(new ActivityDistanceTruncatedTask(facilities, "car", 100000));
		task.addTask(new DistanceJourneyDaysTask("car"));
		task.addTask(new TripDayVolumeTask("car"));
//		task.addTask(new SpeedFactorAnalyzer());
		task.addTask(new SeasonsTask());
		task.addTask(new PkmTask("car"));
		task.addTask(new PkmTaskSeason("car"));
		task.addTask(new MonthTask());
//		task.addTask(new PopulationDensityTask(geometries, facilities, output));
		
		task.setOutputDirectory(output);
		ProxyAnalyzer.analyze(persons, task, output);
		
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
