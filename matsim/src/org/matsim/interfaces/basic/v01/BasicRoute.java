/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.interfaces.basic.v01;

import java.util.List;


/**
* @author dgrether
*/
public interface BasicRoute {

	public double getDist();

	public void setDist(final double dist);

	public double getTravelTime();

	public void setTravelTime(final double travelTime);

	public List<Id> getLinkIds();

//	public void setStartLinkId(final Id linkId);

	public Id getStartLinkId();

//	public void setEndLinkId(final Id linkId);

	public Id getEndLinkId();

}