package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.*;

import pl.poznan.put.vrp.dynamic.data.network.*;


public class MATSimVertexImpl
    implements MATSimVertex
{
    private final int id;
    private final String name;
    private final Coord coord;
    private final Link link;


    protected MATSimVertexImpl(int id, String name, Coord coord, Link link)
    {
        this.id = id;
        this.name = name;
        this.coord = coord;
        this.link = link;
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
    public double getX()
    {
        return coord.getX();
    }


    @Override
    public double getY()
    {
        return coord.getY();
    }


    @Override
    public Coord getCoord()
    {
        return coord;
    }


    @Override
    public Link getLink()
    {
        return link;
    }


    @Override
    public String toString()
    {
        return "Vertex_" + id;
    }


    public static class Builder
        implements VertexBuilder
    {
        private Scenario scenario;
        private NetworkImpl network;

        private int id;
        private String name;
        private double x;
        private double y;


        public Builder(Scenario scenario)
        {
            this.scenario = scenario;
            network = (NetworkImpl)scenario.getNetwork();
        }


        @Override
        public void setId(int id)
        {
            this.id = id;
        }


        @Override
        public void setName(String name)
        {
            this.name = name;
        }


        @Override
        public void setX(double x)
        {
            this.x = x;
        }


        @Override
        public void setY(double y)
        {
            this.y = y;
        }


        @Override
        public Vertex build()
        {
            Coord coord = scenario.createCoord(x, y);
            Link link = network.getNearestLink(coord);

            if (name == null) {
                name = Integer.toString(id);
            }

            return new MATSimVertexImpl(id, name, coord, link);
        }
    }
}
