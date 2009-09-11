/* *********************************************************************** *
 * project: org.matsim.*
 * PlainPlanElementImpl.java
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
package playground.johannes.plans.plain.impl;

import playground.johannes.plans.plain.PlainPlanElement;

/**
 * @author illenberger
 *
 */
public class PlainPlanElementImpl extends AbstractModifiable implements PlainPlanElement {

	private Double endTime;
	
	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(Double endTime) {
		this.endTime = endTime;
		modified();
	}

}
