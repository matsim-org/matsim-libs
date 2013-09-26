package pl.poznan.put.vrp.dynamic.data.network;

import java.util.*;


public interface VrpGraph
{
    int getVertexCount();


    Vertex getVertex(int id);


    List<Vertex> getVertices();


    void addVertex(Vertex v);


    Arc getArc(Vertex vertexFrom, Vertex vertexTo);


    /**
     * Iterates only through existing (i.e. non-null) arcs
     */
    Iterator<Arc> arcIterator();
}
