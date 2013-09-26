package pl.poznan.put.vrp.dynamic.data.network;

public abstract class AbstractArc
    implements Arc
{
    protected final Vertex fromVertex;
    protected final Vertex toVertex;


    public AbstractArc(Vertex fromVertex, Vertex toVertex)
    {
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
    }


    @Override
    public Vertex getFromVertex()
    {
        return fromVertex;
    }


    @Override
    public Vertex getToVertex()
    {
        return toVertex;
    }
}
