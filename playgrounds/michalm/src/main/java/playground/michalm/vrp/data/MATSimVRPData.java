package playground.michalm.vrp.data;

import playground.michalm.vrp.data.network.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.*;
import org.matsim.core.utils.geometry.transformations.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.network.Node;


public class MATSimVRPData
{
    public VRPData vrpData;
    public Scenario scenario;

    public Coord[] nodeToCoords;
    public Link[] nodeToLinks;
    
    public ShortestPath[][] shortestPaths;

    public String coordSystem = TransformationFactory.WGS84_UTM33N;


    public MATSimVRPData(VRPData vrpData, Scenario scenario)
    {
        this.vrpData = vrpData;
        this.scenario = scenario;

        initNodeMaps();
    }


    private void initNodeMaps()
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        Node[] nodes = vrpData.nodes;

        nodeToCoords = new Coord[nodes.length];
        nodeToLinks = new Link[nodes.length];
        
        for (int i = 0; i < nodes.length; i++) {
            Coord coord = scenario.createCoord(nodes[i].x, nodes[i].y);

            nodeToCoords[i] = coord;
            nodeToLinks[i] = network.getNearestLink(coord);
        }
    }
}
