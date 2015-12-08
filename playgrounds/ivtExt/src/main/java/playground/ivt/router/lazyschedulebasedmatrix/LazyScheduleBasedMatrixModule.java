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
package playground.ivt.router.lazyschedulebasedmatrix;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;

/**
 * Adds a wrapper around the PT router, to store a matrix of the best routes.
 * It does not seem to help a lot, at least in the accessibility computation runs
 * (where there are probably to seldom several agents at the same origin)
 * @author thibautd
 */
public class LazyScheduleBasedMatrixModule extends AbstractModule {
	@Override
	public void install() {
		bind( LazyScheduleBasedMatrixRoutingModule.Cache.class );
		addRoutingModuleBinding( TransportMode.pt ).to( LazyScheduleBasedMatrixRoutingModule.class );
	}
}
