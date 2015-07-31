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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 * 
 */
public class CopyDate2PersonTask implements ProxyPersonTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.gsv.synPop.ProxyPersonTask#apply(playground.johannes
	 * .gsv.synPop.PlainPerson)
	 */
	@Override
	public void apply(PlainPerson person) {
		if (person.getPlans().size() > 0) {
			Episode plan = person.getPlans().get(0);

			person.setAttribute("day", plan.getAttribute("day"));
			person.setAttribute("month", plan.getAttribute("month"));
		}
	}

}
