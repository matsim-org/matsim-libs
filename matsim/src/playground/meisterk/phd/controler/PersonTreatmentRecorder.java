/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTreatmentRecorder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.phd.controler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;

import playground.meisterk.phd.replanning.PhDStrategyManager;

public class PersonTreatmentRecorder implements StartupListener, IterationEndsListener, ShutdownListener {

	private static final String FILENAME = "personTreatment.txt";
	private static final Logger log;

	private PrintStream out;
	private ArrayList<String> columnNames = null;

	static {
		log = Logger.getLogger(PersonTreatmentRecorder.class);
	}
	
	public void notifyStartup(StartupEvent event) {
		try {
			this.out = new PrintStream(org.matsim.core.controler.Controler.getOutputFilename(FILENAME));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		
		Controler c = event.getControler();
		PhDStrategyManager phDStrategyManager = (PhDStrategyManager) c.getStrategyManager();
		Map<PlanStrategy, Set<Person>> personTreatment = phDStrategyManager.getPersonTreatment();
		
		log.info("Writing results...");

		if (personTreatment.size() == 0) {
			return;
		}
		
		if (this.columnNames == null) {

			out.print("#iter");

			this.columnNames = new ArrayList<String>();
			for (PlanStrategy strategy : personTreatment.keySet()) {
				for (int rank=0; rank <= c.getConfig().strategy().getMaxAgentPlanMemorySize(); rank++) {
					String columnName = this.getColumnName(strategy, rank);
					out.print("\t");
					out.print(columnName);
					this.columnNames.add(columnName);
				}
			}
			out.println();
		}
		
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		
		for (PlanStrategy strategy : personTreatment.keySet()) {
			Set<Person> persons = personTreatment.get(strategy);
			for (Person person : persons) {
				int rank = this.getRank(person);
				String columnName = this.getColumnName(strategy, rank);
				if (!counts.containsKey(columnName)) {
					counts.put(columnName, 0);
				}
				counts.put(columnName, (counts.get(columnName))+1);
			}
		}
		
		out.print(event.getIteration());
		for (String columnName : this.columnNames) {
			out.print("\t");
			out.print(counts.get(columnName));
		}
		
		out.println();
		
		log.info("Writing results...done.");

	}

	public void notifyShutdown(ShutdownEvent event) {
		this.out.close();
	}

	int getRank(Person person) {
		
		int rank = 0;
		
		Double newScore = person.getSelectedPlan().getScore();
		for (Plan plan : person.getPlans()) {
			if (!plan.isSelected()) {
				Double otherScore = plan.getScore();
				if (otherScore != null) {
					if (otherScore >= newScore) {
						rank++;
					}
				}
			}
		}
		
		return rank;
	}

	private String getColumnName(PlanStrategy strategy, int rank) {
		return strategy.toString() + "_" + Integer.toString(rank);
	}

}
