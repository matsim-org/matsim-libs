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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class ConstrainedAccessibilityConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "constrainedAccessibility";

	private double capacityScalingFactor = 1;

	public ConstrainedAccessibilityConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter("capacityScalingFactor")
	public double getCapacityScalingFactor() {
		return capacityScalingFactor;
	}

	@StringSetter("capacityScalingFactor")
	public void setCapacityScalingFactor( final double capacityScalingFactor ) {
		if ( capacityScalingFactor <= 0 || capacityScalingFactor > 1 ) {
			throw new IllegalArgumentException( "capacity scaling factor must be between 0 and 1" );
		}

		this.capacityScalingFactor = capacityScalingFactor;
	}
}
