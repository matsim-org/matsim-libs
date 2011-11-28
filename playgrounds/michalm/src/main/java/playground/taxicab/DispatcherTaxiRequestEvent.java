/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.taxicab;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.events.*;

/**
 * @author nagel
 *
 */
class DispatcherTaxiRequestEvent extends EventImpl {

	public static final String EVENT_TYPE = "dispatcherTaxiRequestEvent" ;

	public static final String ATTRIBUTE_LINK = "linkId" ;
	public static final String ATTRIBUTE_PASSENGER = "passengerId" ; 

	private Id linkId ;
	private Id passengerId ;

	DispatcherTaxiRequestEvent(double time, Id linkId, Id passengerId) {
		// careful, linkId and agentId come in other sequence than in PassengerTaxiRequestEvent.  There is some logic
		// in this; it is still not very safe.  NEED TYPED IDs!!
		super(time);
		this.linkId = linkId ;
		this.passengerId = passengerId ;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_PASSENGER, this.passengerId.toString());
		return attr;
	}


	@Override
	public String getEventType() {
		return EVENT_TYPE ;
	}

	public Id getLinkId() {
		return linkId;
	}

	public Id getPassengerId() {
		return passengerId;
	}

}
