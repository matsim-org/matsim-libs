package pl.poznan.put.vrp.dynamic.data.network;

public interface ArcFactory
{
    Arc createArc(Vertex fromVertex, Vertex toVertex);
}
