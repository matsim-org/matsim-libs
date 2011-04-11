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

package playground.anhorni.PLOC;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;


public class SinglePlanGenerator {
	
	private ActivityFacilities facilities;
	private boolean tempVar;
	
	public SinglePlanGenerator(ActivityFacilities facilities, boolean tempVar) {
		this.facilities = facilities;
		this.tempVar = tempVar;
	}
	
	public PlanImpl generatePlan(boolean worker, PersonImpl p, int day) {
		PlanImpl plan;
		if (worker) plan = generateWorkPlan(p, day);
		else plan = generateNonWorkPlan(p); 
		return plan;
	}
	
	private PlanImpl generateNonWorkPlan(PersonImpl p) {
		
		int homeIndex = 1;
		int facilityIndex = 1;
		if ((Integer)p.getCustomAttributes().get("townId") == 1) {
			homeIndex = 8;
			facilityIndex = 2;
		}
		Id facilityId = new IdImpl(facilityIndex);
		
		PlanImpl plan = new PlanImpl();
		ActivityImpl actH = new ActivityImpl("h", new IdImpl(homeIndex));
		actH.setFacilityId(facilityId);
		actH.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		
		actH.setStartTime(0.0);
		actH.setMaximumDuration(10.0 * 3600.0);
		actH.setEndTime(10.0 * 3600.0);
		
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));
				
		ActivityImpl actS = new ActivityImpl("s", new IdImpl(homeIndex));
		
		actS.setStartTime(10.0 * 3600.0);
		actS.setMaximumDuration(90.0 * 60.0);
		actS.setEndTime(11.5 * 3600);
		
		actS.setFacilityId(facilityId);
		actS.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		plan.addActivity(actS);
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actH2 = new ActivityImpl("h", new IdImpl(homeIndex));
		
		actH2.setStartTime(11.5 * 3600);
		actH2.setFacilityId(facilityId);
		
		actH2.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		plan.addActivity(actH2);
		return plan;
	}
	
	private PlanImpl generateWorkPlan(PersonImpl p, int day) {
		int homeIndex = 1;
		int facilityIndex = 1;
		if ((Integer)p.getCustomAttributes().get("townId") == 1) {
			homeIndex = 8;
			facilityIndex = 2;
		}
		Id facilityId = new IdImpl(facilityIndex);
		
		double time = 0.0;
		
		PlanImpl plan = new PlanImpl();
		ActivityImpl actH = new ActivityImpl("h", new IdImpl(homeIndex));
		actH.setFacilityId(facilityId);
		actH.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		
		actH.setStartTime(time);
		double maxDuration = 8.0 * 3600.0;
		actH.setMaximumDuration(maxDuration);
		time += maxDuration;
		actH.setEndTime(time);
		
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));

		int workIndex = 17; 
		int workFacilityIndex = 3;

		Id workFacilityId = new IdImpl(workFacilityIndex);
		ActivityImpl actW = new ActivityImpl("w", new IdImpl(workIndex));
		actW.setFacilityId(workFacilityId);
		actW.setCoord(this.facilities.getFacilities().get(workFacilityId).getCoord());
		
		actW.setStartTime(time);
		if (tempVar) {
			maxDuration = 9.0 * 3600.0;
			if (day == 4) maxDuration = 7.0 * 3600.0;
		}
		else {
			maxDuration = 8.5 * 3600.0;
		}
		
		time += maxDuration;
		actW.setMaximumDuration(maxDuration);
		actW.setEndTime(time);
		plan.addActivity(actW);	
		
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actS = new ActivityImpl("s", new IdImpl(homeIndex));
		actS.setFacilityId(facilityId);
		actS.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		actS.setStartTime(time);
		maxDuration = 90.0 * 60.0;
		actS.setMaximumDuration(maxDuration);
		time += maxDuration;
		actS.setEndTime(time);
		plan.addActivity(actS);
		
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actH2 = new ActivityImpl("h", new IdImpl(homeIndex));
		actH2.setFacilityId(facilityId);
		actH2.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		actH2.setStartTime(time);
		
		plan.addActivity(actH2);
		return plan;
	}
}
