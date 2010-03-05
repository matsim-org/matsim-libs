/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatXPlan.java
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
package playground.mfeil;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;


/**
 * @author Matthias Feil
 * Extends the standard Plan object to enable sorting of arrays or lists of Plans after their scores.
 * Use 
 * java.util.Arrays.sort (nameOfArray[]) 
 * or
 * java.util.Collections.sort (nameOfList<>).
 * See e.g., PlanomatX class.
 */
public class PlanomatXPlan extends PlanImpl implements Comparable<PlanomatXPlan>{

	public PlanomatXPlan (Person person){
		super(person);
	}
	
	public final int compareTo(PlanomatXPlan p){
		return this.getScore().compareTo(p.getScore());
//		if (this.getScoreAsPrimitiveType() == p.getScoreAsPrimitiveType()) {
//			return 0;
//		}
//		else if (this.getScoreAsPrimitiveType() - p.getScoreAsPrimitiveType() > 0.0){
//			return 1;
//		}
//		else return -1;
	}
	
	public void setActsLegs (List<? extends PlanElement> actslegs){
		List planElements = getPlanElements();
		planElements.clear();
		planElements.addAll(actslegs);
	}
	/*
	public void copyTimes (ArrayList<Object> in){
		try {
			Act a = (Act)this.actsLegs.get(0);
			Act a0 = (Act)in.get(0);
			a.setDuration(a0.getDuration());
			a.setStartTime(a0.getStartTime());
			a.setEndTime(a0.getEndTime());
			
			for (int i=1; i<in.size()-1; i+=2) {
				Leg lx = (Leg)this.actsLegs.get(i);
				Leg lx0 = (Leg)in.get(i);
				lx.setTravelTime(lx0.getTravelTime());
				lx.setDepartureTime(lx0.getDepartureTime());
				lx.setArrivalTime(lx0.getArrivalTime());
				
				Act ax = (Act)this.actsLegs.get(i+1);
				Act ax0 = (Act)in.get(i+1);
				ax.setDuration(ax0.getDuration());
				ax.setStartTime(ax0.getStartTime());
				ax.setEndTime(ax0.getEndTime());
			}
		} catch (Exception e) {
			// copying a plan is fairly basic. if an exception occurs here, something
			// must be definitively wrong -- exit with an error
			Gbl.errorMsg(e);
		}
		
	}
	*/
}
