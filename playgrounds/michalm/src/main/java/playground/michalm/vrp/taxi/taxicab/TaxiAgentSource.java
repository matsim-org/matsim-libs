package playground.michalm.vrp.taxi.taxicab;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.*;
import org.matsim.vehicles.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.driver.*;
import playground.michalm.vrp.taxi.*;


public class TaxiAgentSource
    implements AgentSource
{
    private MATSimVRPData data;
    private VRPSimEngine vrpSimEngine;
    private boolean isAgentWithPlan;


    public TaxiAgentSource(MATSimVRPData data, VRPSimEngine vrpSimEngine)
    {
        this(data, vrpSimEngine, false);
    }


    public TaxiAgentSource(MATSimVRPData data, VRPSimEngine vrpSimEngine, boolean isAgentWithPlan)
    {
        this.data = data;
        this.vrpSimEngine = vrpSimEngine;
        this.isAgentWithPlan = isAgentWithPlan;
    }


    @Override
    public List<MobsimAgent> insertAgentsIntoMobsim()
    {
        QSim qSim = (QSim)vrpSimEngine.getMobsim();
        List<Vehicle> vehicles = data.getVrpData().getVehicles();
        List<MobsimAgent> taxiAgents = new ArrayList<MobsimAgent>(vehicles.size());

        for (Vehicle vrpVeh : vehicles) {
            TaxiAgentLogic taxiAgentLogic = new TaxiAgentLogic(vrpVeh, data.getVrpGraph()
                    .getShortestPaths(), vrpSimEngine);
            vrpSimEngine.addAgentLogic(taxiAgentLogic);

            Id id = data.getScenario().createId(vrpVeh.getName());
            Id startLinkId = ((MATSimVertex)vrpVeh.getDepot().getVertex()).getLink().getId();

            DynAgent taxiAgent = new DynAgent(id, startLinkId, vrpSimEngine.getMobsim(),
                    taxiAgentLogic);

            if (isAgentWithPlan) {
                taxiAgents.add(new DynAgentWithPlan(taxiAgent, new VRPSchedulePlanFactory(vrpVeh,
                        data)));
            }
            else {
                taxiAgents.add(taxiAgent);
            }

            qSim.createAndParkVehicleOnLink(
                    VehicleUtils.getFactory().createVehicle(taxiAgent.getId(),
                            VehicleUtils.getDefaultVehicleType()), taxiAgent.getCurrentLinkId());
        }

        return taxiAgents;
    }
}
