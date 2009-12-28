/* *********************************************************************** *
 * project: org.matsim.*
 * Activity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.view;

import org.matsim.api.core.v01.network.Link;


/**
 * @author illenberger
 *
 */
public interface Activity extends PlanElement {
	
	public Link getLink();
	
	public void setLink(Link link);
	
	public Facility getFacility();
	
	public void setFacility(Facility facility);
	
	public String getType();
	
	public void setType(String type);
}
