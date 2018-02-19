/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule.api;

import org.matsim.api.core.v01.Id;

/**
 * Container class to manage minimal transfer times between two {@link TransitStopFacility}.
 * Transfer times are *not* bidirectional and *not* transitive. This means:
 * <ul>
 *     <li>A transfer time defined from stop A to stop B does not define a transfer time from stop B to stop A.</li>
 *     <li>A transfer time defined from stop A to stop B, and another defined from stop B to stop C, does not define a transfer time from stop A to stop C.</li>
 * </ul>
 * Both cases, the reverse transfer from stop B to stop A, and the indirect transfer from stop A to stop C via stop B,
 * must explicitly be set in order to be returned in {@link #get(Id, Id)}.
 *
 * @author mrieser / SBB
 */
public interface MinimalTransferTimes {

	/**
	 * Sets the minimal transfer time in seconds needed to transfer from <code>fromStop</code> to <code>toStop</code>.
	 * @param fromStop
	 * @param toStop
	 * @param seconds the minimal transfer time, in seconds
	 * @return the minimal transfer time previously assigned between the two stops, <code>Double.NaN</code> if none was specified.
	 */
	double set(Id<TransitStopFacility> fromStop, Id<TransitStopFacility> toStop, double seconds);

	/**
	 * @param fromStop
	 * @param toStop
	 * @return the minimal transfer time between the two stops if defined, <code>Double.NaN</code> if none is set.
	 */
	double get(Id<TransitStopFacility> fromStop, Id<TransitStopFacility> toStop);

	/**
	 * @param fromStop
	 * @param toStop
	 * @param defaultSeconds
	 * @return the minimal transfer time between the two stops if defined, <code>defaultSeconds</code> if none is set.
	 */
	double get(Id<TransitStopFacility> fromStop, Id<TransitStopFacility> toStop, double defaultSeconds);

	/**
	 * Removes the minimal transfer time between the two stops if there was one set.
	 * @param fromStop
	 * @param toStop
	 * @return the previously set minimal transfer time, or <code>Double.NaN</code> if none was set.
	 */
	double remove(Id<TransitStopFacility> fromStop, Id<TransitStopFacility> toStop);

	/**
	 * @return an iterator to iterate over all minimal transfer times set.
	 */
	MinimalTransferTimesIterator iterator();

	interface MinimalTransferTimesIterator {
		boolean hasNext();

		/**
		 * advances the iterator to the next element
		 */
		void next();

		/**
		 * @return the Id of the fromStop of the current iterator element.
		 */
		Id<TransitStopFacility> getFromStopId();

		/**
		 * @return the Id of the toStop of the current iterator element.
		 */
		Id<TransitStopFacility> getToStopId();

		/**
		 * @return the minimal transfer time in seconds of the current iterator element.
		 */
		double getSeconds();
	}
}
