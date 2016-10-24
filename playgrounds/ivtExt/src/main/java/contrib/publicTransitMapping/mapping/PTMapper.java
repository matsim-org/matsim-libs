/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package contrib.publicTransitMapping.mapping;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Interface for a Public Transit Mapping algorithm
 *
 * @author polettif
 */
public interface PTMapper {

	/**
	 * Based on the stop facilities and transit routes the schedule will
	 * be mapped to the given network. Both schedule and
	 * network are modified.
	 */
	void run();

	void setConfig(Config config);

	void setSchedule(TransitSchedule schedule);

	void setNetwork(Network network);

	Config getConfig();

	TransitSchedule getSchedule();

	Network getNetwork();

}
