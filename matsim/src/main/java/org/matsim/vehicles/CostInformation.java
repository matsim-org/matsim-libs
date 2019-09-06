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

import org.matsim.utils.objectattributes.attributable.Attributes;

public final class CostInformation{
	// yyyy maybe at least these subtypes should be immutable?  kai, aug'19

	private Double fixed;
	private Double perMeter;
	private Double perSecond;
	private Attributes attributes = new Attributes() ;

	public CostInformation() {
		// yyyy leaving this public for the time being because otherwise the carrier vehicle type reading logic needs to change completely.  kai, sep'19
	}

	@Deprecated
	public CostInformation( Double fixed, Double perMeter, Double perTimeUnit ) {
		this.fixed = fixed;
		this.perMeter = perMeter;
		this.perSecond = perTimeUnit;
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

	public void setFixedCosts(Double fixed ){
		this.fixed = fixed;
	}

	public void setCostsPerMeter(Double perMeter ){
		this.perMeter = perMeter;
	}

	public void setCostsPerSecond(Double perSecond ){
		this.perSecond = perSecond;
	}

}
