package playground.michalm.vrp.taxi.taxicab;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vehicles.VehicleUtils;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.dynamic.DynAgent;
import playground.michalm.dynamic.DynAgentWithPlan;
import playground.michalm.vrp.data.MATSimVRPData;
import playground.michalm.vrp.data.network.MATSimVertex;
import playground.michalm.vrp.driver.VRPSchedulePlanFactory;
import playground.michalm.vrp.taxi.TaxiSimEngine;


public class TaxiAgentSource
    implements AgentSource
{
    private MATSimVRPData data;
    private TaxiSimEngine taxiSimEngine;
    private boolean isAgentWithPlan;


    public TaxiAgentSource(MATSimVRPData data, TaxiSimEngine vrpSimEngine)
    {
        this(data, vrpSimEngine, false);
    }


    public TaxiAgentSource(MATSimVRPData data, TaxiSimEngine taxiSimEngine, boolean isAgentWithPlan)
    {
        this.data = data;
        this.taxiSimEngine = taxiSimEngine;
        this.isAgentWithPlan = isAgentWithPlan;
    }


    @Override
    public void insertAgentsIntoMobsim()
    {
        QSim qSim = (QSim)taxiSimEngine.getMobsim();
        List<Vehicle> vehicles = data.getVrpData().getVehicles();

        for (Vehicle vrpVeh : vehicles) {
            TaxiAgentLogic taxiAgentLogic = new TaxiAgentLogic(vrpVeh, data.getVrpGraph()
                    .getShortestPaths(), taxiSimEngine);
            taxiSimEngine.addAgentLogic(taxiAgentLogic);

            Id id = data.getScenario().createId(vrpVeh.getName());
            Id startLinkId = ((MATSimVertex)vrpVeh.getDepot().getVertex()).getLink().getId();

            DynAgent taxiAgent = new DynAgent(id, startLinkId, taxiSimEngine.getMobsim(),
                    taxiAgentLogic);

            if (isAgentWithPlan) {
                qSim.insertAgentIntoMobsim(new DynAgentWithPlan(taxiAgent, new VRPSchedulePlanFactory(vrpVeh,
                        data)));
            }
            else {
                qSim.insertAgentIntoMobsim(taxiAgent);
            }

            qSim.createAndParkVehicleOnLink(
                    VehicleUtils.getFactory().createVehicle(taxiAgent.getId(),
                            VehicleUtils.getDefaultVehicleType()), taxiAgent.getCurrentLinkId());
        }
    }
}
