/* *********************************************************************** *
 * project: org.matsim.*
 * ModeChoiceAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.ciarif.modechoice_old;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;


/**
 * @author ciarif
 *
 */
public class ModeChoiceAnalyzer extends AbstractPersonAlgorithm {
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	TreeMap<TransportMode, Integer> modeStatistics =
		new TreeMap<TransportMode, Integer>();
	
	public ModeChoiceAnalyzer() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.plans.algorithms.PersonAlgorithm#run(org.matsim.demandmodeling.plans.Person)
	 */
	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				TransportMode mode = leg.getMode();
				int modeCount = 0;
				
				if (modeStatistics.containsKey(mode)) {
					modeCount = modeStatistics.get(mode);
				}
				modeCount++;
				modeStatistics.put(mode, modeCount);
			}
		}
	}
	
	public void printInformation() {
		Iterator<Map.Entry<TransportMode, Integer>> modeIt = modeStatistics.entrySet().iterator();
		while (modeIt.hasNext()) {
			Map.Entry entry = modeIt.next();
			System.out.println("There are " + entry.getValue() + " modes of "
					+ " type " + entry.getKey());
		}
	}
	
	public void writeStatistics(String filename) {
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			Iterator<Map.Entry<TransportMode, Integer>> modeIt = modeStatistics.entrySet().iterator();
			while (modeIt.hasNext()) {
				Map.Entry entry = modeIt.next();
				out.write(entry.getKey() + ";" + entry.getValue() + "\n");
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
				+ e.getMessage());
		}
	}
}

	