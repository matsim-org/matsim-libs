package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.NetworkImpl;

import pl.poznan.put.vrp.dynamic.data.network.VertexBuilder;


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


    public static MATSimVertexBuilder createFromLinkIdBuilder(Network network)
    {
        return new MATSimVertexFromLinkIdBuilder(network);
    }


    public static VertexBuilder createFromXYBuilder(Scenario scenario)
    {
        return new MATSimVertexFromXYBuilder(scenario);
    }


    private static class MATSimVertexFromLinkIdBuilder
        implements MATSimVertexBuilder
    {
        private static int ID = -1;

        private Network network;

        private Id linkId;


        private MATSimVertexFromLinkIdBuilder(Network network)
        {
            this.network = network;
        }


        @Override
        public MATSimVertexBuilder setLinkId(Id linkId)
        {
            this.linkId = linkId;
            return this;
        }


        @Override
        public MATSimVertex build()
        {
            ID++;
            Link link = network.getLinks().get(linkId);

            return new MATSimVertexImpl(ID, linkId.toString(), link.getCoord(), link);
        }
    }


    private static class MATSimVertexFromXYBuilder
        implements VertexBuilder
    {
        private static int ID = -1;

        private Scenario scenario;
        private NetworkImpl network;

        private String name;
        private double x;
        private double y;


        private MATSimVertexFromXYBuilder(Scenario scenario)
        {
            this.scenario = scenario;
            network = (NetworkImpl)scenario.getNetwork();
        }


        @Override
        public VertexBuilder setName(String name)
        {
            this.name = name;
            return this;
        }


        @Override
        public VertexBuilder setX(double x)
        {
            this.x = x;
            return this;
        }


        @Override
        public VertexBuilder setY(double y)
        {
            this.y = y;
            return this;
        }


        @Override
        public MATSimVertex build()
        {
            ID++;

            Coord coord = scenario.createCoord(x, y);
            Link link = network.getNearestLink(coord);

            if (name == null) {
                return new MATSimVertexImpl(ID++, Integer.toString(ID), coord, link);
            }

            return new MATSimVertexImpl(ID, name, coord, link);
        }
    }
}
