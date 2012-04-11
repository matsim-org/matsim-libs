package playground.michalm.vrp.data;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import playground.michalm.vrp.data.network.MatsimVrpGraph;


public class MatsimVrpData
{
    private VrpData vrpData;
    private Scenario scenario;

    private String coordSystem;


    public MatsimVrpData(VrpData vrpData, Scenario scenario)
    {
        this(vrpData, scenario, TransformationFactory.WGS84_UTM33N);
    }


    public MatsimVrpData(VrpData vrpData, Scenario scenario, String coordSystem)
    {
        this.vrpData = vrpData;
        this.scenario = scenario;
        this.coordSystem = coordSystem;
    }


    public VrpData getVrpData()
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


    public MatsimVrpGraph getVrpGraph()
    {
        return (MatsimVrpGraph)vrpData.getVrpGraph();
    }
}
