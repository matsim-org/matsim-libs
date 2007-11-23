/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficNetI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.interfaces.networks.trafficNet;

import org.matsim.interfaces.networks.basicNet.BasicNetI;

/**
 * A network representation from a traffic engineering point of view.
 */
public interface TrafficNetI extends BasicNetI {

    /**
     * Builds this network's internal structure. Requires that the all nodes'
     * and links' adjacencies have been properly registered among each other, as
     * it is ensured by a call to <code>BasicNetworkI.connect()</code>.
     */
    public void build();

}