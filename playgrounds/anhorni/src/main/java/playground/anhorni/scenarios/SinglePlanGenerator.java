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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;


public class SinglePlanGenerator {
	
	public PlanImpl generatePlan(int homeIndex, boolean worker, PersonImpl p) {
		PlanImpl plan;
		if (worker) plan = generateWorkPlan(homeIndex, p);
		else plan = generateNonWorkPlan(homeIndex); 
		return plan;
	}
	
	private PlanImpl generateNonWorkPlan(int homeIndex) {
		
		int facilityIndex = 1;
		if (homeIndex == 8) facilityIndex = 2;
		
		PlanImpl plan = new PlanImpl();
		ActivityImpl actH = new ActivityImpl("h", new IdImpl(homeIndex));
		actH.setFacilityId(new IdImpl(facilityIndex));
		actH.setEndTime(14 * 3600);
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));
				
		ActivityImpl actS = new ActivityImpl("s", new IdImpl(homeIndex));
		actS.setEndTime(17 * 3600);
		actS.setFacilityId(new IdImpl(facilityIndex));
		plan.addActivity(actS);
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actH2 = new ActivityImpl("h", new IdImpl(homeIndex));
		actH2.setFacilityId(new IdImpl(facilityIndex));
		plan.addActivity(actH2);
		return plan;
	}
	
	private PlanImpl generateWorkPlan(int homeIndex, PersonImpl p) {
		
		int facilityIndex = 1;
		if (homeIndex == 8) facilityIndex = 2;
		
		PlanImpl plan = new PlanImpl();
		ActivityImpl actH = new ActivityImpl("h", new IdImpl(homeIndex));
		actH.setFacilityId(new IdImpl(facilityIndex));
		actH.setEndTime(7.0 * 3600);
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));
		
		int workIndex = 9; int workFacilityIndex = 3;
		if ((Boolean) p.getCustomAttributes().get("cityWorker")) {
			workIndex = 17; workFacilityIndex = 4;
		}
		ActivityImpl actW = new ActivityImpl("w", new IdImpl(workIndex));
		actW.setFacilityId(new IdImpl(workFacilityIndex));
		actW.setEndTime(17 * 3600);
		plan.addActivity(actW);	
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actS = new ActivityImpl("s", new IdImpl(homeIndex));
		actS.setFacilityId(new IdImpl(facilityIndex));
		actS.setEndTime(20 * 3600);
		plan.addActivity(actS);
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actH2 = new ActivityImpl("h", new IdImpl(homeIndex));
		actH2.setFacilityId(new IdImpl(facilityIndex));
		plan.addActivity(actH2);
		return plan;
	}
}
