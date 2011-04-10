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
	
	public SinglePlanGenerator(ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	public PlanImpl generatePlan(boolean worker, PersonImpl p) {
		PlanImpl plan;
		if (worker) plan = generateWorkPlan(p);
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
		actH.setEndTime(10 * 3600);
		
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));
				
		ActivityImpl actS = new ActivityImpl("s", new IdImpl(homeIndex));
		
		actS.setStartTime(10.0 * 3600.0);
		actS.setMaximumDuration(90 * 60);
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
	
	private PlanImpl generateWorkPlan(PersonImpl p) {
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
		actH.setMaximumDuration(8.0 * 3600.0);
		actH.setEndTime(8.0 * 3600);
		
		plan.addActivity(actH);		
		plan.addLeg(new LegImpl("car"));

		int workIndex = 17; 
		int workFacilityIndex = 3;

		Id workFacilityId = new IdImpl(workFacilityIndex);
		ActivityImpl actW = new ActivityImpl("w", new IdImpl(workIndex));
		actW.setFacilityId(workFacilityId);
		actW.setCoord(this.facilities.getFacilities().get(workFacilityId).getCoord());
		
		actW.setStartTime(8.0 * 3600.0);
		actW.setMaximumDuration(8.5 * 3600.0);
		actW.setEndTime(16.5 * 3600.0);
		
		plan.addActivity(actW);	
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actS = new ActivityImpl("s", new IdImpl(homeIndex));
		actS.setFacilityId(facilityId);
		actS.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		
		actS.setStartTime(16.5 * 3600.0);
		actS.setMaximumDuration(90.0 * 60.0);
		actS.setEndTime(18.0 * 3600);
		
		plan.addActivity(actS);
		plan.addLeg(new LegImpl("car"));
		
		ActivityImpl actH2 = new ActivityImpl("h", new IdImpl(homeIndex));
		actH2.setFacilityId(facilityId);
		actH2.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
		actH2.setStartTime(18.0 * 3600.0);
		
		plan.addActivity(actH2);
		return plan;
	}
}
