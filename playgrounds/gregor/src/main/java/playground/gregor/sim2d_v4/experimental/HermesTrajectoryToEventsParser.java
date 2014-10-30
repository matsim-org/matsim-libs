/* *********************************************************************** *
 * project: org.matsim.*
 * HermesTrajectoryToEventsParser.java
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

package playground.gregor.sim2d_v4.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.StringUtils;

import playground.gregor.sim2d_v4.events.Sim2DAgentConstructEvent;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class HermesTrajectoryToEventsParser {

	private final EventsManager em;

	public HermesTrajectoryToEventsParser(EventsManager e) {
		this.em = e;
	}

	public void parse(String string) {
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(new File(string)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String line = null;
		try {
			line = r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		EventsComparator ecomp = new EventsComparator();
		Queue<Event> q = new PriorityQueue<Event>(100,ecomp);
		
		Set<Id<Person>> a = new HashSet<>();
		while (line != null) {
			String[] expl = StringUtils.explode(line, ' ');
			
			int frame = Integer.parseInt(expl[1]);
			double time = frame/16.;
			Id<Person> id = Id.create(expl[0], Person.class);
			if (!a.contains(id)){
				Sim2DAgentConstructEvent ac = new Sim2DAgentConstructEvent(time, new Sim2DAgent(id));
				q.add(ac);
				
			}
			double x = Double.parseDouble(expl[2])/100;
			double y = Double.parseDouble(expl[3])/100;
			XYVxVyEventImpl e = new XYVxVyEventImpl(id, x, y, 0, 0, time);
			q.add(e);
			try {
				line = r.readLine();
			} catch (IOException ee) {
				// TODO Auto-generated catch block
				ee.printStackTrace();
			}			
		}
		
		while (q.size() > 0) {
			Event e = q.poll();
			this.em.processEvent(e);
		}
	}
	
	private final class EventsComparator implements Comparator<Event> {

		@Override
		public int compare(Event o1, Event o2) {

			if (o1.getTime() < o2.getTime()) {
				return -1;
			} else if (o1.getTime() > o2.getTime()) {
				return 1;
			}
			return 0;
		}
		
	}

}
