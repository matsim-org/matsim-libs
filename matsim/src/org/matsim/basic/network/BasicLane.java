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
package org.matsim.basic.network;

import java.util.List;

import org.matsim.api.basic.v01.Id;
/**
 * 
 * @author dgrether
 *
 */
public interface BasicLane {

	/**
	 * @param number
	 */
	public void setNumberOfRepresentedLanes(int number);

	public void setLength(double meter);

	public Id getId();

	public int getNumberOfRepresentedLanes();

	public double getLength();

	public void addToLinkId(Id id);

	public List<Id> getToLinkIds();

}