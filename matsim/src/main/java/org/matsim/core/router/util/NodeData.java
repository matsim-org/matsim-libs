/* *********************************************************************** *
 * project: org.matsim.*
 * NodeData.java
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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Link;

public interface NodeData {

	public void resetVisited();
	
	public void visit(final Link comingFrom, final double cost, final double time, final int iterID);
	
	public boolean isVisited(final int iterID);
	
	public double getCost();
	
	public double getTime();
	
	public Link getPrevLink();	
}