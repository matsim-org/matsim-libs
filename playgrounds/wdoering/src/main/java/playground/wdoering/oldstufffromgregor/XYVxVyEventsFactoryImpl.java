/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEventsFactoryImpl.java
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
package playground.wdoering.oldstufffromgregor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

/**
 * @author laemmel
 * 
 */
public class XYVxVyEventsFactoryImpl {

	public XYVxVyEventsFactoryImpl() {
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param vx
	 * @param vy
	 * @param id
	 * @param time
	 * @return
	 */
	public Event createXYZAzimuthEvent(String x, String y, String vx, String vy, String id, String time) {
		XYVxVyEventImpl e = new XYVxVyEventImpl(Id.create(id, Person.class), Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(vx), Double.parseDouble(vy), Double.parseDouble(time));
		return e;
	}

}
