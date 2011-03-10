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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;


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
				if(act.getType().contains(type)) {
				int start = (int) discretizer.discretize(act.getStartTime());
				int end = (int) discretizer.discretize(act.getEndTime());
				if(Double.isInfinite(act.getEndTime()))
					end = (int)discretizer.discretize(86400);
				
				for(int t = start; t <= end; t += 1) {
					histogram.adjustOrPutValue(t, 1, 1);
				}
				}
			}
		}
		
		return histogram;
	}
	
	
	
	public static void main(String args[]) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		String popFile ="/Volumes/hertz.math.tu-berlin.de/net/ils/jillenberger/socialnets/sim/output/2200000.plan.xml";
//		String popFile = "/Users/jillenberger/Work/work/socialnets/sim/plans.out.xml";
//		String graphFile = args[3];
//		double proba = Double.parseDouble(args[4]);

//		GeometryFactory geoFactory = new GeometryFactory();

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
		facReader.parse("/Users/jillenberger/Work/work/socialnets/sim/facilities.xml");

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);
		
		ActivityTimeAnalyzer analyzer = new ActivityTimeAnalyzer();
		TDoubleDoubleHashMap histogram = analyzer.analyze(scenario.getPopulation(), "home");
//		Distribution.writeHistogram(histogram,"/Volumes/hertz.math.tu-berlin.de/net/ils/jillenberger/socialnets/sim/times.out.txt");
		Distribution.writeHistogram(histogram,"/Volumes/hertz.math.tu-berlin.de/net/ils/jillenberger/socialnets/sim/output/2200000.home.txt");
	}
}
