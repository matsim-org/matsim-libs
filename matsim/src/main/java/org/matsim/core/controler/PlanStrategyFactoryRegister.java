/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.replanning.PlanStrategyFactory;

public class PlanStrategyFactoryRegister {

	private Map<String, PlanStrategyFactory> factoryMap = new HashMap<String, PlanStrategyFactory>();

	public PlanStrategyFactory getInstance(String strategyType) {
		if (!factoryMap.containsKey(strategyType)) {
			throw new IllegalArgumentException("Plan strategy " + strategyType
					+ " doesn't exist.");
		}
		return factoryMap.get(strategyType);
	}

	public void register(String string, PlanStrategyFactory strategyFactory) {
		if (string.contains(".")) {
			throw new IllegalArgumentException("Plan strategy names with a '.' are reserved for direct class loading.");
		}
		factoryMap.put(string, strategyFactory);
	}

}
