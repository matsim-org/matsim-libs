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

package playground.agarwalamit.utils;

import java.util.List;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Created by amit on 24/11/2016.
 */


public class NetworkUtils {

    public static void removeIsolatedNodes(final Network network ){
        List<Node> nodes2remove = network.getNodes()
                                         .values()
                                         .stream()
                                         .filter(n -> n.getInLinks().isEmpty() && n.getOutLinks().isEmpty())
                                         .collect(Collectors.toList());

        for(Node n : nodes2remove) {network.removeNode(n.getId());}
    }
}
