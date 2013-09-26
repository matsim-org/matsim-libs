package pl.poznan.put.vrp.dynamic.data.network;

import pl.poznan.put.vrp.dynamic.data.model.Localizable;


public class VertexTimePair
    implements Localizable
{
    private final Vertex vertex;
    private final int time;


    public VertexTimePair(Vertex vertex, int time)
    {
        this.vertex = vertex;
        this.time = time;
    }


    @Override
    public Vertex getVertex()
    {
        return vertex;
    }


    public int getTime()
    {
        return time;
    }
}
