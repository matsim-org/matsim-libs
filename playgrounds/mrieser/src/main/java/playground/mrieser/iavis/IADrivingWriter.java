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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class IADrivingWriter implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler {

	private final static Logger log = Logger.getLogger(IADrivingWriter.class);

	private final Network network;
	private final BufferedWriter writer;
	private final Map<Id, Double> linkEnterTimes = new HashMap<Id, Double>(10000);

	public IADrivingWriter(final Network network, final String filename) throws FileNotFoundException, IOException {
		this.network = network;
		this.writer = IOUtils.getBufferedWriter(filename);
		this.writer.write("agentId\t");
		this.writer.write("fromX\t");
		this.writer.write("fromY\t");
		this.writer.write("toX\t");
		this.writer.write("toY\t");
		this.writer.write("startTime\t");
		this.writer.write("endTime\n");
	}

	public void close() {
		if (this.writer == null) {
			try { this.writer.close(); }
			catch (IOException e) { log.warn("Could not close writer.", e); }
		}
	}

	private void writeData(String id, Coord from, Coord to, double startTime, double endTime) {
		try {
			this.writer.write(id + "\t");
			this.writer.write(from.getX() + "\t");
			this.writer.write(from.getY() + "\t");
			this.writer.write(to.getX() + "\t");
			this.writer.write(to.getY() + "\t");
			this.writer.write(startTime + "\t");
			this.writer.write(endTime + "\n");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.linkEnterTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void reset(int iteration) {
		this.linkEnterTimes.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double enterTime = this.linkEnterTimes.remove(event.getPersonId());
		if (enterTime != null) {
			Link link = this.network.getLinks().get(event.getLinkId());
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			writeData(event.getPersonId().toString(), from, to, enterTime, event.getTime());
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Double enterTime = this.linkEnterTimes.remove(event.getPersonId());
		if (enterTime != null) {
			Link link = this.network.getLinks().get(event.getLinkId());
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			writeData(event.getPersonId().toString(), from, to, enterTime, event.getTime());
		}
	}
}
