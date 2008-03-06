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

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.plans.Person;
import org.matsim.plans.Route;
import org.matsim.utils.io.IOUtils;

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
	
	private BufferedWriter summaryWriter;
	
	public EUTRouterAnalyzer(ArrowPrattRiskAversionI utilFunction) {
		this.utilFunction = utilFunction;
	}
	
	public void appendSnapshot(Route bestRoute, double bestRouteCosts, Route indiffRoute) {
		Snapshot s = new Snapshot();
		s.ce = utilFunction.getTravelTime(bestRouteCosts);
		if(!bestRoute.getRoute().equals(indiffRoute.getRoute()))
			s.routesDiffer = true;
		
		s.person = person;
		snaphots.add(s);
	}
	
	public void setNextPerson(Person person) {
		this.person = person;
	}

	public void addGuidedPerson(Person person) {
		guidedPersons.add(person);
	}
	
	public Collection<Person> getGuidedPersons() {
		return guidedPersons;
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
			/*
			 * Dump iteration analysis...
			 */
			String filename = Controler.getIterationFilename("routeranalysis.txt");
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.write("Person\tCE\troutesDiffer");
			writer.newLine();
			for(Snapshot s : snaphots) {
				writer.write(s.toString());
				writer.newLine();
				
				if(s.routesDiffer) {
					totalRouteDiffers++;
					if(guidedPersons.contains(s.person))
						replannedTwice++;
				}
			}
			writer.close();
			
			/*
			 * Dump summary...
			 */
			summaryWriter.write(String.valueOf(event.getIteration()));
			summaryWriter.write(TAB);
			summaryWriter.write(String.valueOf(totalRouteDiffers));
			summaryWriter.write(TAB);
			summaryWriter.write(String.valueOf(guidedPersons.size()));
			summaryWriter.write(TAB);
			summaryWriter.write(String.valueOf(replannedTwice));
			summaryWriter.newLine();
			summaryWriter.flush();			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void notifyStartup(StartupEvent event) {
		String filename = Controler.getOutputFilename("replanAnalysis.txt");
		try {
			summaryWriter = IOUtils.getBufferedWriter(filename);
			summaryWriter.write("Iteration\trouteDiffers\tn_guided\treplannedTwice");
			summaryWriter.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			summaryWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class Snapshot {
		
		private Person person;
		
		private double ce;
		
		private boolean routesDiffer;
		
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
