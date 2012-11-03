/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.data.network;

import java.io.IOException;

import org.matsim.api.core.v01.*;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;


public class MatsimVrpGraphCreator
{
    public static MatsimVrpGraph create(Scenario scenario, TravelTime ttimeCalc,
            TravelDisutility tcostCalc, TimeDiscretizer timeDiscretizer, boolean fixedSizeVrpGraph)
        throws IOException
    {
        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);
        ShortestPathCalculator shortestPathCalculator = new ShortestPathCalculator(router,
                ttimeCalc, tcostCalc);

        ArcFactory arcFactory = new SparseDiscreteMatsimArc.SparseDiscreteMatsimArcFactory(
                shortestPathCalculator, timeDiscretizer);

        MatsimVrpGraph graph;

        if (fixedSizeVrpGraph) {
            int vertexCount = scenario.getNetwork().getLinks().size();
            graph = new FixedSizeMatsimVrpGraph(vertexCount);
            ((FixedSizeVrpGraph)graph).initArcs(arcFactory);
        }
        else {
            graph = new GrowingMatsimVrpGraph(arcFactory);
        }

        // build vertices for all links...
        MatsimVertexBuilder vertexBuilder = MatsimVertexImpl.createFromLinkIdBuilder(scenario
                .getNetwork());

        for (Id id : scenario.getNetwork().getLinks().keySet()) {
            graph.addVertex(vertexBuilder.setLinkId(id).build());
        }

        return graph;
    }
}
