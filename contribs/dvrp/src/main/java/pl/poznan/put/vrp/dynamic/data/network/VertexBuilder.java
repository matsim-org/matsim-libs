package pl.poznan.put.vrp.dynamic.data.network;

public interface VertexBuilder
{
    VertexBuilder setName(String name);


    VertexBuilder setX(double x);


    VertexBuilder setY(double y);


    Vertex build();
}
