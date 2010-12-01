/* *********************************************************************** *
 * project: org.matsim.*
 * LegDistanceAnalyzer.java
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.xml.sax.SAXException;

/**
 * @author illenberger
 * 
 */
public class LegDistanceAnalyzer {

	public static Map<String, Distribution> analyze(Population population) {
//		Map<String, TDoubleDoubleHashMap> map = new HashMap<String, TDoubleDoubleHashMap>();
		Map<String, Distribution> map = new HashMap<String, Distribution>();
//		TDoubleDoubleHashMap all = new TDoubleDoubleHashMap();
		Distribution all = new Distribution();
		map.put("all", all);
		
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (plan.getPlanElements().size() > 1) {
				for (int i = 1; i < plan.getPlanElements().size(); i += 2) {
					Route route = ((Leg) plan.getPlanElements().get(i)).getRoute();
					if (route != null) {
						double dist = route.getDistance();
//						double dist = route.getTravelTime();
						String type = ((Activity) plan.getPlanElements().get(i + 1)).getType();//.substring(0, 1);
//						if(type.equalsIgnoreCase("visit") || type.equalsIgnoreCase("bar") || type.equalsIgnoreCase("loudoor")) {
//							type = "leisure";
//						TDoubleDoubleHashMap hist = map.get(type);
						Distribution hist = map.get(type);
						if (hist == null) {
//							hist = new TDoubleDoubleHashMap();
							hist = new Distribution();
							map.put(type, hist);
						}

//						hist.adjustOrPutValue(Math.ceil(dist / 300.0)*5, 1, 1);
//						hist.adjustOrPutValue(Math.ceil(dist / 1000.0), 1, 1);
						hist.add(dist);
//						}
//						all.adjustOrPutValue(Math.ceil(dist / 1000.0), 1, 1);
						all.add(dist);
					}
				}
			}
		}
		
		return map;
	}
	
	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		Scenario scenario = new ScenarioImpl();
//		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
//		netReader.parse("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml");
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
//		reader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_transitincl_10pct.xml");
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/plans.xml");
		
		Population population = scenario.getPopulation();
		Map<String , Distribution> map = analyze(population);
		

		for(Entry<String, Distribution> entry : map.entrySet()) {
			Distribution distr = entry.getValue();
//			if(distr.getValues().length > 100) {
			Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistribution(1000)), "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/" + entry.getKey() + ".dist.ref.txt");
//			}
		}
	}
}
