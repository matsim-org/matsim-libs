/* *********************************************************************** *
 * project: org.matsim.*
 * Plan.java
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

package teach.multiagent07.population;

import org.matsim.basic.v01.BasicPlan;

public class Plan extends BasicPlan {

	public Plan() {
		
	}
	
	// copy constructor
	public Plan(Plan bluePrint) {
		this.setScore(bluePrint.getScore());
		
		ActLegIterator iter = bluePrint.getIterator();
		Activity act = (Activity)iter.nextAct();
		this.addAct(new Activity(act));
		
		while ( iter.hasNextLeg()) {
			Leg leg = (Leg)iter.nextLeg();
			this.addLeg(new Leg(leg));
			
			act = (Activity)iter.nextAct();
			this.addAct(new Activity(act));
		}
	}
}
