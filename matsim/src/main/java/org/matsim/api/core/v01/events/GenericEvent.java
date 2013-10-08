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

package org.matsim.api.core.v01.events;

import java.util.Map;

/**
 * @author nagel
 *
 */
public class GenericEvent extends Event {
	
	private final String type;
	private final Map<String, String> attributes;
	
	public GenericEvent( String type, double time ) {
		super(time);
		this.type = type;
		this.attributes = super.getAttributes();
	}

  @Override
  public String getEventType() {
      return this.type;
  }
	
	@Override
	public Map<String, String> getAttributes() {
		return this.attributes;
	}


}
