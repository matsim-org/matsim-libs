/* *********************************************************************** *
 * project: org.matsim.*
 * ArrivalTimeModAdaptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.planmod;

import java.util.Map;

import playground.johannes.coopsim.mental.choice.ArrivalTimeSelector;
import playground.johannes.coopsim.mental.choice.PlanIndexSelector;

/**
 * @author illenberger
 *
 */
public class ArrivalTimeModAdaptor implements Choice2ModAdaptor {

	private final ArrivalTimeMod mod;
	
	public ArrivalTimeModAdaptor() {
		mod = new ArrivalTimeMod();
	}
	
	@Override
	public PlanModifier convert(Map<String, Object> choices) {
		int index = (Integer) choices.get(PlanIndexSelector.KEY);
		double time = (Double) choices.get(ArrivalTimeSelector.KEY);
		
		mod.setPlanIndex(index);
		mod.setDesiredArrivalTime(time);
		
		return mod;
	}

}
