/* *********************************************************************** *
 * project: org.matsim.*
 * CHCustomizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;

/**
 * Assigns edge weights to a {@link CHGraph} (the "customization" step of CCH/CH).
 *
 * <ul>
 *   <li><b>Real edges</b>: weight = {@link TravelDisutility#getLinkMinimumTravelDisutility(Link)}</li>
 *   <li><b>Shortcuts</b>: weight = weight[lowerEdge1] + weight[lowerEdge2]</li>
 * </ul>
 *
 * @author Steffen Axer
 */
public class CHCustomizer {

    public void customize(CHGraph chGraph, TravelDisutility td) {
        SpeedyGraph baseGraph = chGraph.getBaseGraph();
        int      edgeCount = chGraph.totalEdgeCount;
        double[] weights   = chGraph.edgeWeights;
        int[]    origLink  = chGraph.edgeOrigLink;
        int[]    lower1    = chGraph.edgeLower1;
        int[]    lower2    = chGraph.edgeLower2;
        int[]    order     = chGraph.customizeOrder;

        for (int i = 0; i < edgeCount; i++) {
            int e = order[i];
            if (origLink[e] >= 0) {
                Link link = baseGraph.getLink(origLink[e]);
                weights[e] = td.getLinkMinimumTravelDisutility(link);
            } else {
                weights[e] = weights[lower1[e]] + weights[lower2[e]];
            }
        }

        // Propagate weights into colocated CSR weight arrays
        CHTTFCustomizer.propagateWeightsToCSR(chGraph);
    }
}
