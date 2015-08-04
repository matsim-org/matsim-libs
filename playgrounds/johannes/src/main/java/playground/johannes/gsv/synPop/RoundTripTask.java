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

import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainSegment;
import playground.johannes.synpop.data.Segment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class RoundTripTask implements ProxyPlanTask {

	private static final String ROUNDTRIP_SUFFIX = "";//":roundTrip";
	
	@Override
	public void apply(Episode plan) {
		List<Integer> insertPoints = new ArrayList<Integer>();
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			Element leg = plan.getLegs().get(i);
			Boolean val = Boolean.parseBoolean(leg.getAttribute(CommonKeys.LEG_ROUNDTRIP)); 
			if(val != null && val == true) {
				Element act = plan.getActivities().get(i+1);
				String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, type + ROUNDTRIP_SUFFIX);
				
				insertPoints.add(i+2);
			}
		}
		
		int offset = 0;
		for(Integer idx : insertPoints) {
			int i = idx + offset;
			
			Element toLeg = plan.getLegs().get(i - 2);
			int toLegStart = Integer.parseInt(toLeg.getAttribute(CommonKeys.LEG_START_TIME));
			int toLegEnd = Integer.parseInt(toLeg.getAttribute(CommonKeys.LEG_END_TIME));
			int dur = toLegEnd - toLegStart;
			
			/*
			 * half the leg duration and distance
			 */
			toLeg.setAttribute(CommonKeys.LEG_END_TIME, String.valueOf(toLegStart + dur/2 - 1));
			String distStr = toLeg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
			if(distStr != null) {
				double dist = Double.parseDouble(distStr);
				toLeg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, String.valueOf(dist/2.0));
			}
			/*
			 * insert a dummy activity with duration 1 s.
			 */
			Segment act = new PlainSegment();
			String prevType = (String) plan.getActivities().get(i-2).getAttribute(CommonKeys.ACTIVITY_TYPE);
			act.setAttribute(CommonKeys.ACTIVITY_TYPE, prevType);
			plan.getActivities().add(i, act);
			/*
			 * insert a return leg with half the duration and distance
			 */
			Segment fromLeg = new PlainSegment();
			fromLeg.setAttribute(CommonKeys.LEG_START_TIME, String.valueOf(toLegStart + dur/2));
			fromLeg.setAttribute(CommonKeys.LEG_END_TIME, String.valueOf(toLegEnd));
			fromLeg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, toLeg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE));
			fromLeg.setAttribute(CommonKeys.LEG_MODE, toLeg.getAttribute(CommonKeys.LEG_MODE));
			
			Element nextAct = plan.getActivities().get(i);
			fromLeg.setAttribute(CommonKeys.LEG_PURPOSE, nextAct.getAttribute(CommonKeys.ACTIVITY_TYPE));
			plan.getLegs().add(i-1, fromLeg);
			
			offset += 1;
		}

	}

}
