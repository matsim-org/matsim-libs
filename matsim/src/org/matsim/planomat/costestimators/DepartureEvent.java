/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureEvent.java
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

package org.matsim.planomat.costestimators;

import org.matsim.basic.v01.Id;

public class DepartureEvent {

	private Id agentId;
	//private String legId;

	public DepartureEvent(Id agentId, String legId) {
		super();
		this.agentId = agentId;
		//this.legId = legId;
	}

	@Override
	public boolean equals(Object arg0) {

		DepartureEvent event = (DepartureEvent)arg0;
//		return (this.agentId.equals(event.agentId) && this.legId.equals(event.legId));
		return (this.agentId.equals(event.agentId));
	}

	@Override
	public String toString() {
//		return "[[agentId = " + agentId + "][legId = " + legId + "]]";
		return "[[agentId = " + agentId + "]";
	}

	@Override
	public int hashCode() {
		return agentId.hashCode();
	}
	
}
