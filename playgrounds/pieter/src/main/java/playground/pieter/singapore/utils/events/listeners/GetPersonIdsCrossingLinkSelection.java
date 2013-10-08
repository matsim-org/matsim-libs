/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterXML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.pieter.singapore.utils.events.listeners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.*;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class GetPersonIdsCrossingLinkSelection implements BasicEventHandler {
	private BufferedWriter out = null;
	private HashSet<String> linkIds;
	private HashSet<String> personIds;

	public void reset(int iteration) {
		
	}


	public void handleEvent(Event event) {
		Map<String, String> attrs = event.getAttributes();
		if(linkIds.contains(attrs.get("link"))){
			personIds.add(attrs.get("person"));
		}
	}

	public GetPersonIdsCrossingLinkSelection(HashSet<String> linkIds2) {
		this.linkIds = linkIds2;
		this.personIds = new HashSet<String>();
	}


	public HashSet<String> getPersonIds() {
		return personIds;
	}




}
