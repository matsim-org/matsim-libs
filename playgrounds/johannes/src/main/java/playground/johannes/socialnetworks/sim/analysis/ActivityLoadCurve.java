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
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.network.NetworkReaderMatsimV1;
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
	
	public TDoubleDoubleHashMap makeLoadCurve(Set<Plan> plans, String type) {
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		
		for(Plan plan : plans) {
			for(PlanElement element : plan.getPlanElements()) {
				if(element instanceof Activity) {
					if (((Activity) element).getType().indexOf(type) > -1) {
						int start = (int) discretizer.discretize(((Activity) element).getStartTime());
						int end = (int) discretizer.discretize(((Activity) element).getEndTime());

						for (int t = start; t <= end; t++) {
							hist.adjustOrPutValue(t, 1, 1);
						}
					}
				}
			}
		}
		
		return hist;
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		Scenario scenario = new ScenarioImpl();
		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml");
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_transitincl_10pct.xml");
		
		Set<Plan> plans = new HashSet<Plan>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
		}
		
		ActivityLoadCurve loadCurve = new ActivityLoadCurve();
		TDoubleDoubleHashMap hist = loadCurve.makeLoadCurve(plans, "w");
		
		Distribution.writeHistogram(hist, "/Users/jillenberger/Work/work/socialnets/data/schweiz/leisures.txt");
	}
}
