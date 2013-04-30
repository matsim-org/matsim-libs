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

import java.util.*;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.network.*;


/**
 * It consists of ShortestPathsArcs with ShortestPath of any type (Sparse, Full or other)
 * 
 * In general, use GrowingMatsimVrpGraph instead (smaller memory demand).
 * 
 * @author michalm
 */
public class FixedSizeMatsimVrpGraph
    extends FixedSizeVrpGraph
    implements MatsimVrpGraph
{
    private final Map<Id, MatsimVertex> linkIdToVertex;


    public FixedSizeMatsimVrpGraph(int vertexCount)
    {
        super(vertexCount);
        linkIdToVertex = new LinkedHashMap<Id, MatsimVertex>();
    }


    @Override
    public MatsimVertex getVertex(Id linkId)
    {
        return linkIdToVertex.get(linkId);
    }


    @Override
    public void addVertex(Vertex vertex)
    {
        MatsimVertex mVertex = (MatsimVertex)vertex;
        Id linkId = mVertex.getLink().getId();

        if (linkIdToVertex.put(linkId, mVertex) != null) {
            throw new RuntimeException("Duplicated vertex for link=" + linkId);
        }

        super.addVertex(mVertex);
    }

}
