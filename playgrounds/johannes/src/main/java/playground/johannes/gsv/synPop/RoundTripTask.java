/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class RoundTripTask implements ProxyPlanTask {

	private static final String ROUNDTRIP_SUFFIX = ".roundTrip";
	
	@Override
	public void apply(ProxyPlan plan) {
		List<Integer> insertPoints = new ArrayList<Integer>();
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			ProxyLeg leg = plan.getLegs().get(i);
			Boolean val = (Boolean) leg.getAttribute(CommonKeys.LEG_ROUNDTRIP); 
			if(val != null && val == true) {
				ProxyActivity act = plan.getActivities().get(i+1);
				String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, type + ROUNDTRIP_SUFFIX);
				
				insertPoints.add(i+2);
			}
		}
		
		int offset = 0;
		for(Integer idx : insertPoints) {
			int i = idx + offset;
			
			ProxyLeg toLeg = plan.getLegs().get(i - 2);
			int toLegStart = (Integer) toLeg.getAttribute(CommonKeys.LEG_START_TIME);
			int toLegEnd = (Integer) toLeg.getAttribute(CommonKeys.LEG_END_TIME);
			int dur = toLegEnd - toLegStart;
			Double dist = (Double) toLeg.getAttribute(CommonKeys.LEG_DISTANCE);
			/*
			 * half the leg duration and distance
			 */
			toLeg.setAttribute(CommonKeys.LEG_END_TIME, toLegStart + dur/2 - 1);
			if(dist != null) toLeg.setAttribute(CommonKeys.LEG_DISTANCE, dist/2.0);
			/*
			 * insert a dummy activity with duration 1 s.
			 */
			ProxyActivity act = new ProxyActivity();
			String prevType = (String) plan.getActivities().get(i-2).getAttribute(CommonKeys.ACTIVITY_TYPE);
			act.setAttribute(CommonKeys.ACTIVITY_TYPE, prevType);
			plan.getActivities().add(i, act);
			/*
			 * insert a return leg with half the duration and distance
			 */
			ProxyLeg fromLeg = new ProxyLeg();
			fromLeg.setAttribute(CommonKeys.LEG_START_TIME, toLegStart + dur/2);
			fromLeg.setAttribute(CommonKeys.LEG_END_TIME, toLegEnd);
			fromLeg.setAttribute(CommonKeys.LEG_DISTANCE, toLeg.getAttribute(CommonKeys.LEG_DISTANCE));
			plan.getLegs().add(i-1, fromLeg);
			
			offset += 1;
		}

	}

}
