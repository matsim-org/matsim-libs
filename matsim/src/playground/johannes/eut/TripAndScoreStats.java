/* *********************************************************************** *
 * project: org.matsim.*
 * TripAndScoreStats.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.plans.Person;
import org.matsim.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class TripAndScoreStats implements StartupListener, ShutdownListener,
		IterationEndsListener, EventHandlerAgentWait2LinkI, EventHandlerAgentArrivalI {

	private final static String TAB = "\t";
	
	private Map<Person, Double> departures;
	
	private Map<Person, Double> tripDurations;
	
	private EUTRouterAnalyzer analyzer;
	
	private BufferedWriter writer;
	
	public TripAndScoreStats(EUTRouterAnalyzer analyzer) {
		this.analyzer = analyzer;
	}

	@SuppressWarnings("unchecked")
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			writer.write(String.valueOf(event.getIteration()));
			writer.write(TAB);
			/*
			 * Total average
			 */
			Collection<Person> population = event.getControler().getPopulation().getPersons().values();
			double avrTripDur = avrTripDuration(population);
			double avrScore = avrSelectedScore(population);
			dump(avrTripDur, avrScore);
			/*
			 * Guided agents
			 */
			avrTripDur = avrTripDuration(analyzer.getGuidedPersons());
			avrScore = avrSelectedScore(analyzer.getGuidedPersons());
			dump(avrTripDur, avrScore);
			/*
			 * Unguided agents
			 */
			Collection<Person> unguided = CollectionUtils.subtract(population, analyzer.getGuidedPersons());
			avrTripDur = avrTripDuration(unguided);
			avrScore = avrSelectedScore(unguided);
			dump(avrTripDur, avrScore);
			/*
			 * Re-planed agents
			 */
			avrTripDur = avrTripDuration(analyzer.getReplanedPersons());
			avrScore = avrSelectedScore(analyzer.getReplanedPersons());
			dump(avrTripDur, avrScore);
			/*
			 * Risk-averse persons
			 */
			avrTripDur = avrTripDuration(analyzer.getRiskAversePersons());
			avrScore = avrSelectedScore(analyzer.getRiskAversePersons());
			dump(avrTripDur, avrScore);
			
			writer.newLine();
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void dump(double... values) throws IOException {
		for(double d : values) {
			writer.write(String.valueOf((float)d));
			writer.write(TAB);
		}
		
	}

	private double avrTripDuration(Collection<Person> persons) {
		if(persons.isEmpty())
			return 0;
		
		double sum = 0;
		for(Person p : persons) {
			Double d = tripDurations.get(p);
			if(d != null)// unfortunately this can happen -> within-day bug!
				sum += d;
		}
		
		return sum/(double)persons.size();
	}
	
	private double avrSelectedScore(Collection<Person> persons) {
		if(persons.isEmpty())
			return 0;
		
		double sum = 0;
		for(Person p : persons) {
			sum += p.getSelectedPlan().getScore();
		}
		
		return sum/(double)persons.size();
	}
	
	public void handleEvent(EventAgentWait2Link event) {
		departures.put(event.agent, event.time);
	}

	public void reset(int iteration) {
		departures = new HashMap<Person, Double>();
		tripDurations = new HashMap<Person, Double>();
	}

	public void handleEvent(EventAgentArrival event) {
		Double time = departures.get(event.agent);
		if(time != null) {
			double deltaT = event.time - time;
			tripDurations.put(event.agent, deltaT);
			departures.remove(event.agent);
		}
		
	}

	public void notifyStartup(StartupEvent event) {
		try {
			writer = IOUtils.getBufferedWriter(Controler.getOutputFilename("tripAndScoreStats.txt"));
			writer.write("Iteration");
			writer.write("\tavr_trip\tavr_score");
			writer.write("\tguided_trip\tguided_score");
			writer.write("\tunguided_trip\tunguided_score");
			writer.write("\treplaned_trip\treplaned_score");
			writer.write("\triskaverse_trip\triskaverse_score");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public Map<Person, Double> getTripDurations() {
		return tripDurations;
	}
}
