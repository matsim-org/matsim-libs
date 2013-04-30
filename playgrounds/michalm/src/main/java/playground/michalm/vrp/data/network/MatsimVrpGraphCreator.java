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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import pl.poznan.put.vrp.dynamic.data.network.*;


public class MatsimVrpGraphCreator
{
    public static MatsimVrpGraph create(Network network, ArcFactory arcFactory,
            boolean fixedSizeVrpGraph)
        throws IOException
    {
        MatsimVrpGraph graph;
        if (fixedSizeVrpGraph) {
            int vertexCount = network.getLinks().size();
            graph = new FixedSizeMatsimVrpGraph(vertexCount);
        }
        else {
            graph = new GrowingMatsimVrpGraph(arcFactory);
        }

        // build vertices for all links...
        
        // one can build Arcs with the feature of lazy initialization (even in the case of the fixed
        // size graph)
        MatsimVertexBuilder vertexBuilder = MatsimVertexImpl.createFromLinkIdBuilder(network);

        for (Id id : network.getLinks().keySet()) {
            graph.addVertex(vertexBuilder.setLinkId(id).build());
        }

        if (fixedSizeVrpGraph) {
            ((FixedSizeVrpGraph)graph).initArcs(arcFactory);
        }

        return graph;
    }
}
