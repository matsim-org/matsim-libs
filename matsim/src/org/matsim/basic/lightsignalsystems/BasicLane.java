/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.basic.lightsignalsystems;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicLane {

	private Id id;
	private double numberOfRepresentedLanes;
	private double length;
	private List<Id> toLinkIds;

	/**
	 * @param id
	 */
	public BasicLane(Id id) {
		this.id = id;
	}

	/**
	 * @param number
	 */
	public void setNumberOfRepresentedLanes(double number) {
		this.numberOfRepresentedLanes = number;
	}

	public void setLength(double meter) {
		this.length = meter;
	}

	public Id getId() {
		return id;
	}

	
	public double getNumberOfRepresentedLanes() {
		return numberOfRepresentedLanes;
	}

	
	public double getLength() {
		return length;
	}

	public void addToLinkId(Id id) {
		if (this.toLinkIds == null) {
			this.toLinkIds = new ArrayList<Id>();
		}
		this.toLinkIds.add(id);
	}
	
	public List<Id> getToLinkIds() {
		return this.toLinkIds;
	}

	
}
