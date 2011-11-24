package playground.michalm.vrp.sim;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.interfaces.*;

import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.supply.*;


public class VRPAgentFactory
    implements AgentFactory
{
    private Netsim netsim;
    private MATSimVRPData data;

    public final static String VRP_DRIVER_PREFIX = "vrpDriver_";


    public VRPAgentFactory(MATSimVRPData data)
    {
        this.data = data;
        this.netsim = data.getVrpSimEngine().getMobsim();
    }


    @Override
    public MobsimAgent createMobsimAgentFromPersonAndInsert(Person p)
    {
        if (p instanceof VRPDriverPerson) {
            VRPDriverPerson driverPerson = (VRPDriverPerson)p;

            TaxiAgentLogic taxiAgentLogic = new TaxiAgentLogic(
                    driverPerson.getVrpVehicle(), data.getVrpGraph().getShortestPaths(), netsim);

            // VRPSchedulePlanFactory planFactory = new VRPSchedulePlanFactory(p,
            // driverPerson.getVrpVehicle(), data);

            Id startLinkId = ((MATSimVertex)driverPerson.getVrpVehicle().getDepot().getVertex())
                    .getLink().getId();

            DynAgent dynAgent = new DynAgent(p.getId(), startLinkId, netsim,
                    taxiAgentLogic);

            taxiAgentLogic.setAgent(dynAgent);

            data.getVrpSimEngine().addAgent(taxiAgentLogic);

            return dynAgent;
        }
        // else if ()// other possible agents
        // {}
        else {// default agents (according to DefaultAgentFactory)
            return PersonDriverAgentImpl.createAndInsertPersonDriverAgentImpl(p, netsim);
        }
    }
}
