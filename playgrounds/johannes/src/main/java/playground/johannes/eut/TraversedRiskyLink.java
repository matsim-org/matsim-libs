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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class TraversedRiskyLink implements StartupListener, ShutdownListener, IterationEndsListener {
	
	private Population population;
	
	private Predicate personPredicate;
	
	private BufferedWriter writer;
	
	private List<Integer> samples = new LinkedList<Integer>();
	
	private SummaryWriter summaryWriter;
	
	private Collection<Person> persons = null;
	
	public TraversedRiskyLink(Population population, List<Link> riskyLinks, SummaryWriter summaryWriter) {
		this.summaryWriter = summaryWriter;
		this.population = population;
		
		if (!riskyLinks.isEmpty()) {
			List<Predicate> linkPlanPredicates = new LinkedList<Predicate>();
			for (Link link : riskyLinks) {
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
			PersonImpl person = (PersonImpl)arg0;
			return linkPlanPredicat.evaluate(person.getSelectedPlan());
		}
		
	}
	
	private class LinkPlanPredicate implements Predicate {

		private Link predicateLink;
		
		public LinkPlanPredicate(Link link) {
			predicateLink = link;
		}
		
		public boolean evaluate(Object arg0) {
			PlanImpl plan = (PlanImpl)arg0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					for(Id linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
						if(linkId.equals(predicateLink.getId()))
							return true;
					}
					return false; // doesn't make sense, only first Leg will ever be evaluated
				}
			}
			return false;
		}
		
	}

	public void notifyStartup(StartupEvent event) {
		try {
			writer = IOUtils.getBufferedWriter(event.getControler().getControlerIO().getOutputFilename("traversedRiskyLink.txt"));
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
