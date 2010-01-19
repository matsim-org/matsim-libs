/* *********************************************************************** *
 * project: org.matsim.*
 * AssignLocations.java
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

package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.locationchoice.utils.QuadTreeRing;

public class AssignLocations {

	//private final static Logger log = Logger.getLogger(AssignLocations.class);

	private QuadTreeRing<ActivityFacilityImpl> actTree = null;
	private final ActivityFacilities facilities;

	public AssignLocations(final QuadTreeRing<ActivityFacilityImpl> actTree, final ActivityFacilities facilities) {
		super();
		this.actTree = actTree;
		this.facilities = facilities;
	}

	//////////////////////////////////////////////////////////////////////

	private final ActivityFacilityImpl getFacilities(double x, double y, double radius) {
		
		Collection<ActivityFacilityImpl> locs = this.actTree.get(x, y, radius);
		if (locs.isEmpty()) {
			if (radius > 200000.0) {
				Gbl.errorMsg("radius>200'000 meters and still no facility found!");
			}
			return this.getFacilities(x, y, 2.0 * radius);
		}
		int r = MatsimRandom.getRandom().nextInt(locs.size());
		Vector<ActivityFacilityImpl> locations = new Vector<ActivityFacilityImpl>();
		locations.addAll(locs);
		return locations.get(r);
	}
	
	
	private final ActivityFacilityImpl getFacilities(CoordImpl coordStart, CoordImpl coordEnd) {		
		double x = (coordStart.getX() + coordEnd.getX()) / 2.0;
		double y = (coordStart.getY() + coordEnd.getY()) / 2.0;		
		double radius = coordStart.calcDistance(coordEnd);		
		return this.getFacilities(x, y, radius);
	}
	

	//////////////////////////////////////////////////////////////////////

	private final void assignLocation(ActivityImpl act, ActivityFacility start, ActivityFacility end) {
		CoordImpl c_start = (CoordImpl)start.getCoord();
		CoordImpl c_end   = (CoordImpl)end.getCoord();

		double dx = c_end.getX() - c_start.getX();
		double dy = c_end.getX() - c_start.getX();
		if ((dx == 0.0) && (dy == 0.0)) {
			// c_start and c_end equal			
			ActivityFacilityImpl facility = this.getFacilities(c_start.getX(), c_start.getY(), 1000.0);
			act.setFacilityId(facility.getId());
			act.setCoord(facility.getCoord());
		}
		else {
			// c_start and c_end different
			ActivityFacilityImpl facility = this.getFacilities(c_start, c_end);
			act.setFacilityId(facility.getId());
			act.setCoord(facility.getCoord());
		}
	}
	

	public void run(Population plans, String type) {	
		Iterator<? extends Person> person_it = plans.getPersons().values().iterator();
		while (person_it.hasNext()) {
			Person person = person_it.next();
		
			if (person.getPlans().size() != 1) {
				Gbl.errorMsg("pid = "+person.getId()+" : There must be exactly one plan."); 
			}
			Plan plan = person.getSelectedPlan();
			this.run(plan, type);
		}
	}

	private void run(Plan plan, String type) {
		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
			ActivityFacility actFacility = this.facilities.getFacilities().get(act.getFacilityId());
			if (actFacility == null && act.getType().equals(type)) {
				// get the prev act with a facility
				ActivityFacility start = null;
				for (int b = i - 2; b >= 0; b = b - 2) {
					ActivityImpl b_act = (ActivityImpl)plan.getPlanElements().get(b);
					if (b_act.getFacilityId() != null) {
						start = this.facilities.getFacilities().get(b_act.getFacilityId()); 
						break; 
					}
				}
				// get the next act with a facility
				ActivityFacility end = null;
				for (int a = i + 2; a < plan.getPlanElements().size(); a = a + 2) {
					ActivityImpl a_act = (ActivityImpl)plan.getPlanElements().get(a);
					if (a_act.getFacilityId() != null) {
						end = this.facilities.getFacilities().get(a_act.getFacilityId()); 
						break;
					}
				}
				if ((start == null) || (end == null)) {
					Gbl.errorMsg("That should not happen!");
				}
				this.assignLocation(act, start, end);
			}
		}
	}
}

