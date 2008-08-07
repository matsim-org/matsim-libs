/* *********************************************************************** *
 * project: org.matsim.*
 * TraversedRiskyLink.java
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.OrPredicate;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.network.Link;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Plans;
import org.matsim.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class TraversedRiskyLink implements StartupListener, ShutdownListener, IterationEndsListener {
	
	private Plans population;
	
	private Predicate personPredicate;
	
	private BufferedWriter writer;
	
	private List<Integer> samples = new LinkedList<Integer>();
	
	private SummaryWriter summaryWriter;
	
	private Collection<Person> persons = null;
	
	public TraversedRiskyLink(Plans population, List<Link> riskyLinks, SummaryWriter summaryWriter) {
		this.summaryWriter = summaryWriter;
		this.population = population;
		
		if (!riskyLinks.isEmpty()) {
			List<Predicate> linkPlanPredicates = new LinkedList<Predicate>();
			for (BasicLink link : riskyLinks) {
				linkPlanPredicates.add(new LinkPlanPredicate(link));
			}

			Iterator<Predicate> it = linkPlanPredicates.iterator();
			Predicate predicate1 = it.next();
			if (linkPlanPredicates.size() > 1) {
				for (; it.hasNext();) {
					Predicate predicate2 = it.next();
					predicate1 = OrPredicate
							.getInstance(predicate1, predicate2);
				}
			}

			personPredicate = new LinkPersonPredicate(predicate1);
		}
	}
	
	public Collection<Person> getPersons() {
		return persons;
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		Collection subCollection = CollectionUtils.select(population.getPersons().values(), personPredicate);
		persons = subCollection;
		try{
			writer.write(String.valueOf(event.getIteration()));
			writer.write("\t");
			writer.write(String.valueOf(subCollection.size()));
			samples.add(subCollection.size());
			writer.newLine();
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class LinkPersonPredicate implements Predicate {

		private Predicate linkPlanPredicat;
	
		public LinkPersonPredicate(Predicate linkPlanPredicate) {
			this.linkPlanPredicat = linkPlanPredicate;
		}
		
		public boolean evaluate(Object arg0) {
			Person person = (Person)arg0;
			return linkPlanPredicat.evaluate(person.getSelectedPlan());
		}
		
	}
	
	private class LinkPlanPredicate implements Predicate {

		private BasicLink predicateLink;
		
		public LinkPlanPredicate(BasicLink link) {
			predicateLink = link;
		}
		
		public boolean evaluate(Object arg0) {
			Plan plan = (Plan)arg0;
			for(Iterator it = plan.getIteratorLeg(); it.hasNext();) {
				Leg leg = (Leg)it.next();
				for(Link link : leg.getRoute().getLinkRoute()) {
					if(link.equals(predicateLink))
						return true;
				}
				
				return false;
			}
			
			return false;
		}
		
	}

	public void notifyStartup(StartupEvent event) {
		try {
			writer = IOUtils.getBufferedWriter(Controler.getOutputFilename("traversedRiskyLink.txt"));
			writer.write("Iteration\tcount");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			int sum = 0;
			for(Integer i : samples)
				sum += i;
			writer.write("avr\t");
			double avr = sum/(double)samples.size();
			summaryWriter.setN_traversedRiskyLink(avr);
			writer.write(String.valueOf(avr));
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
