/* *********************************************************************** *
 * project: org.matsim.*
 * InteractionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.interaction;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.ActivityEndEvent;
import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.handler.ActivityStartEventHandler;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class InteractionHandler implements ActivityStartEventHandler,
		ActivityEndEventHandler {

	private InteractionSelector selector;
	
	private Interactor interactor;
	
	private Map<ActivityFacility, PhysicalFacility> pfacilities;
	
	public InteractionHandler(InteractionSelector selector, Interactor interactor, ActivityFacilities facilities) {
		this.selector = selector;
		this.interactor = interactor;
		this.pfacilities = new HashMap<ActivityFacility, PhysicalFacility>();
		
		for(ActivityFacility f : facilities.getFacilities().values()) {
			this.pfacilities.put(f, new PhysicalFacility());
		}
	}
	
	public void handleEvent(ActivityStartEvent event) {
		ActivityFacility f = event.getAct().getFacility();
		PhysicalFacility pf = pfacilities.get(f);
//		if(pf == null) {
//			pf = new PhysicalFacility();
//			facilities.put(f, pf);
//		}
		
		pf.enterPerson(event.getPerson(), event.getTime());
	}

	public void reset(int iteration) {
		for(PhysicalFacility pf : pfacilities.values())
			pf.reset();

	}

	public void handleEvent(ActivityEndEvent event) {
		if(!event.getAct().getType().equalsIgnoreCase("home")) {
		ActivityFacility f = event.getAct().getFacility();
		PhysicalFacility pf = pfacilities.get(f);
		
		if(pf == null)
			 throw new RuntimeException("Tried to remove a visitor from a non-existing physical facility!");
		else
			pf.leavePerson(event.getPerson(), event.getTime());

		}
	}

	void dumpVisitorStatisitcs(String file) throws FileNotFoundException, IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(file);
		
		writer.write("id\ttotal\tmin\tmax\tmean\topts");
		writer.newLine();
		
		final String TAB = "\t"; 
		final String WSPACE = " "; 
		for(ActivityFacility f : pfacilities.keySet()) {
			PhysicalFacility pf = pfacilities.get(f);
			writer.write(f.getId().toString());
			writer.write(TAB);
			writer.write(String.valueOf(pf.totalVisitors));
			writer.write(TAB);
			writer.write(String.valueOf(pf.concurrentVisitors.getMin()));
			writer.write(TAB);
			writer.write(String.valueOf(pf.concurrentVisitors.getMax()));
			writer.write(TAB);
			writer.write(String.valueOf(pf.concurrentVisitors.getMean()));
			writer.write(TAB);
			for(ActivityOption opt : f.getActivityOptions().values()) {
				writer.write(opt.getType());
				writer.write(WSPACE);
			}
			writer.newLine();
		}
		writer.close();
	}
	
	private class PhysicalFacility {
		
		private int totalVisitors;
		
		private DescriptiveStatistics concurrentVisitors = new DescriptiveStatistics();
		
		private Map<Person, Visitor> visitors = new HashMap<Person, Visitor>();
		
		private void enterPerson(Person p, double time) {
			visitors.put(p, new Visitor(p, time));
			
			// only for statistics
			totalVisitors++;
		}
		
		private void leavePerson(Person p, double time) {
			Visitor v = visitors.remove(p);
			if(v == null)
				throw new RuntimeException("Tried to remove a visitor that did not enter this facility!");
			else {
				
				Collection<Visitor> targets = selector.select(v, visitors.values());
				for(Visitor target : targets) {
					double startTime = Math.max(v.getEnterTime(), target.getEnterTime());
					interactor.interact(v.getPerson(), target.getPerson(), startTime, time);
				}
			}
			
			// only for statistics
			concurrentVisitors.addValue(visitors.values().size() + 1);
		}
		
		private void reset() {
			visitors.clear();
			
			// only for statistics
			totalVisitors = 0;
			concurrentVisitors.clear();
		}
	}
}
