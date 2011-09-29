/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.archive;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */

public class BusCorridorMoneyEvent implements AgentMoneyEvent{

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.PersonEvent#getPersonId()
	 */
	@Override
	public Id getPersonId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.Event#getTime()
	 */
	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.Event#getAttributes()
	 */
	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.AgentMoneyEvent#getAmount()
	 */
	@Override
	public double getAmount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
}
