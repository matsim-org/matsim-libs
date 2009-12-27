/* *********************************************************************** *
 * project: org.matsim.*
 * EUTRouterAnalyzer.java
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class EUTRouterAnalyzer implements IterationStartsListener, IterationEndsListener, StartupListener, ShutdownListener {

	private static final String TAB = "\t";
	
	private ArrowPrattRiskAversionI utilFunction;
	
	private List<Snapshot> snaphots = new LinkedList<Snapshot>();
	
	private Person person;
	
	private Collection<Person> guidedPersons;
	
	private Collection<Person> replannedPersons;
	
	private Collection<Person> riskAversePersons;
	
	private List<Integer> sampelsRiskAverse = new LinkedList<Integer>();
	
	private BufferedWriter runWriter;
	
	private SummaryWriter summaryWriter;
	
	public EUTRouterAnalyzer(ArrowPrattRiskAversionI utilFunction, SummaryWriter summaryWriter) {
		this.utilFunction = utilFunction;
		this.summaryWriter = summaryWriter;
		replannedPersons = new HashSet<Person>();
		riskAversePersons = new HashSet<Person>();
	}
	
	public void appendSnapshot(Path bestRoute, double bestRouteCosts, Path indiffRoute) {
		Snapshot s = new Snapshot();
		s.ce = utilFunction.getTravelTime(bestRouteCosts);
		if(!bestRoute.nodes.equals(indiffRoute.nodes)) {
			s.routesDiffer = true;
			riskAversePersons.add(person);
		}
		s.person = person;
		snaphots.add(s);
	}
	
	public void setNextPerson(Person person) {
		this.person = person;
		replannedPersons.add(person);
	}

	public void addGuidedPerson(Person person2) {
		guidedPersons.add(person2);
	}
	
	public Collection<Person> getGuidedPersons() {
		return guidedPersons;
	}
	
	public Collection<Person> getReplanedPersons() {
		return replannedPersons;
	}
	
	public Collection<Person> getRiskAversePersons() {
		return riskAversePersons;
	}
	
	public void notifyIterationStarts(IterationStartsEvent event) {
		snaphots = new LinkedList<Snapshot>();
		guidedPersons = new HashSet<Person>();
		person = null;
		
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			int totalRouteDiffers = 0;
			int replannedTwice = 0;
			Set<Person> riskyPersons = new HashSet<Person>();
			/*
			 * Dump iteration analysis...
			 */
			String filename = event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(), "routeranalysis.txt");
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.write("Person\tCE\troutesDiffer");
			writer.newLine();
			for(Snapshot s : snaphots) {
				writer.write(s.toString());
				writer.newLine();
				
				if(s.routesDiffer) {
					riskyPersons.add(s.person);
					totalRouteDiffers++;
					if(guidedPersons.contains(s.person))
						replannedTwice++;
				}
			}
			sampelsRiskAverse.add(totalRouteDiffers);
			writer.close();
			
			/*
			 * Get agents that are risk averse and guided...
			 */
			Collection riskyAndGuided = CollectionUtils.intersection(riskyPersons, guidedPersons);
			/*
			 * Dump summary...
			 */
			runWriter.write(String.valueOf(event.getIteration()));
			runWriter.write(TAB);
			runWriter.write(String.valueOf(totalRouteDiffers));
			runWriter.write(TAB);
			runWriter.write(String.valueOf(guidedPersons.size()));
			runWriter.write(TAB);
			runWriter.write(String.valueOf(replannedTwice));
			runWriter.write(TAB);
			runWriter.write(String.valueOf(riskyAndGuided.size()));
			runWriter.newLine();
			runWriter.flush();
			/*
			 * We need to do this here, since re-planning happens in
			 * iteration-start event.
			 */
			replannedPersons = new HashSet<Person>();
			riskAversePersons = new HashSet<Person>();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void notifyStartup(StartupEvent event) {
		String filename = Controler.getOutputFilename("replanAnalysis.txt");
		try {
			runWriter = IOUtils.getBufferedWriter(filename);
			runWriter.write("Iteration\trouteDiffers\tn_guided\treplannedTwice\triskyAndGuided");
			runWriter.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			int sum = 0;
			for(Integer i : sampelsRiskAverse) {
				sum += i;
			}
			runWriter.write("avr\t");
			double n_avr = sum/(double)sampelsRiskAverse.size();
			summaryWriter.setN_riskaverse(n_avr);
			runWriter.write(String.valueOf(n_avr));
			runWriter.newLine();
			runWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class Snapshot {
		
		private Person person;
		
		private double ce;
		
		private boolean routesDiffer;
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(person.getId().toString());
			builder.append(TAB);
			builder.append(String.valueOf(ce));
			builder.append(TAB);
			builder.append(String.valueOf(routesDiffer));
			return builder.toString();
		}
	}
}
