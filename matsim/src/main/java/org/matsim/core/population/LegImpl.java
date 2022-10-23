/* *********************************************************************** *
 * project: org.matsim.*
 * Leg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.LazyAllocationAttributes;

/* deliberately package */  final class LegImpl  implements Leg {

	private Route route = null;

	private OptionalTime depTime = OptionalTime.undefined();
	private OptionalTime travTime = OptionalTime.undefined();
	private String mode;

	private Attributes attributes = null;

	/* deliberately package */  LegImpl(final String transportMode) {
		this.mode = transportMode;
	}

	@Override
	public final String getMode() {
		return this.mode;
	}

	@Override
	public final void setMode(String transportMode) {
		this.mode = transportMode;
		TripStructureUtils.setRoutingMode( this, null );
//		TripStructureUtils.setRoutingMode( this, null ); // setting routingMode to null leads to exceptions in AttributesXmlWriterDelegate.writeAttributes() : Class<?> clazz = objAttribute.getValue().getClass();
		// (yyyy or maybe "transportMode" instead of "null"?? kai, oct'19)
	}

	@Override
	public final OptionalTime getDepartureTime() {
		return this.depTime;
	}

	@Override
	public final void setDepartureTime(final double depTime) {
		this.depTime = OptionalTime.defined(depTime);
	}

	@Override
	public void setDepartureTimeUndefined() {
		this.depTime = OptionalTime.undefined();
	}

	@Override
	public final OptionalTime getTravelTime() {
		return this.travTime;
	}

	@Override
	public final void setTravelTime(final double travTime) {
		this.travTime = OptionalTime.defined(travTime);
	}

	@Override
	public void setTravelTimeUndefined() {
		this.travTime = OptionalTime.undefined();
	}

	@Override
	public Route getRoute() {
		return this.route;
	}

	@Override
	public final void setRoute(Route route) {
		this.route = route;
	}

	@Override
	public final String toString() {
		return "leg [mode="
				+ this.getMode()
				+ "]"
				+ "[depTime="
				+ Time.writeTime(this.getDepartureTime())
				+ "]"
				+ "[travTime="
				+ Time.writeTime(this.getTravelTime())
				+ "]"
				+ "[arrTime="
				+ (depTime.isDefined() && travTime.isDefined()?
				Time.writeTime(depTime.seconds() + travTime.seconds()) :
				Time.writeTime(OptionalTime.undefined()))
				+ "]"
				+ "[route="
				+ this.route
				+ "]";
	}


	@Override
	public Attributes getAttributes() {
		if (this.attributes != null) {
			return this.attributes;
		}
		return new LazyAllocationAttributes(attributes -> this.attributes = attributes);
	}

	//	private boolean locked;
//
//	public void setLocked() {
//		this.locked = true ;
//	}
//	private void testForLocked() {
//		if ( this.locked ) {
//			throw new RuntimeException("too late to change this") ;
//		}
//	}

}
