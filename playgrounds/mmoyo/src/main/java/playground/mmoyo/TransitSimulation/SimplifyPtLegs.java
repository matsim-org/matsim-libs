/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.TransitSimulation;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
/**
 *Deletes all pt activities in a plan
 */
public class SimplifyPtLegs {

	public SimplifyPtLegs(){

	}

	public void run(Plan plan){
		for (int i= plan.getPlanElements().size()-1; i>=0; i--) {
			PlanElement pe = plan.getPlanElements().get(i);
			if (pe instanceof Activity) {
				String t = ((Activity)pe).getType();
				if (t.equals("transf") || t.equals("transf on") || t.equals("transf off") || t.equals("wait pt") || t.equals("exit pt veh")){
					((PlanImpl) plan).removeActivity(i);
				}
			}else{
				Leg leg = ((Leg)pe);
				leg.setMode(TransportMode.pt);  //-> improve this, some of these legs are deleted
			}

		}
	}

}