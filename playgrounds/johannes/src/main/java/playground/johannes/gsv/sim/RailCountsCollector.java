/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.sim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import playground.johannes.gsv.analysis.RailCounts;
import playground.johannes.gsv.analysis.TransitLineAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class RailCountsCollector implements TransitBoardEventHandler,
		LinkEnterEventHandler, TransitAlightEventHandler {
	
	private static final Logger logger = Logger.getLogger(RailCountsCollector.class);

	private Map<Id, TransitLine> currentLines;
	
	private RailCounts railCounts;

	private TransitLineAttributes lineAttribues;
	
	public RailCountsCollector(TransitLineAttributes lineAttribs) {
		this.lineAttribues = lineAttribs;
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		railCounts = new RailCounts(lineAttribues);
		currentLines = new HashMap<Id, TransitLine>();
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.TransitAlightEventHandler#handleEvent(playground.johannes.gsv.sim.TransitAlightEvent)
	 */
	@Override
	public void handleEvent(TransitAlightEvent event) {
		if(currentLines.remove(event.getPersonId()) == null) {
			logger.warn(String.format("Person %s alights line %s without ever boarding it.", event.getPersonId(), event.getLine()));
		}

	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// FIXME: Works only if person = vehicle
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		TransitLine line = currentLines.get(personId);
		if(line != null) {
			railCounts.addCounts(event.getLinkId(), line.getId(), 1);
		}

	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.TransitBoardEventHandler#handleEvent(playground.johannes.gsv.sim.TransitBoardEvent)
	 */
	@Override
	public void handleEvent(TransitBoardEvent event) {
		Id personId = event.getPersonId();
		if(currentLines.get(personId) == null) {
			currentLines.put(personId, event.getLine());
		} else {
			logger.warn(String.format("Person %s boards line %s without ever alighting previous line.", personId, event.getLine().getId().toString()));
		}

	}
	
	public RailCounts getRailCounts() {
		return railCounts;
	}

}
