package playground.michalm.vrp.taxi.taxicab;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.*;
import org.matsim.vehicles.*;

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
    public List<MobsimAgent> insertAgentsIntoMobsim()
    {
        QSim qSim = (QSim)taxiSimEngine.getMobsim();
        List<Vehicle> vehicles = data.getVrpData().getVehicles();
        List<MobsimAgent> taxiAgents = new ArrayList<MobsimAgent>(vehicles.size());

        for (Vehicle vrpVeh : vehicles) {
            TaxiAgentLogic taxiAgentLogic = new TaxiAgentLogic(vrpVeh, data.getVrpGraph()
                    .getShortestPaths(), taxiSimEngine);
            taxiSimEngine.addAgentLogic(taxiAgentLogic);

            Id id = data.getScenario().createId(vrpVeh.getName());
            Id startLinkId = ((MATSimVertex)vrpVeh.getDepot().getVertex()).getLink().getId();

            DynAgent taxiAgent = new DynAgent(id, startLinkId, taxiSimEngine.getMobsim(),
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
