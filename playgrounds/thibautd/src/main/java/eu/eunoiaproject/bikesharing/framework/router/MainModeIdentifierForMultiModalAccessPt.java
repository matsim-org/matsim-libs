/* *********************************************************************** *
 * project: org.matsim.*
 * MainModeIdentifierForMultiModalAccessPt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.framework.router;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

/**
 * @author thibautd
 */
public class MainModeIdentifierForMultiModalAccessPt implements MainModeIdentifier {
	private final MainModeIdentifier delegate;

	public MainModeIdentifierForMultiModalAccessPt(
			final MainModeIdentifier delegate ) {
		this.delegate = delegate;
	}

	@Override
	public String identifyMainMode(final List<? extends PlanElement> tripElements) {
		for ( Activity act : TripStructureUtils.getActivities( tripElements , EmptyStageActivityTypes.INSTANCE ) ) {
			if ( act.getType().equals( TransitMultiModalAccessRoutingModule.DEPARTURE_ACTIVITY_TYPE ) ) {
				return TransportMode.pt;
			}
		}
		return delegate.identifyMainMode( tripElements );
	}

}

