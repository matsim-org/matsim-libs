/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.contrib.taxi.optimizer;

import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;


/**
 * kNN - k Nearest Neighbours
 */
public class StraightLineKNNFinders
{
    public static StraightLineKNNFinder<VehicleData.Entry, TaxiRequest> createTaxiRequestFinder(
            int k)
    {
        return new StraightLineKNNFinder<>(k, LinkProviders.VEHICLE_ENTRY_TO_LINK,
                LinkProviders.REQUEST_TO_FROM_LINK);
    }


    public static StraightLineKNNFinder<TaxiRequest, VehicleData.Entry> createVehicleDepartureFinder(
            int k)
    {
        return new StraightLineKNNFinder<>(k, LinkProviders.REQUEST_TO_FROM_LINK,
                LinkProviders.VEHICLE_ENTRY_TO_LINK);
    }
}
