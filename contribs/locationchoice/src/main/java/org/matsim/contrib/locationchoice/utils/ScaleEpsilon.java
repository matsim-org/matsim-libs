/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.utils;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Set;

 public class ScaleEpsilon {
	
	private TObjectDoubleMap<String> epsilonFactors = new TObjectDoubleHashMap<>();
	private final boolean useSimpleTypes = false; // demand v1: e.g., s0.5, ... s24.0 = s
	
	public double getEpsilonFactor(String actType) {
		if (this.useSimpleTypes) actType = actType.substring(0, 1);
		return this.epsilonFactors.get(actType);
	}
	
	public void setEpsilonFactor(String actType, double factor) {
		this.epsilonFactors.put(actType, factor);
	}
	
//	public void setUseSimpleTypes(boolean useSimpleTypes) {
//		this.useSimpleTypes = useSimpleTypes;
//	}
	
	public boolean isFlexibleType(String actType) {
		if (this.useSimpleTypes) actType = actType.substring(0, 1);
		return this.epsilonFactors.containsKey(actType);
	}
	
//	public int getNumberOfFlexibleTypes() {
//		return this.epsilonFactors.size();
//	}
	
	public Set<String> getFlexibleTypes() {
		return this.epsilonFactors.keySet();
	}
}
