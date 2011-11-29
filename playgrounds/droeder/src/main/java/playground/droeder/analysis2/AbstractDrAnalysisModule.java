/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.analysis2;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;
import org.matsim.core.events.handler.LinkEventHandler;

/**
 * @author droeder
 *
 */
public abstract class AbstractDrAnalysisModule implements LinkEventHandler, PersonEventHandler{
	
	public abstract void processEvent(Event e);
	
	public abstract void dumpResults(String outDir);

	@Override
	public void handleEvent(PersonEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(LinkEvent event) {
		this.processEvent(event);
	}

}
