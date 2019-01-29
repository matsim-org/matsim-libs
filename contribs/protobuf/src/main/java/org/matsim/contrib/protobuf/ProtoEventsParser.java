package org.matsim.contrib.protobuf;
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

import org.matsim.contrib.protobuf.events.ProtobufEvents;
import org.matsim.core.api.experimental.events.EventsManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by laemmel on 17/02/16.
 */
public class ProtoEventsParser {

	private final EventsManager em;

	public ProtoEventsParser(EventsManager em) {
		this.em = em;
	}

	public void parse(String file) {
		try {
			FileInputStream fis = new FileInputStream(file);

			ProtobufEvents.Event pe;
			while ((pe = ProtobufEvents.Event.parseDelimitedFrom(fis)) != null) {
				this.em.processEvent(ProtoEvent2Event.getEvent(pe));
			}
			fis.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
