/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEventsManager.java
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
package playground.gregor.sim2d.events;

import java.util.ArrayList;
import java.util.List;

public class XYZEventsManager {
	
	private List<XYZEventsHandler> handler = new ArrayList<XYZEventsHandler>();
	
	public void processXYZEvent(XYZEvent e) {
		for (XYZEventsHandler h : this.handler) {
			h.handleXYZEvent(e);
		}
	}
	
	public void reset() {
		for (XYZEventsHandler h : this.handler) {
			h.reset();
		}		
	}
	
	public void addHandler(XYZEventsHandler h) {
		this.handler.add(h);
	}

}
