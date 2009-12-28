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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class TripAndScoreStats implements StartupListener, ShutdownListener,
		IterationEndsListener, AgentWait2LinkEventHandler, AgentArrivalEventHandler {

	private final static String TAB = "\t";
	
	private Map<Id, Double> departures;
	
	private Map<Id, Double> tripDurations;
	
	private EUTRouterAnalyzer analyzer;
	
	private BufferedWriter writer;
	
	private List<Double> samplesAll = new LinkedList<Double>();
	
	private List<Double> samplesGuided = new LinkedList<Double>();
	
	private List<Double> samplesUnguided = new LinkedList<Double>();
	
	private List<Double> samplesRiskAverse = new LinkedList<Double>();
	
	private List<Double> samplesReplanned = new LinkedList<Double>();
	
	private List<Double> samplesTravRiskyLink = new LinkedList<Double>();
	
	private List<Double> samplesTravSafeLink = new LinkedList<Double>();
	
	private SummaryWriter summaryWriter;
	
	private TraversedRiskyLink traversedRiskLink;
	
	public TripAndScoreStats(EUTRouterAnalyzer analyzer, TraversedRiskyLink traversedRiskyLink, SummaryWriter summaryWriter) {
		this.analyzer = analyzer;
		this.summaryWriter = summaryWriter;
		this.traversedRiskLink = traversedRiskyLink;
	}

	@SuppressWarnings("unchecked")
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			writer.write(String.valueOf(event.getIteration()));
			writer.write(TAB);
			/*
			 * Total average
			 */
			Collection<Person> population = (Collection) event.getControler().getPopulation().getPersons().values();
			double avrTripDur = avrTripDuration(population, samplesAll);
			double avrScore = avrSelectedScore(population);
			dump(avrTripDur, avrScore);
			/*
			 * Guided agents
			 */
			avrTripDur = avrTripDuration(analyzer.getGuidedPersons(), samplesGuided);
			avrScore = avrSelectedScore(analyzer.getGuidedPersons());
			dump(avrTripDur, avrScore);
			/*
			 * Unguided agents
			 */
			Collection<Person> unguided = CollectionUtils.subtract(population, analyzer.getGuidedPersons());
			avrTripDur = avrTripDuration(unguided, samplesUnguided);
			avrScore = avrSelectedScore(unguided);
			dump(avrTripDur, avrScore);
			/*
			 * Re-planed agents
			 */
			avrTripDur = avrTripDuration(analyzer.getReplanedPersons(), samplesReplanned);
			avrScore = avrSelectedScore(analyzer.getReplanedPersons());
			dump(avrTripDur, avrScore);
			/*
			 * Risk-averse persons
			 */
			avrTripDur = avrTripDuration(analyzer.getRiskAversePersons(), samplesRiskAverse);
			avrScore = avrSelectedScore(analyzer.getRiskAversePersons());
			dump(avrTripDur, avrScore);
			/*
			 * Traversed risky links
			 */
			avrTripDur = avrTripDuration(traversedRiskLink.getPersons(), samplesTravRiskyLink);
			avrScore = avrSelectedScore(traversedRiskLink.getPersons());
			dump(avrTripDur, avrScore);
			/*
			 * Traversed save links
			 */
			Collection traversedSave = CollectionUtils.subtract(population, traversedRiskLink.getPersons());
			avrTripDur = avrTripDuration(traversedSave, samplesTravSafeLink);
			avrScore = avrSelectedScore(traversedSave);
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

	private double avrTripDuration(Collection<Person> persons, List<Double> samples) {
		if(persons == null || persons.isEmpty())
			return 0;
		
		double sum = 0;
		for(Person p : persons) {
			Double d = tripDurations.get(p.getId());
			if(d != null) {// unfortunately this can happen -> within-day bug!
				sum += d;
				samples.add(d);
			}
		}
		
		return sum/(double)persons.size();
	}
	
	private double avrSelectedScore(Collection<Person> persons) {
		if(persons == null || persons.isEmpty())
			return 0;
		
		double sum = 0;
		for(Person p : persons) {
			sum += p.getSelectedPlan().getScore().doubleValue();
		}
		
		return sum/(double)persons.size();
	}
	
	public void handleEvent(AgentWait2LinkEvent event) {
		departures.put(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		departures = new HashMap<Id, Double>();
		tripDurations = new HashMap<Id, Double>();
	}

	public void handleEvent(AgentArrivalEvent event) {
		Double time = departures.get(event.getPersonId());
		if(time != null) {
			double deltaT = event.getTime() - time;
			tripDurations.put(event.getPersonId(), deltaT); // TODO: Does not work with round trips!
			departures.remove(event.getPersonId());
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
			writer.write("\triskyLink_trip\triskyLink_score");
			writer.write("\tsafeLink_trip\tsafeLink_score");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			writer.write("avr\t");
			double avr = calcAvr(samplesAll);
			summaryWriter.setTt_avr(avr);
			writer.write(String.valueOf(avr));
			writer.write("\t");
			writer.write("\t");
			avr = calcAvr(samplesGuided);
			summaryWriter.setTt_guided(avr);
			writer.write(String.valueOf(avr));
			writer.write("\t");
			writer.write("\t");
			avr = calcAvr(samplesUnguided);
			summaryWriter.setTt_unguided(avr);
			writer.write(String.valueOf(avr));
			writer.write("\t");
			writer.write("\t");
			avr = calcAvr(samplesReplanned);
			summaryWriter.setTt_replaned(avr);
			writer.write(String.valueOf(avr));
			writer.write("\t");
			writer.write("\t");
			avr = calcAvr(samplesRiskAverse);
			summaryWriter.setTt_riskaverse(avr);
			writer.write(String.valueOf(avr));
			
			writer.write("\t");
			writer.write("\t");
			avr = calcAvr(samplesTravRiskyLink);
			summaryWriter.setTt_riskaverse(avr);
			writer.write(String.valueOf(avr));
			
			writer.write("\t");
			writer.write("\t");
			avr = calcAvr(samplesTravSafeLink);
			summaryWriter.setTt_riskaverse(avr);
			writer.write(String.valueOf(avr));
			
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private double calcAvr(List<Double> samples) {
		double sum = 0;
		for(Double d : samples)
			sum += d;
		
		return sum/(double)samples.size();
	}

	public Map<Id, Double> getTripDurations() {
		return tripDurations;
	}
}
