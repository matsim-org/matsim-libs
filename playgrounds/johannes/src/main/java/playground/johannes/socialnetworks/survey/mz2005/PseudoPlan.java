/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoPlan.java
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
package playground.johannes.socialnetworks.survey.mz2005;

import java.util.ArrayList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class PseudoPlan {

	public List<PseudoActivity> activities = new ArrayList<PseudoActivity>();
	
	public List<TripData> trips = new ArrayList<TripData>();
	
	public String activityChain() {
		StringBuilder builder = new StringBuilder(activities.size() * 10);
		for(PseudoActivity act : activities) {
			builder.append(act.type);
			builder.append("-");
		}
		return builder.toString();
	}
}
