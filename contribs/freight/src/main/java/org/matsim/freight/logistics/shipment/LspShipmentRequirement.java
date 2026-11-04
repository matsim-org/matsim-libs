/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.shipment;

import org.matsim.freight.logistics.LogisticChain;

/**
 * A condition that a {@link org.matsim.freight.logistics.LogisticChain} must satisfy for a given
 * {@link LspShipment} to be handled by it. Implementations are used to decide, for example, whethe
 * a chain's resources can meet the shipment's capacity or delivery requirements.
 */
public interface LspShipmentRequirement {

	/**
	 * @param solution the chain being checked
	 * @return {@code true} if the chain fulfills the requirement, {@code false} otherwise
	 */
	boolean checkRequirement(LogisticChain solution);
}
