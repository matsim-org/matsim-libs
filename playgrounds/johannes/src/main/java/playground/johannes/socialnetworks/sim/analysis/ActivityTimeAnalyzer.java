/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
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

import java.io.FileNotFoundException;
import java.io.IOException;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.population.PopulationReaderMatsimV4;

import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;

import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author illenberger
 *
 */
public class ActivityTimeAnalyzer {

	private Discretizer discretizer = new LinearDiscretizer(900);
	
	public TDoubleDoubleHashMap analyze(Population population, String type) {
		TDoubleDoubleHashMap histogram = new TDoubleDoubleHashMap();
		
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
				Activity act = (Activity)plan.getPlanElements().get(i);
				if(act.getType().equalsIgnoreCase(type)) {
				int start = (int) discretizer.discretize(act.getStartTime());
				int end = (int) discretizer.discretize(act.getEndTime());
				for(int t = start; t <= end; t += 900) {
					histogram.adjustOrPutValue(t, 1, 1);
				}
				}
			}
		}
		
		return histogram;
	}
	
	
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		String popFile ="/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/plans_v2/census2000v2_dilZh30km_10pct/plans.xml.gz";
//		String graphFile = args[3];
//		double proba = Double.parseDouble(args[4]);

//		GeometryFactory geoFactory = new GeometryFactory();

		Scenario scenario = new ScenarioImpl();

//		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
//		netReader.parse(netFile);
//
//		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
//		facReader.parse(facFile);

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);
		
		ActivityTimeAnalyzer analyzer = new ActivityTimeAnalyzer();
		TDoubleDoubleHashMap histogram = analyzer.analyze(scenario.getPopulation(), "leisure");
		Distribution.writeHistogram(histogram,"/Users/jillenberger/Work/work/socialnets/sim/times.txt");
	}
}
