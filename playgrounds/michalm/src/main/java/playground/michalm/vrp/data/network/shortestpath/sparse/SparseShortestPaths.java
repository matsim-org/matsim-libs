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

package playground.michalm.vrp.data.network.shortestpath.sparse;

import java.lang.reflect.Array;
import java.util.List;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPathCalculator;


public class SparseShortestPaths
{
    public static SparseShortestPathArc[][] findShortestPaths(
            ShortestPathCalculator shortestPathCalculator, TimeDiscretizer timeDiscretizer,
            MatsimVrpGraph graph)
    {
        List<Vertex> vertices = graph.getVertices();
        int n = vertices.size();

        SparseShortestPathArc[][] shortestPaths = (SparseShortestPathArc[][])Array.newInstance(
                SparseShortestPathArc.class, n, n);

        for (Vertex a : vertices) {
            MatsimVertex vA = (MatsimVertex)a;

            SparseShortestPathArc[] sPath_A = shortestPaths[vA.getId()];

            for (Vertex b : vertices) {
                MatsimVertex vB = (MatsimVertex)b;

                SparseShortestPathArc sPath_AB = new SparseShortestPathArc(shortestPathCalculator,
                        timeDiscretizer, vA, vB);

                sPath_A[vB.getId()] = sPath_AB;
            }
        }

        return shortestPaths;
    }
}
