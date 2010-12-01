/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistance.java
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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.core.population.MatsimPopulationReader;


/**
 * @author illenberger
 *
 */
public class TravelTimeDistance {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/plans.xml");
		
		Population pop = scenario.getPopulation();
		
		TDoubleArrayList dist = new TDoubleArrayList();
		TDoubleArrayList travtime = new TDoubleArrayList();
		
		Discretizer discret = new LinearDiscretizer(1000);
		
		TDoubleDoubleHashMap distMap = new TDoubleDoubleHashMap();
//		TDoubleDoubleHashMap ttMap = new TDoubleDoubleHashMap();
		TDoubleIntHashMap cntDist = new TDoubleIntHashMap();
//		TIntIntHashMap ttCnt = new TIntIntHashMap();
		
		for(Person person : pop.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 1; i < plan.getPlanElements().size(); i+=2) {
					Leg leg = (Leg) plan.getPlanElements().get(i);
					if(!leg.getMode().equalsIgnoreCase("other")) {
					Route route = leg.getRoute();
					if(route != null) {
						double d = discret.discretize(route.getDistance());
						double tt = route.getTravelTime();
						dist.add(d);
						travtime.add(tt);
						
						distMap.adjustOrPutValue(d, tt, tt);
						cntDist.adjustOrPutValue(d, 1, 1);
					}
					}
				}
			}
		}

		TDoubleDoubleIterator it = distMap.iterator();
		for(int i = 0; i < distMap.size(); i++) {
			it.advance();
			double tt = it.value();
			distMap.put(it.key(), tt/(double)cntDist.get(it.key()));
		}
		Distribution dummy = new Distribution();
		dummy.absoluteDistribution(1);
		Distribution.writeHistogram(distMap, "/Users/jillenberger/Work/socialnets/sim/tt_dist_aggr.txt");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/sim/tt_dist.txt"));
		writer.write("d\ttt");
		writer.newLine();
		for(int i = 0; i < dist.size(); i++) {
			writer.write(String.valueOf(dist.get(i)));
			writer.write("\t");
			writer.write(String.valueOf(travtime.get(i)));
			writer.newLine();
		}
		writer.close();
	}

}
