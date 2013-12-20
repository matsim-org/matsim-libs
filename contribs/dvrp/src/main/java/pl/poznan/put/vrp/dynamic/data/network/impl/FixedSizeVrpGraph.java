/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package pl.poznan.put.vrp.dynamic.data.network.impl;

import java.lang.reflect.Array;
import java.util.*;

import pl.poznan.put.vrp.dynamic.data.network.*;


public class FixedSizeVrpGraph
    implements VrpGraph
{
    private final int vertexCount;

    private final Vertex[] vertices;
    private final List<Vertex> unmodifiableVertices;

    private Arc[][] arcs;


    public FixedSizeVrpGraph(int vertexCount)
    {
        this.vertexCount = vertexCount;

        vertices = new Vertex[vertexCount];
        unmodifiableVertices = Collections.unmodifiableList(Arrays.asList(vertices));

        arcs = (Arc[][])Array.newInstance(Arc.class, vertexCount, vertexCount);
    }


    public void initArcs(ArcFactory arcFactory)
    {
        for (Vertex a : vertices) {
            Arc[] sPath_A = arcs[a.getId()];

            for (Vertex b : vertices) {
                sPath_A[b.getId()] = arcFactory.createArc(a, b);
            }
        }
    }


    @Override
    public int getVertexCount()
    {
        return vertexCount;
    }


    @Override
    public Vertex getVertex(int id)
    {
        return vertices[id];
    }


    @Override
    public List<Vertex> getVertices()
    {
        return unmodifiableVertices;
    }


    @Override
    public void addVertex(Vertex v)
    {
        int idx = v.getId();

        if (vertices[idx] != null) {
            throw new IllegalStateException();
        }

        vertices[idx] = v;
    }


    @Override
    public Arc getArc(Vertex fromVertex, Vertex toVertex)
    {
        return arcs[fromVertex.getId()][toVertex.getId()];
    }


    public void setArc(Arc arc)
    {
        arcs[arc.getFromVertex().getId()][arc.getToVertex().getId()] = arc;
    }


    @Override
    public Iterator<Arc> arcIterator()
    {
        return new FixedSizeVrpGraphArcIterator();
    }


    private class FixedSizeVrpGraphArcIterator
        implements Iterator<Arc>
    {
        private int nextArcIdx;
        private Arc nextArc;


        public FixedSizeVrpGraphArcIterator()
        {
            nextArcIdx = -1;
            updateNextArc();
        }


        @Override
        public Arc next()
        {
            if (nextArc == null) {
                throw new NoSuchElementException();
            }

            Arc currentArc = nextArc;
            updateNextArc();
            return currentArc;
        }


        private void updateNextArc()
        {
            while (++nextArcIdx < vertexCount * vertexCount) {
                int row = nextArcIdx / vertexCount;
                int column = nextArcIdx % vertexCount;
                nextArc = arcs[row][column];

                if (nextArc != null) {
                    return;
                }
            }

            nextArc = null;
        }


        @Override
        public boolean hasNext()
        {
            return nextArc != null;
        }


        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
