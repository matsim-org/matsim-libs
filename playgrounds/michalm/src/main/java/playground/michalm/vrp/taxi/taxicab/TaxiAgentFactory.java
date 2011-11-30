package playground.michalm.vrp.taxi.taxicab;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.agents.*;

import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.driver.*;
import playground.michalm.vrp.taxi.*;


public class TaxiAgentFactory
    implements AgentFactory
{
    private MATSimVRPData data;
    private VRPSimEngine vrpSimEngine;


    public TaxiAgentFactory(MATSimVRPData data, VRPSimEngine vrpSimEngine)
    {
        this.data = data;
        this.vrpSimEngine = vrpSimEngine;
    }


    @Override
    public MobsimAgent createMobsimAgentFromPersonAndInsert(Person p)
    {
        if (p instanceof VRPDriverPerson) {
            VRPDriverPerson driverPerson = (VRPDriverPerson)p;

            TaxiAgentLogic taxiAgentLogic = new TaxiAgentLogic(driverPerson.getVrpVehicle(), data
                    .getVrpGraph().getShortestPaths(), vrpSimEngine);

            // VRPSchedulePlanFactory planFactory = new VRPSchedulePlanFactory(p,
            // driverPerson.getVrpVehicle(), data);

            Id startLinkId = ((MATSimVertex)driverPerson.getVrpVehicle().getDepot().getVertex())
                    .getLink().getId();

            DynAgent dynAgent = new DynAgent(p.getId(), startLinkId, vrpSimEngine.getMobsim(),
                    taxiAgentLogic);

            data.getVrpSimEngine().addAgentLogic(taxiAgentLogic);

            return dynAgent;
        }
        else {// default agents (according to DefaultAgentFactory)
            return PersonDriverAgentImpl.createAndInsertPersonDriverAgentImpl(p,
                    vrpSimEngine.getMobsim());
        }
    }
}
