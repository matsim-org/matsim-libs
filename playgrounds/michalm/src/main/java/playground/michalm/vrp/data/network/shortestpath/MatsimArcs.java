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

package playground.michalm.vrp.data.network.shortestpath;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.vrp.data.network.MatsimVrpGraph;


public class MatsimArcs
{
    public static ShortestPath getShortestPath(MatsimVrpGraph graph, Vertex vertexFrom,
            Vertex vertexTo, int departTime)
    {
        return ((MatsimArc)graph.getArc(vertexFrom, vertexTo)).getShortestPath(departTime);
    }

}
