/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.facilities.ActivityFacility;


public class AssignInitalShopFacilities {

	private QuadTreeRing<ActivityFacility> actTree = null;

	public AssignInitalShopFacilities(final QuadTreeRing<ActivityFacility> actTree) {
		this.actTree = actTree;
	}

	public void run(PlanImpl plan, String type) {

		ActivityImpl actPre = (ActivityImpl)plan.getPlanElements().get(0);

		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);

			if (act.getType().equals(type)) {
				ActivityImpl actAnchor = actPre;
				if (actPre.getType().startsWith("leisure")) {
					int j = i + 2;
					while (j < plan.getPlanElements().size() && actAnchor.getType().startsWith("leisure")) {
						actAnchor = (ActivityImpl)plan.getPlanElements().get(j);
						j +=2;
					}
				}
				ActivityFacility facility = this.actTree.get(actAnchor.getCoord().getX(), actAnchor.getCoord().getY());
				act.setFacilityId(facility.getId());
				act.setCoord(facility.getCoord());
			}
			actPre = act;
		}
	}
}
