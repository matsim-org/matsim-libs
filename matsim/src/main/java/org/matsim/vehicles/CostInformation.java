/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

public final class CostInformation implements Attributable{
	// maybe at least these subtypes should be immutable?  kai, aug'19
	// No, the decision is to rather have the typical matsim data model where this is an object-oriented database in memory, and everything is settable.
	// kai, sep'19

	private Double fixed;
	private Double perMeter;
	private Double perSecond;
	private Attributes attributes = new Attributes() ;

	/* package-private */ CostInformation() {
	}
	public Double getFixedCosts() {
		return fixed;
	}
	public Double getCostsPerMeter() {
		return perMeter;
	}
	public Double getCostsPerSecond() {
		return perSecond;
	}
	public Attributes getAttributes() {
		return attributes;
	}
	public CostInformation setFixedCost( Double fixed ){
		this.fixed = fixed;
		return this ;
	}
	public CostInformation setCostsPerMeter( Double perMeter ){
		this.perMeter = perMeter;
		return this ;
	}
	public CostInformation setCostsPerSecond( Double perSecond ){
		this.perSecond = perSecond;
		return this ;
	}
}
