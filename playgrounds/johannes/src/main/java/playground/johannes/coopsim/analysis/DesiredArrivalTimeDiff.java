/* *********************************************************************** *
 * project: org.matsim.*
 * DesiredArrivalTimeDiff.java
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
package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;

/**
 * @author illenberger
 *
 */
public class DesiredArrivalTimeDiff extends DesireDifference {

	public DesiredArrivalTimeDiff(Map<Person, ActivityDesires> desires) {
		super(desires);
	}

	@Override
	protected double getDifference(Trajectory t, ActivityDesires desire) {
		Activity act = (Activity) t.getElements().get(2);
		String type = act.getType();
		
		Double desiredStartTime = desire.getActivityStartTime(type);
		if(desiredStartTime != null) {
			double realizedStartTime = t.getTransitions().get(2);
			return realizedStartTime - desiredStartTime;
		} else {
			return Double.NaN;
		}
	}

}
