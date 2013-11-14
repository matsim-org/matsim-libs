/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPersonEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thibautd
 */
abstract class AbstractPersonEvent extends Event implements HasPersonId {
	private final Id personId;
	private final Id facilityId;

	/*package*/ AbstractPersonEvent(
			final Event event) {
		this( event.getTime() ,
				new IdImpl(
					event.getAttributes().get( "person" ) ), 
				new IdImpl(
					event.getAttributes().get( "facility" ) ) );
	}

	public AbstractPersonEvent(
			final double time,
			final Id personId,
			final Id facilityId) {
		super( time );
		this.personId = personId;
		this.facilityId = facilityId;
	}

	@Override
	public Id getPersonId() {
		return personId;
	}

	public Id getFacilityId() {
		return facilityId;
	}

	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> map = super.getAttributes();
		map.put( "person" , personId.toString() );
		map.put( "facility" , facilityId.toString() );
		return map;
	}
}

