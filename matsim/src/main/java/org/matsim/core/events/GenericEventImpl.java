/* *********************************************************************** *
 * project: matsim
 * GenericEventImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.api.experimental.events.GenericEvent;

/**
 * @author nagel
 *
 */
public class GenericEventImpl implements GenericEvent {
	private final Map<String,String> atts = new TreeMap<String,String>() ;
	private final double time ;
	
	public GenericEventImpl( String type, double time ) {
		this.time = time ;
		atts.put("type", type );
	}

	@Override
	public Map<String, String> getAttributes() {
		return atts ;
	}

	@Override
	public double getTime() {
		return time ;
	}

}
