/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * Disposition, handling route and track reservations.
 */
public interface TrainDisposition {

	/**
	 * Method invoked when a train is departing.
	 */
	void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route);

	/**
	 * Request the next segment to be reserved.
	 * @param time current time
	 * @param position position information
	 * @param dist distance in meter the train is requesting
	 */
	DispositionResponse requestNextSegment(double time, TrainPosition position, double dist);


	/**
	 * Inform the resource manager that the train has passed a link that can now be unblocked.
	 */
	void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link);

}
