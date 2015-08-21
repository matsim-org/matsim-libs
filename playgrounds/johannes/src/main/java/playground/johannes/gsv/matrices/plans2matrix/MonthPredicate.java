/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.plans2matrix;

import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 * 
 */
public class MonthPredicate implements Predicate {

	private final String month;

	public MonthPredicate(String month) {
		this.month = month;
	}

	@Override
	public boolean test(PlainPerson person, Attributable leg, Attributable prev, Attributable next) {
		if (month == null || month.equalsIgnoreCase(person.getAttribute(MiDKeys.PERSON_MONTH))) {
			return true;
		} else {
			return false;
		}
	}

}
