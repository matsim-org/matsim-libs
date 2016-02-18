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

import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.protobuf.events.ProtobufEvents;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by laemmel on 17/02/16.
 */
public class ProtoEventsWriter implements BasicEventHandler{

	private final FileOutputStream fos;

	public ProtoEventsWriter(FileOutputStream fos) {
		this.fos = fos;
	}


	@Override
	public void handleEvent(Event event) {
		ProtobufEvents.Event pe = Event2ProtoEvent.getProtoEvent(event);
		try {
			pe.writeDelimitedTo(this.fos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {

	}
}
