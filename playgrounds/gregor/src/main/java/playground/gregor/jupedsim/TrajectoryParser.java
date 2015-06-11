/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.jupedsim;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.gregor.hybridsim.events.ExternalAgentConstructEvent;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

public class TrajectoryParser extends MatsimXmlParser {

	private static final String TRAJECTORIES = "trajectories";
	private static final String HEADER = "header";
	private static final String GEOMETRY = "geometry";
	private static final String FILE = "file";
	private static final String HLINE = "hline";
	private static final String POINT = "point";
	private static final String FRAME_RATE = "frameRate";
	private static final String FRAME = "frame";
	private static final String AGENT = "agent";

	private static final Logger log = Logger.getLogger(TrajectoryParser.class);

	private double onsetX = 1113711.9263903373;
	private double onsetY = 7041180.26922217;
	private double frameRate;
	private double time;
	private PriorityQueue<Event> q;
	private ReentrantLock lock;

	private Set<Id<Person>> constructed = new HashSet<>();

	public TrajectoryParser(PriorityQueue<Event> q, ReentrantLock lock) {
		this.q = q;
		this.lock = lock;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (FRAME.equals(name)) {
			this.time = Double.parseDouble(atts.getValue("ID"))
					* this.frameRate;
		} else if (AGENT.equals(name)) {

			String id = atts.getValue("ID");
			Id<Person> mid = Id.createPersonId(id);
			if (!this.constructed.contains(mid)) {
				ExternalAgentConstructEvent ee = new ExternalAgentConstructEvent(
						time, mid);
				this.lock.lock();
				this.q.add(ee);
				this.constructed.add(mid);
				this.lock.unlock();
			}

			double x = Double.parseDouble(atts.getValue("x")) + onsetX;
			double y = Double.parseDouble(atts.getValue("y")) + onsetY;
			double angle = Double.parseDouble(atts.getValue("eO"));
			double dx = Math.cos(Math.PI*angle/180);
			double dy = Math.sin(Math.PI*angle/180);
			XYVxVyEventImpl e = new XYVxVyEventImpl(mid, x, y, dx, dy, this.time);
			if (!this.lock.isHeldByCurrentThread()) {
				this.lock.lock();
			}
			this.q.add(e);
			if (this.time - q.peek().getTime() > 10) {
				this.lock.unlock();
			}

		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (FRAME_RATE.equals(name)) {
			this.frameRate = Double.parseDouble(content);
		}

	}

}
