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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.util.List;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * Created by amit on 05/12/2016.
 */


public class PatnaUnwantedConnectorsRemover {

    public static void main(String[] args) {
        Scenario scenario = null;
        new PatnaUnwantedConnectorsRemover().processNetworkForRemoval(scenario.getNetwork());
    }

    void processNetworkForRemoval (final Network network){
        List<Link> links2remove = network.getLinks().values().stream().filter(
                link -> link.getId().toString().startsWith(PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX) && link.getFreespeed()==0.01
        ).collect(Collectors.toList());
        links2remove.stream().forEach(link -> network.removeLink(link.getId()));
    }
}
