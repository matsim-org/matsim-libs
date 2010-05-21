/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.iavis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.xml.sax.SAXException;

public class IAActivitiesWriter implements ActivityStartEventHandler, ActivityEndEventHandler {

	private final static Logger log = Logger.getLogger(IAActivitiesWriter.class);

	private final Scenario s;
	private final BufferedWriter writer;
	private final Map<Id, ActivityStartEvent> startEvents = new HashMap<Id, ActivityStartEvent>(10000);
	private final Map<Id, Integer> actCounter = new HashMap<Id, Integer>(10000);
	private final Map<Id, List<Coord>> actCoords = new HashMap<Id, List<Coord>>(10000);

	public IAActivitiesWriter(final Scenario s, final String popFilename, final String filename) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		this.s = s;
		this.writer = IOUtils.getBufferedWriter(filename);
		this.writer.write("personId\t");
		this.writer.write("fromTime\t");
		this.writer.write("toTime\t");
		this.writer.write("act-type\t");
		this.writer.write("x\t");
		this.writer.write("y\n");
		this.loadActLocations(popFilename);
	}

	private void loadActLocations(String popFilename) throws SAXException, ParserConfigurationException, IOException {
		log.info("loading activity coordinates...");
		PopulationImpl p = (PopulationImpl) this.s.getPopulation();
		p.setIsStreaming(true);
		p.addAlgorithm(new ActLocationExtractor(this.actCoords));
		new MatsimPopulationReader(s).parse(popFilename);
		log.info("done.");
	}

	public void close() {
		if (this.writer == null) {
			try { this.writer.close(); }
			catch (IOException e) { log.warn("Could not close writer.", e); }
		}
	}

	private void writeData(String id, double fromTime, double toTime, String type, Coord c) {
		try {
			this.writer.write(id + "\t");
			this.writer.write(fromTime + "\t");
			this.writer.write(toTime + "\t");
			this.writer.write(type + "\t");
			this.writer.write(c.getX() + "\t");
			this.writer.write(c.getY() + "\n");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.startEvents.put(event.getPersonId(), event);
	}

	@Override
	public void reset(int iteration) {
		this.startEvents.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		ActivityStartEvent startEvent = this.startEvents.get(event.getPersonId());
		Integer cnt = this.actCounter.get(event.getPersonId());
		if (cnt == null) {
			cnt = 0;
		}
		Coord c = this.actCoords.get(event.getPersonId()).get(cnt);
		this.actCounter.put(event.getPersonId(), cnt + 1);

		if (startEvent == null) {
			// assume it's the first of the day
			writeData(event.getPersonId().toString(), 0, event.getTime(), event.getActType(), c);
		}
		if (startEvent != null) {
			writeData(event.getPersonId().toString(), startEvent.getTime(), event.getTime(), event.getActType(), c);
		}
	}

	private static class ActLocationExtractor implements PersonAlgorithm {

		private final Map<Id, List<Coord>> actCoords;

		public ActLocationExtractor(final Map<Id, List<Coord>> actCoords) {
			this.actCoords = actCoords;
		}

		@Override
		public void run(Person person) {
			Plan p = person.getSelectedPlan();
			ArrayList<Coord> coords = new ArrayList<Coord>();
			for (PlanElement pe : p.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity a = (Activity) pe;
					coords.add(a.getCoord());
				}
			}
			coords.trimToSize();
			this.actCoords.put(person.getId(), coords);
		}

	}
}
