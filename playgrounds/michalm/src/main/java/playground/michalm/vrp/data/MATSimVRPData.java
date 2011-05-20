package playground.michalm.vrp.data;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.transformations.*;

import pl.poznan.put.vrp.dynamic.data.*;
import playground.michalm.vrp.data.network.*;


public class MATSimVRPData
{
    private VRPData vrpData;
    private Scenario scenario;

    private ShortestPath[][] shortestPaths;

    private String coordSystem;


    public MATSimVRPData(VRPData vrpData, Scenario scenario)
    {
        this(vrpData, scenario, TransformationFactory.WGS84_UTM33N);
    }


    public MATSimVRPData(VRPData vrpData, Scenario scenario, String coordSystem)
    {
        this.vrpData = vrpData;
        this.scenario = scenario;
        this.coordSystem = coordSystem;
    }


    public VRPData getVrpData()
    {
        return vrpData;
    }


    public Scenario getScenario()
    {
        return scenario;
    }


    // TODO This should go into DVRPGraph
    public ShortestPath[][] getShortestPaths()
    {
        return shortestPaths;
    }


    public void setShortestPaths(ShortestPath[][] sPaths)
    {
        this.shortestPaths = sPaths;
    }


    public String getCoordSystem()
    {
        return coordSystem;
    }
}
