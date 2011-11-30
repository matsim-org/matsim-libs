package playground.michalm.vrp.data.network;

import java.util.*;

import org.matsim.api.core.v01.*;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;


public class MATSimVRPGraph
    extends VRPGraph
{
    private final Map<Id, MATSimVertex> linkIdToVertex;
    private ShortestPath[][] shortestPaths;


    public MATSimVRPGraph(int vertexCount)
    {
        super(vertexCount);

        linkIdToVertex = new LinkedHashMap<Id, MATSimVertex>();
    }


    public Map<Id, MATSimVertex> getLinkIdToVertex()
    {
        return linkIdToVertex;
    }


    public MATSimVertex getVertex(Id linkId)
    {
        return linkIdToVertex.get(linkId);
    }


    @Override
    public void addVertex(Vertex vertex)
    {
        MATSimVertex mVertex = (MATSimVertex)vertex;

        Id linkId = mVertex.getLink().getId();

        if (linkIdToVertex.put(linkId, mVertex) != null) {
            throw new RuntimeException("Duplicated vertex for link=" + linkId);
        }

        super.addVertex(mVertex);
    }


    public ShortestPath[][] getShortestPaths()
    {
        return shortestPaths;
    }


    public void setShortestPaths(ShortestPath[][] sPaths)
    {
        this.shortestPaths = sPaths;
    }
}
