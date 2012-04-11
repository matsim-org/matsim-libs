package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;


public interface MATSimVRPGraph
    extends VRPGraph
{
    MATSimVertex getVertex(Id linkId);


    ShortestPath getShortestPath(Vertex vertexFrom, Vertex vertexTo);
}
