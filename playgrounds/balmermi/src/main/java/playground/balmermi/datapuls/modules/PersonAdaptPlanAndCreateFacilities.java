/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFacility2Link
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

package playground.balmermi.datapuls.modules;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PersonAdaptPlanAndCreateFacilities extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Random random = MatsimRandom.getRandom();
	private final ActivityFacilities activityFacilities;
	private final QuadTree<ActivityFacilityImpl> facs = new QuadTree<ActivityFacilityImpl>(-900000,-900000,2700000,2700000);
	private int id;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAdaptPlanAndCreateFacilities(ActivityFacilities activityFacilities) {
		super();
		this.activityFacilities = activityFacilities;
		if (!activityFacilities.getFacilities().isEmpty()) { throw new RuntimeException("given facilities container is not empty!"); }
		this.id = 100000000;
		random.nextDouble();
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		OpeningTime ot = new OpeningTimeImpl(0,24*3600);
		if (person.getPlans().size() != 1) { throw new RuntimeException("Each person must have one plan."); }
		Plan plan = person.getPlans().get(0);
		//Desires desires = ((PersonImpl) person).createDesires("");
		double time = 0.0;
		for (PlanElement e : plan.getPlanElements()) {
			if (e instanceof Activity) {
				ActivityImpl a = (ActivityImpl)e;
				
				// set times
				a.setStartTime(time);
				if (a.getEndTime() != Time.UNDEFINED_TIME) {
					a.setMaximumDuration(a.getEndTime()-a.getStartTime());
					time = a.getEndTime();
				}
				else if (a.getMaximumDuration() != Time.UNDEFINED_TIME) {
					a.setEndTime(a.getStartTime()+a.getMaximumDuration());
					time = a.getEndTime();
				}
				
				// redefine act types
				//if (a.getType().startsWith("h")) {
				//	a.setType("home");
				//	desires.putActivityDuration(a.getType(),16*3600);
				//}
				//else if (a.getType().startsWith("s")) {
				//	a.setType("shop");
				//	desires.putActivityDuration(a.getType(),8*3600);
				//}
				//else if (a.getType().startsWith("l")) {
				//	a.setType("leisure");
				//	desires.putActivityDuration(a.getType(),8*3600);
				//}
				//else if (a.getType().equals("tta")) {
				//	desires.putActivityDuration(a.getType(),16*3600);
				//}
				//else if (a.getType().startsWith("w")) {
				//	if (random.nextDouble() < 0.66) { a.setType("work_sector3"); }
				//	else { a.setType("work_sector2"); }
				//	desires.putActivityDuration(a.getType(),8*3600);
				//}
				//else { throw new RuntimeException("act type="+a.getType()+" not known!"); }
				if ( true ) throw new RuntimeException( "desires are gone and the behavior here can be reproduced with the classic config PlanCalcScore" );
				
				// reset coordinates
				a.getCoord().setXY((int)a.getCoord().getX(),(int)a.getCoord().getY());
				
				// create CB facilities (located outside of Switzerland)
				if (a.getType().equals("tta") || a.getType().equals("home")) {
					int x = (int)a.getCoord().getX();
					int y = (int)a.getCoord().getY();
					Coord c = new Coord((double) x, (double) y);
					ActivityFacilityImpl af = facs.getClosest(x, y);
					if (af == null) {
						af = ((ActivityFacilitiesImpl) activityFacilities).createAndAddFacility(Id.create(id, ActivityFacility.class),c);
						id++;
						ActivityOptionImpl ao = af.createAndAddActivityOption(a.getType());
						ao.setCapacity(1.0);
						if (a.getType().equals("tta")){ ao.addOpeningTime(ot); }
						facs.put(af.getCoord().getX(),af.getCoord().getY(),af);
					}
					else if (af.getCoord().equals(c)) {
						ActivityOptionImpl ao = (ActivityOptionImpl) af.getActivityOptions().get(a.getType());
						if (ao == null) {
							ao = af.createAndAddActivityOption(a.getType());
							ao.setCapacity(1.0);
							if (a.getType().equals("tta")){ ao.addOpeningTime(ot); }
						}
					}
					else {
						af = ((ActivityFacilitiesImpl) activityFacilities).createAndAddFacility(Id.create(id, ActivityFacility.class),c);
						id++;
						ActivityOptionImpl ao = af.createAndAddActivityOption(a.getType());
						ao.setCapacity(1.0);
						if (a.getType().equals("tta")){ ao.addOpeningTime(ot); }
						facs.put(af.getCoord().getX(),af.getCoord().getY(),af);
					}
				}
			}
		}
	}
}
