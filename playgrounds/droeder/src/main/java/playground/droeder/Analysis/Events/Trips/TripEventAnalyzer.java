/* *********************************************************************** *
 * project: org.matsim .*
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
package playground.droeder.Analysis.Events.Trips;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.PersonEvent;

/**
 * @author droeder
 *
 */
public class TripEventAnalyzer {
	private static final Logger log = Logger.getLogger(TripEventAnalyzer.class);
	
	private Map<Id, Set<PersonEvent>> personEvents;
	private Set<TransportMode> modes;
	
	
	/**
	 * the syntax of the used ids should be like 123abc_*mode*
	 * @param personEvents
	 * @param modes
	 */
	public TripEventAnalyzer(Map<Id, Set<PersonEvent>> personEvents, Set<TransportMode> modes){
		this.personEvents = personEvents;
		this.modes = modes;
	}
	
}
