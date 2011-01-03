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

package playground.anhorni.scenarios;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;


public class SinglePlanGenerator {

	private int shopCityLinkIds[];
	
	public SinglePlanGenerator(int shopCityLinkIds[]) {
		this.shopCityLinkIds = shopCityLinkIds;
	}
	
	public PlanImpl generatePlan(int homeTownIndex, boolean shopCity, int shopCityIndex) {
		PlanImpl plan = new PlanImpl();
	
		int homeId;
		int workId;
		
		if (homeTownIndex == 0) {
			homeId = 1;
			workId = 5;
		}
		else {
			homeId = 10;
			workId = 6;
		}
		ActivityImpl actH = new ActivityImpl("h", new IdImpl(homeId));
		actH.setEndTime(8.5 * 3600);
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actW = new ActivityImpl("w", new IdImpl(workId));
		actW.setEndTime(17 * 3600);
		plan.addActivity(actW);		
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actS;
		if (shopCity) {
			actS = new ActivityImpl("sc", new IdImpl(shopCityLinkIds[shopCityIndex]));
			actS.setEndTime(20 * 3600);
		}
		else {
			actS = new ActivityImpl("sh", new IdImpl(homeId));
			actS.setEndTime(18 * 3600);
		}
		plan.addActivity(actS);
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actH2 = new ActivityImpl("h", new IdImpl(homeId));
		plan.addActivity(actH2);	
				
		return plan;
	}
}
