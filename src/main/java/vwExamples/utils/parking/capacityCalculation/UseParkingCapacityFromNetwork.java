/* *********************************************************************** *
 * project: org.matsim.*
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

package vwExamples.utils.parking.capacityCalculation;

import org.matsim.api.core.v01.network.Link;

public class UseParkingCapacityFromNetwork implements LinkParkingCapacityCalculator {

    public static final String CAP_ATT_NAME = "parkingCapacity";

    @Override
    public double getLinkCapacity(Link link) {
        Double d = (Double) link.getAttributes().getAttribute(CAP_ATT_NAME);

        if (d == null) {
            throw new NullPointerException("parking capacity not set for link " + link.getId());
        }

        return d;
    }

}
