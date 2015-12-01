/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityChoiceSetDistribution.java
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

import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.studies.sbsurvey.io.SocialSparseGraphMLReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author illenberger
 * 
 */
public class FacilityChoiceSetDistribution {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		config.addCoreModules();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader
				.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.leisure.xml");
		ActivityFacilities facilities = scenario.getActivityFacilities();

		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialSparseGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/locationChoice/mcmc.backup/run336/output/20000000000/graph.graphml");

		DistanceCalculator calculator = new CartesianDistanceCalculator();
		Discretizer discretizer = new LinearDiscretizer(1000.0);

		Map<String, Map<SocialVertex, List<Id<ActivityFacility>>>> choicesets = new HashMap<>();

		choicesets.put("culture", FacilityChoiceSetGenerator.read("/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.culture.txt", graph));
		choicesets.put("gastro", FacilityChoiceSetGenerator.read("/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.gastro.txt", graph));
		choicesets.put("sports", FacilityChoiceSetGenerator.read("/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.sports.txt", graph));

		for (Entry<String, Map<SocialVertex, List<Id<ActivityFacility>>>> entry2 : choicesets.entrySet()) {
			Map<SocialVertex, List<Id<ActivityFacility>>> map = entry2.getValue();

			TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
			for (Entry<SocialVertex, List<Id<ActivityFacility>>> entry : map.entrySet()) {
				Point p1 = entry.getKey().getPoint();
				for (Id<ActivityFacility> id : entry.getValue()) {
					ActivityFacility f = facilities.getFacilities().get(id);
					Point p2 = MatsimCoordUtils.coordToPoint(f.getCoord());

					double d = calculator.distance(p1, p2);
					d = discretizer.discretize(d);

					hist.adjustOrPutValue(d, 1.0, 1.0);
				}
			}

			StatsWriter.writeHistogram(hist, "d", "n", "/Users/jillenberger/Work/socialnets/locationChoice/output/" + entry2.getKey() + ".txt");
		}
	}
}
