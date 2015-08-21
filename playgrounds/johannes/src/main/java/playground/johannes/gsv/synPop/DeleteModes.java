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

import playground.johannes.synpop.data.*;
import playground.johannes.synpop.source.mid2008.processing.PersonTask;

/**
 * @author johannes
 *
 */
public class DeleteModes implements PersonTask {

	private final String mode;
	
	public DeleteModes(String mode) {
		this.mode = mode;
	}
	
	@Override
	public void apply(Person person1) {
		PlainPerson person = (PlainPerson)person1;
		boolean found = false;

		Episode plan = person.getPlan();
		for(Attributable leg : plan.getLegs()) {
			if(mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
				found = true;
				break;
			}
		}

		if(!found)
			person.setAttribute(CommonKeys.DELETE, "true");
	}

}
