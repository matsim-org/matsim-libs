/* *********************************************************************** *
 * project: org.matsim.*
 * TimeDependentColorizer.java
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

package playground.gregor.snapshots.postprocessor.processors;

import org.matsim.plans.Plans;

public class TimeDependentColorizer implements PostProcessorI{

	private Plans plans;

	public TimeDependentColorizer(Plans plans) {
		this.plans = plans;
	}

	public String[] processEvent(String[] event){
		String id = event[0];
		int time =  (int) (-60 * this.plans.getPerson(id).getSelectedPlan().getScore() / 6);
		event[7] = Integer.toString(Math.min(time,255));
		return event;
	}
	
	
}
