/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityDesires.java
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
package playground.johannes.coopsim.mental;

import java.util.HashMap;
import java.util.Map;

import org.matsim.population.Desires;

/**
 * @author illenberger
 *
 */
public class ActivityDesires extends Desires {

	private final Map<String, Double> startTimes;
	
	public ActivityDesires() {
		super(null);
		startTimes = new HashMap<String, Double>();
	}
	
	public void putActivityStartTime(String type, Double time) {
		startTimes.put(type, time);
	}
	
	public Double getActivityStartTime(String type) {
		return startTimes.get(type);
	}

}
