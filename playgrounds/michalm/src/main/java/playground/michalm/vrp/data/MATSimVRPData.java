package playground.michalm.vrp.data;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.transformations.*;

import pl.poznan.put.vrp.dynamic.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.taxi.*;


public class MATSimVRPData
{
    private VRPData vrpData;
    private Scenario scenario;

    private VRPSimEngine vrpSimEngine;

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


    public String getCoordSystem()
    {
        return coordSystem;
    }


    public void setVrpSimEngine(VRPSimEngine vrpSimEngine)
    {
        this.vrpSimEngine = vrpSimEngine;
    }


    public VRPSimEngine getVrpSimEngine()
    {
        return vrpSimEngine;
    }
    
    
    public MATSimVRPGraph getVrpGraph()
    {
        return (MATSimVRPGraph)vrpData.getVrpGraph();
    }
}
