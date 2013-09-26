package pl.poznan.put.vrp.dynamic.chart;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public interface VertexSource
{
    int getCount();


    Vertex getVertex(int item);
}