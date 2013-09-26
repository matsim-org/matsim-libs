package pl.poznan.put.vrp.dynamic.data.model;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public class DepotImpl
    implements Depot
{
    private final int id;
    private final String name;
    private final Vertex vertex;


    public DepotImpl(int id, String name, Vertex vertex)
    {
        this.id = id;
        this.name = name;
        this.vertex = vertex;
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public Vertex getVertex()
    {
        return vertex;
    }


    @Override
    public String toString()
    {
        return "Depot_" + id;
    }
}
