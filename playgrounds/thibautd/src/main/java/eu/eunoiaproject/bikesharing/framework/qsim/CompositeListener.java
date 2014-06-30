/* *********************************************************************** *
 * project: org.matsim.*
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

package eu.eunoiaproject.bikesharing.framework.qsim;

import java.util.ArrayList;
import java.util.List;


class CompositeListener implements BikeSharingManagerListener {
	private final List<BikeSharingManagerListener> listeners = new ArrayList<BikeSharingManagerListener>();

	public void addListener( final BikeSharingManagerListener l ) {
		listeners.add( l );
	}

	@Override
	public void handleChange(final StatefulBikeSharingFacility f) {
		for ( BikeSharingManagerListener l : listeners ) l.handleChange( f );
	}

}
