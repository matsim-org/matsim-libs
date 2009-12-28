/* *********************************************************************** *
 * project: org.matsim.*
 * DesiredDurationPerson.java
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

package playground.anhorni.locationchoice.preprocess.plans.modifications.helper;

import org.matsim.api.core.v01.Id;

public class DesiredDurationPerson {

	private Id personId;
	private double duration;
	private boolean planContainsLeisurePriorToWork;
	
	public DesiredDurationPerson(Id personId, double duration, boolean planContainsLeisurePriorToWork) {
		this.personId = personId;
		this.duration = duration;
		this.planContainsLeisurePriorToWork = planContainsLeisurePriorToWork;
	}

	public Id getPersonId() {
		return personId;
	}

	public void setPersonId(Id personId) {
		this.personId = personId;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public boolean planContainsLeisurePriorToWork() {
		return planContainsLeisurePriorToWork;
	}
}
