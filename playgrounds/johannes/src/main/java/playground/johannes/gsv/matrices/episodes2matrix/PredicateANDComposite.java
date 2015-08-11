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

package playground.johannes.gsv.matrices.episodes2matrix;

import playground.johannes.gsv.matrices.plans2matrix.Predicate;
import playground.johannes.sna.util.Composite;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 *
 */
public class PredicateANDComposite extends Composite<LegPredicate> implements LegPredicate {

	@Override
	public boolean test(Segment leg) {
		for(LegPredicate p : components) {
			if(!p.test(leg)) {
				return false;
			}
		}
		
		return true;
	}

}
