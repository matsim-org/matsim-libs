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
package playground.johannes.socialnetworks.interaction;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.handler.DeprecatedActivityEndEventHandler;
import org.matsim.core.events.handler.DeprecatedActivityStartEventHandler;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class InteractionHandler implements DeprecatedActivityStartEventHandler,
DeprecatedActivityEndEventHandler {

	private final InteractionSelector selector;
	
	private final Interactor interactor;
	
	private final Map<Id, PhysicalFacility> pfacilities;
	
	private final Population population;
	
	private final ActivityFacilities facilities;
	
	public InteractionHandler(InteractionSelector selector, Interactor interactor, ActivityFacilities facilities, Population population) {
		this.selector = selector;
		this.interactor = interactor;
		this.population = population;
		this.facilities = facilities;
		this.pfacilities = new HashMap<Id, PhysicalFacility>();
		
		for(ActivityFacility f : facilities.getFacilities().values()) {
			this.pfacilities.put(f.getId(), new PhysicalFacility());
		}
	}
	
	public void handleEvent(ActivityStartEventImpl event) {
		PhysicalFacility pf = pfacilities.get(event.getAct().getFacilityId());
//		if(pf == null) {
//			pf = new PhysicalFacility();
//			facilities.put(f, pf);
//		}
		
		pf.enterPerson(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		for(PhysicalFacility pf : pfacilities.values())
			pf.reset();
	}

	public void handleEvent(ActivityEndEventImpl event) {
		if(!event.getActType().equalsIgnoreCase("home")) {
		PhysicalFacility pf = pfacilities.get(event.getAct().getFacilityId());
		
		if(pf == null)
			 throw new RuntimeException("Tried to remove a visitor from a non-existing physical facility!");
		else
			pf.leavePerson(event.getPersonId(), event.getTime());

		}
	}

	void dumpVisitorStatisitcs(String file) throws FileNotFoundException, IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(file);
		
		writer.write("id\ttotal\tmin\tmax\tmean\topts");
		writer.newLine();
		
		final String TAB = "\t"; 
		final String WSPACE = " "; 
		for(Id fId : pfacilities.keySet()) {
			PhysicalFacility pf = pfacilities.get(fId);
			writer.write(fId.toString());
			writer.write(TAB);
			writer.write(String.valueOf(pf.totalVisitors));
			writer.write(TAB);
			writer.write(String.valueOf(pf.concurrentVisitors.getMin()));
			writer.write(TAB);
			writer.write(String.valueOf(pf.concurrentVisitors.getMax()));
			writer.write(TAB);
			writer.write(String.valueOf(pf.concurrentVisitors.getMean()));
			writer.write(TAB);
			ActivityFacility f = this.facilities.getFacilities().get(fId);
			for(ActivityOptionImpl opt : f.getActivityOptions().values()) {
				writer.write(opt.getType());
				writer.write(WSPACE);
			}
			writer.newLine();
		}
		writer.close();
	}
	
	private class PhysicalFacility {
		
		private int totalVisitors;
		
		private final DescriptiveStatistics concurrentVisitors = new DescriptiveStatistics();
		
		private final Map<Id, Visitor> visitors = new HashMap<Id, Visitor>(); // <PersonId, Visitor>
		
		private void enterPerson(Id personId, double time) {
			visitors.put(personId, new Visitor(personId, time));
			
			// only for statistics
			totalVisitors++;
		}
		
		private void leavePerson(Id personId, double time) {
			Visitor v = visitors.remove(personId);
			if(v == null)
				throw new RuntimeException("Tried to remove a visitor that did not enter this facility!");
			else {
				
				Collection<Visitor> targets = selector.select(v, visitors.values());
				for(Visitor target : targets) {
					double startTime = Math.max(v.getEnterTime(), target.getEnterTime());
					interactor.interact(population.getPersons().get(v.getPersonId()), population.getPersons().get(target.getPersonId()), startTime, time);
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
