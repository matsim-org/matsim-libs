/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityStateChangeRepeater.java
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
package eu.eunoiaproject.bikesharing.framework.qsim;

import eu.eunoiaproject.bikesharing.framework.events.NewBikeSharingFacilityStateEvent;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * A simple {@link BikeSharingManagerListener}, which generates
 * {@link NewBikeSharingFacilityStateEvent}s each time the state of a bike sharing
 * facility changes.
 * @author thibautd
 */
public class FacilityStateChangeRepeater implements BikeSharingManagerListener {
	private final QSim qSim;

	public FacilityStateChangeRepeater(final QSim qSim) {
		this.qSim = qSim;
	}

	@Override
	public void handleChange(
			final StatefulBikeSharingFacility facilityInNewState) {
		qSim.getEventsManager().processEvent(
			new NewBikeSharingFacilityStateEvent(
				qSim.getSimTimer().getTimeOfDay(),
				facilityInNewState.getId(),
				facilityInNewState.getNumberOfBikes() ) );
	}
}

