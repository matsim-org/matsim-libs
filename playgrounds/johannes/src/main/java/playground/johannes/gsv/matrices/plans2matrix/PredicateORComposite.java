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

package playground.johannes.gsv.matrices.plans2matrix;

import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.util.Composite;

/**
 * @author johannes
 *
 */
public class PredicateORComposite extends Composite<Predicate> implements Predicate {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.matrices.Predicate#test(playground.johannes.synpop.data.PlainElement, playground.johannes.synpop.data.PlainElement, playground.johannes.synpop.data.PlainElement)
	 */
	@Override
	public boolean test(ProxyPerson person, Element leg, Element prev, Element next) {
		for(Predicate p : components) {
			if(p.test(person, leg, prev, next)) {
				return true;
			}
		}
		return false;
	}

}
