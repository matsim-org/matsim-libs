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

package org.matsim.contrib.locationchoice.utils;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

public class PlanUtils {
	
	public static void copyPlanFieldsToFrom(PlanImpl planTarget, PlanImpl planTemplate) {
		planTarget.setScore(planTemplate.getScore());
		
		int actLegIndex = 0;
		for (PlanElement pe : planTarget.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl actTemplate = ((ActivityImpl)planTemplate.getPlanElements().get(actLegIndex));
				((ActivityImpl) pe).setEndTime(actTemplate.getEndTime());
				((ActivityImpl) pe).setCoord(actTemplate.getCoord());
				((ActivityImpl) pe).setFacilityId(actTemplate.getFacilityId());
				((ActivityImpl) pe).setLinkId(actTemplate.getLinkId());
				((ActivityImpl) pe).setMaximumDuration(actTemplate.getMaximumDuration());
				((ActivityImpl) pe).setStartTime(actTemplate.getStartTime());
				((ActivityImpl) pe).setType(actTemplate.getType());
				
			} else if (pe instanceof LegImpl) {
				LegImpl legTemplate = ((LegImpl)planTemplate.getPlanElements().get(actLegIndex));
				((LegImpl) pe).setArrivalTime(legTemplate.getArrivalTime());
				((LegImpl) pe).setDepartureTime(legTemplate.getArrivalTime());
				((LegImpl) pe).setMode(legTemplate.getMode());
				((LegImpl) pe).setRoute(legTemplate.getRoute());
				((LegImpl) pe).setTravelTime(legTemplate.getTravelTime());
			}
			actLegIndex++;
		}
	}
}
