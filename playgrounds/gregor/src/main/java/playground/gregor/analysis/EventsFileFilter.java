package playground.gregor.analysis;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * Created by laemmel on 26/11/15.
 */
public class EventsFileFilter implements BasicEventHandler {

	private final EventsManager em;

	public EventsFileFilter(EventsManager em) {
		this.em = em;
	}

	public static void main(String[] args) {
		String input = "/Volumes/data/svn/runs-svn/patnaIndia/run105/1pct/evac_seepage/ITERS/it.100/100.events.xml.gz";
		String output = "/Volumes/data/svn/runs-svn/patnaIndia/run105/1pct/evac_seepage/ITERS/it.100/100.filtered_events.xml.gz";

		EventsManager em1 = new EventsManagerImpl();
		EventsManager em2 = new EventsManagerImpl();
		EventsFileFilter filter = new EventsFileFilter(em1);
		em2.addHandler(filter);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(em2);
		EventWriterXML writer = new EventWriterXML(output);
		em1.addHandler(writer);
		reader.parse(input);
		writer.closeFile();
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof LinkEnterEvent) {// || event instanceof LinkLeaveEvent) {
			Id<Link> id = ((LinkEnterEvent) event).getLinkId();
			if (id.toString().contains("el")) {
				return;
			}
		}
		if (event instanceof LinkLeaveEvent) {// || event instanceof LinkLeaveEvent) {
			Id<Link> id = ((LinkLeaveEvent) event).getLinkId();
			if (id.toString().contains("el")) {
				return;
			}
		}
		this.em.processEvent(event);
	}

	@Override
	public void reset(int iteration) {

	}
}
