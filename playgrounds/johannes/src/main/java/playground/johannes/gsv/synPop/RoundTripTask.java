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

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes.gsv.synPop.ProxyPlan)
	 */
	@Override
	public void apply(ProxyPlan plan) {
		List<Integer> insertPoints = new ArrayList<Integer>();
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			ProxyLeg leg = plan.getLegs().get(i);
			String val = (String) leg.getAttribute(CommonKeys.LEG_ROUNDTRIP); 
			if(val.equalsIgnoreCase("true")) {
				ProxyActivity act = plan.getActivities().get(i+1);
				String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, type + "_rt");
				
				insertPoints.add(i+2);
//				
			}
		}
		
		int offset = 0;
		for(Integer idx : insertPoints) {
			int i = idx + offset;
			String prevType = (String) plan.getActivities().get(i-2).getAttribute(CommonKeys.ACTIVITY_TYPE);
			
			ProxyActivity act = new ProxyActivity();
			act.setAttribute(CommonKeys.ACTIVITY_TYPE, prevType);
			
			plan.getActivities().add(i, act);
			plan.getLegs().add(i-1, new ProxyLeg());
			
			offset += 1;
		}

	}

}
