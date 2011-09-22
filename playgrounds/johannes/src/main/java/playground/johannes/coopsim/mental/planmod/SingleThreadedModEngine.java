/* *********************************************************************** *
 * project: org.matsim.*
 * SingleThreadedModEngine.java
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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class SingleThreadedModEngine implements PlanModEngine {

	private final Choice2ModAdaptor adaptor;
	
	public SingleThreadedModEngine(Choice2ModAdaptor adaptor) {
		this.adaptor = adaptor;
	}
	
	@Override
	public void run(List<Plan> plans, Map<String, Object> choices) {
		/*
		 * convert choices to plan modifiers 
		 */
		PlanModifier mod = adaptor.convert(choices);
		/*
		 * apply modifications
		 */
		for(Plan plan : plans)
			mod.apply(plan);
	}

}
