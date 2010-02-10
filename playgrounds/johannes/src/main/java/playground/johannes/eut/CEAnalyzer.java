/* *********************************************************************** *
 * project: org.matsim.*
 * CEAnalyzer.java
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

/**
 * 
 */
package playground.johannes.eut;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class CEAnalyzer implements IterationEndsListener, ShutdownListener {

	private ArrowPrattRiskAversionI utilFunc;
	
	private TripAndScoreStats stats;
	
	private List<Person> persons;
	
	private String personsFile;
	
	private Population plans;
	
	private Map<Person, List<Double>> samples;
	
	public CEAnalyzer(String personsFile, Population plans, TripAndScoreStats stats, ArrowPrattRiskAversionI utilFunc) {
		this.personsFile = personsFile;
		this.plans = plans;
		this.stats = stats;
		this.utilFunc = utilFunc;
		samples = new HashMap<Person, List<Double>>();
	}
	
	public CEAnalyzer(List<Person> persons, TripAndScoreStats stats, ArrowPrattRiskAversionI utilFunc) {
		this.persons = persons;
		this.stats = stats;
		this.utilFunc = utilFunc;
		samples = new HashMap<Person, List<Double>>();
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		if(persons == null && personsFile != null) {
			readPersons(personsFile, plans);
		}
		for (Person p : persons) {
			Double tripDur = stats.getTripDurations().get(p.getId());
			if (tripDur != null) { // F***ing withinday bug!
				List<Double> personSamples = samples.get(p);
				if (personSamples == null) {
					personSamples = new LinkedList<Double>();
					samples.put(p, personSamples);
				}
				personSamples.add(tripDur);
			}
		}
	}

	private void readPersons(String file, Population plans) {
		persons = new LinkedList<Person>();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(file);
			String personId;
			while ((personId = reader.readLine()) != null) {
				Person p = plans.getPersons().get(new IdImpl(personId));
				if (p != null)
					persons.add(p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void notifyShutdown(ShutdownEvent event) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(event.getControler().getControlerIO()
					.getOutputFilename("ceValues.txt"));

			for (Person p : persons) {
				List<Double> personSamples = samples.get(p);
				double utilsum = 0;
				for (Double d : personSamples)
					utilsum += utilFunc.evaluate(d);
				writer.write(p.getId().toString());
				writer.write("\t");
				double exp_util = utilsum / personSamples.size();
				writer.write(String.valueOf(utilFunc.getTravelTime(exp_util)));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
