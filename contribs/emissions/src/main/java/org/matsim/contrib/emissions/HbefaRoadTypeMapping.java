/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by molloyj on 01.12.2017.
 */
public abstract class HbefaRoadTypeMapping {

    public void addHbefaMappings(Network network) {

        network.getLinks().values().parallelStream()
                .forEach(link -> {
                    String hbefaString = determineHbefaType(link);
                    EmissionUtils.setHbefaRoadType(link, hbefaString);
                });
    }

    protected abstract String determineHbefaType(Link link);

}
