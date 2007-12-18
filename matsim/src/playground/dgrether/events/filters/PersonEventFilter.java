/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package playground.dgrether.events.filters;

import java.util.Set;

import org.matsim.basic.v01.Id;
import org.matsim.events.BasicEvent;
import org.matsim.utils.identifiers.IdI;


/**
 * @author dgrether
 *
 */
public class PersonEventFilter implements EventFilter {

	private Set<IdI> personIds;

	public PersonEventFilter(final Set<IdI> personIDs) {
		this.personIds = personIDs;
	}


	/**
	 * @see playground.dgrether.events.filters.EventFilter#judge(org.matsim.events.BasicEvent)
	 */
	public boolean judge(BasicEvent event) {
		return this.personIds.contains(new Id(event.agentId));
	}
}
