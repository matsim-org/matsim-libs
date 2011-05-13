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

import org.matsim.core.events.EventImpl;

/**
 * @author nagel
 *
 */
 class DispatcherTaxiRequestEvent extends EventImpl {

	/**
	 * @param time
	 */
	DispatcherTaxiRequestEvent(double time) {
		super(time);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getEventType() {
		// yyyy Auto-generated method stub
		return null;
	}

}
