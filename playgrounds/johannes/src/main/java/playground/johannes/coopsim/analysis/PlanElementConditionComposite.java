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

package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.common.collections.Composite;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author johannes
 *
 */
public class PlanElementConditionComposite<T extends PlanElement> extends Composite<PlanElementCondition<T>> implements PlanElementCondition<T> {

	@Override
	public boolean test(Trajectory t, T element, int idx) {
		for(PlanElementCondition<T> condition : components) {
			if(!condition.test(t, element, idx)) {
				return false;
			}
		}
		
		return true;
	}

}
