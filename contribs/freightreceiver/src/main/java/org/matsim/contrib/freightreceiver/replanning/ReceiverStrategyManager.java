/**
 * ********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 * *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 * LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 * *
 * *********************************************************************** *
 * *
 * This program is free software; you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation; either version 2 of the License, or     *
 * (at your option) any later version.                                   *
 * See also COPYING, LICENSE and WARRANTY file                           *
 * *
 * ***********************************************************************
 */

package org.matsim.contrib.freightreceiver.replanning;

import org.matsim.freight.carriers.controler.CarrierStrategyManager;
import org.matsim.contrib.freightreceiver.Receiver;
import org.matsim.contrib.freightreceiver.ReceiverPlan;
import org.matsim.core.replanning.GenericStrategyManager;

/**
 * See {@link CarrierStrategyManager}.
 */
public interface ReceiverStrategyManager extends GenericStrategyManager<ReceiverPlan, Receiver> {
}
