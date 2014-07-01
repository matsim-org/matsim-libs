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

/**
 * @author johannes
 *
 */
public class DeleteLegs implements ProxyPersonTask {

	private final String mode;
	
	public DeleteLegs(String mode) {
		this.mode = mode;
	}
	
	@Override
	public void apply(ProxyPerson person) {
		ProxyPlan plan = person.getPlan();
		ProxyPlan newPlan = new ProxyPlan();
		
		newPlan.addActivity(plan.getActivities().get(0));
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			ProxyObject leg = plan.getLegs().get(i);
			
			if(mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
				newPlan.addLeg(leg);
				newPlan.addActivity(plan.getActivities().get(i+1));
			}
		}

		
	}

}
