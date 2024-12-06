/* *********************************************************************** *
 * project: org.matsim.*
 * ReorderPolicy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.freight.receiver;

import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * A general interface to use for different {@link Receiver} reordering policies.
 *
 * @author jwjoubert
 */
public interface ReorderPolicy extends Attributable {

	String getPolicyName();

	/**
	 * This method assumes that the stock on hand, and the reordering policy's
	 * quantities are expressed in the same unit-of-measure.
	 */
	double calculateOrderQuantity(double onHand);

}
