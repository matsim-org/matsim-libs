package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.NetworkImpl;

import pl.poznan.put.vrp.dynamic.data.network.VertexBuilder;


public class MatsimVertexImpl
    implements MatsimVertex
{
    private final int id;
    private final String name;
    private final Coord coord;
    private final Link link;


    protected MatsimVertexImpl(int id, String name, Coord coord, Link link)
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


    public static MatsimVertexBuilder createFromLinkIdBuilder(Network network)
    {
        return new MatsimVertexFromLinkIdBuilder(network);
    }


    public static VertexBuilder createFromXYBuilder(Scenario scenario)
    {
        return new MatsimVertexFromXYBuilder(scenario);
    }


    private static class MatsimVertexFromLinkIdBuilder
        implements MatsimVertexBuilder
    {
        private static int ID = -1;

        private Network network;

        private Id linkId;


        private MatsimVertexFromLinkIdBuilder(Network network)
        {
            this.network = network;
        }


        @Override
        public MatsimVertexBuilder setLinkId(Id linkId)
        {
            this.linkId = linkId;
            return this;
        }


        @Override
        public MatsimVertex build()
        {
            ID++;
            Link link = network.getLinks().get(linkId);

            return new MatsimVertexImpl(ID, linkId.toString(), link.getCoord(), link);
        }
    }


    private static class MatsimVertexFromXYBuilder
        implements VertexBuilder
    {
        private static int ID = -1;

        private Scenario scenario;
        private NetworkImpl network;

        private String name;
        private double x;
        private double y;


        private MatsimVertexFromXYBuilder(Scenario scenario)
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
        public MatsimVertex build()
        {
            ID++;

            Coord coord = scenario.createCoord(x, y);
            Link link = network.getNearestLink(coord);

            if (name == null) {
                return new MatsimVertexImpl(ID++, Integer.toString(ID), coord, link);
            }

            return new MatsimVertexImpl(ID, name, coord, link);
        }
    }
}
