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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class CEAnalyzer implements IterationEndsListener, ShutdownListener {

	private ArrowPrattRiskAversionI utilFunc;
	
	private TripAndScoreStats stats;
	
	private List<PersonImpl> persons;
	
	private String personsFile;
	
	private PopulationImpl plans;
	
	private Map<PersonImpl, List<Double>> samples;
	
	public CEAnalyzer(String personsFile, PopulationImpl plans, TripAndScoreStats stats, ArrowPrattRiskAversionI utilFunc) {
		this.personsFile = personsFile;
		this.plans = plans;
		this.stats = stats;
		this.utilFunc = utilFunc;
		samples = new HashMap<PersonImpl, List<Double>>();
	}
	
	public CEAnalyzer(List<PersonImpl> persons, TripAndScoreStats stats, ArrowPrattRiskAversionI utilFunc) {
		this.persons = persons;
		this.stats = stats;
		this.utilFunc = utilFunc;
		samples = new HashMap<PersonImpl, List<Double>>();
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		if(persons == null && personsFile != null) {
			readPersons(personsFile, plans);
		}
		for (PersonImpl p : persons) {
			Double tripDur = stats.getTripDurations().get(p);
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

	private void readPersons(String file, PopulationImpl plans) {
		persons = new LinkedList<PersonImpl>();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(file);
			String personId;
			while ((personId = reader.readLine()) != null) {
				PersonImpl p = plans.getPersons().get(new IdImpl(personId));
				if (p != null)
					persons.add(p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void notifyShutdown(ShutdownEvent event) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(Controler
					.getOutputFilename("ceValues.txt"));

			for (PersonImpl p : persons) {
				List<Double> personSamples = samples.get(p);
				double utilsum = 0;
				for (Double d : personSamples)
					utilsum += utilFunc.evaluate(d);
				writer.write(p.getId().toString());
				writer.write("\t");
				double exp_util = utilsum / (double) personSamples.size();
				writer.write(String.valueOf(utilFunc.getTravelTime(exp_util)));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
