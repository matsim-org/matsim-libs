/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaColdEmissionFactor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions;

/**
 * @author benjamin
 *
 */
class HbefaColdEmissionFactor {
	// yy not sure if it really makes sense to have an object for this.  But at least it should be immutable
	// (set from constructor). Going one step in that direction by introducing a corresponding constructor.
	// kai, jul'18

	private double coldEmissionFactor;

	/*package-private*/ HbefaColdEmissionFactor(double coldEmissionFactor){
		this.coldEmissionFactor = coldEmissionFactor ;
	}
	
	public double getColdEmissionFactor() {
		return coldEmissionFactor;
	}
	
}
