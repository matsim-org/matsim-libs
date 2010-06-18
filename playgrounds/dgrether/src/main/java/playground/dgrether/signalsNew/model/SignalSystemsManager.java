/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsManager
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
package playground.dgrether.signalsNew.model;

import org.matsim.core.api.experimental.events.EventsManager;


/**
 * @author dgrether
 *
 */
public interface SignalSystemsManager {

	public EventsManager getEventsManager();
	
	public void setEventsManager(EventsManager events);
	
}
