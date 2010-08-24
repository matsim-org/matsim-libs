/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLoadCurve.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;

/**
 * @author illenberger
 *
 */
public class ActivityLoadCurve {

	private Discretizer discretizer = new LinearDiscretizer(3600);
	
	public Map<String, TDoubleDoubleHashMap> makeLoadCurve(Set<Plan> plans) {
		Map<String, TDoubleDoubleHashMap> map = new HashMap<String, TDoubleDoubleHashMap>();

		for (Plan plan : plans) {
			if(plan.getPlanElements().size() > 0) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity element = (Activity) plan.getPlanElements().get(i);
					String type = element.getType().substring(0, 1);
					
					double start = 0;
					if(!Double.isNaN(element.getStartTime()) && !Double.isInfinite(element.getStartTime()))
						start = discretizer.discretize(element.getStartTime());
					
					double end = 24;
					if(!Double.isNaN(element.getEndTime()) && !Double.isInfinite(element.getEndTime()))
						end = discretizer.discretize((element).getEndTime());

					start = Math.min(start, end);
					
					TDoubleDoubleHashMap hist = map.get(type);
					if (hist == null) {
						hist = new TDoubleDoubleHashMap();
						map.put(type, hist);
					}

					
					
					for (int t = (int)start; t <= end; t++) {
						hist.adjustOrPutValue(t, 1, 1);
					}

				}
			}
		}

		return map;
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		Scenario scenario = new ScenarioImpl();
//		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
//		netReader.parse("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml");
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
//		reader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_transitincl_10pct.xml");
		reader.readFile("/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/plans.xml");
		
		Set<Plan> plans = new HashSet<Plan>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
//			if(Math.random() < 0.1)
				plans.add(person.getSelectedPlan());
		}
		
		ActivityLoadCurve loadCurve = new ActivityLoadCurve();
		Map<String, TDoubleDoubleHashMap> map = loadCurve.makeLoadCurve(plans);
		
		Distribution distr = new Distribution();
		for(Entry<String, TDoubleDoubleHashMap> entry : map.entrySet()) {
			
			Distribution.writeHistogram(distr.normalizedDistribution(entry.getValue()), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/analysis/compareWithSim/actload/" + entry.getKey() + ".ref.txt");
		}
	}
}
