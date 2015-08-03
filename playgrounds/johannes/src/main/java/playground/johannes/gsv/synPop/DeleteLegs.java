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
import playground.johannes.synpop.data.PlainPerson;

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
	public void apply(PlainPerson person) {
		Episode plan = person.getPlan();
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			Element leg = plan.getLegs().get(i);
			
			if(mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
				Element prevAct = plan.getActivities().get(i);
				for(int k = i+1; k < plan.getActivities().size(); k++) {
					Element nextAct = plan.getActivities().get(k);
					
				}
			}
		}

		
	}

}
