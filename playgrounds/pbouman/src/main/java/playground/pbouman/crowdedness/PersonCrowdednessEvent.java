/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.pbouman.crowdedness;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.internal.HasPersonId;

public class PersonCrowdednessEvent extends Event implements HasPersonId {

	public PersonCrowdednessEvent(double time) {
		super(time);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getPersonId() {
		// TODO Auto-generated method stub
		return null;
	}

}
