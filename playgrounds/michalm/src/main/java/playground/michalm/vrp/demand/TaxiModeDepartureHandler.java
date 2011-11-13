package playground.michalm.vrp.demand;

import org.matsim.api.core.v01.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.customer.*;
import pl.poznan.put.vrp.dynamic.customer.CustomerAction.CAType;
import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.sim.*;


public class TaxiModeDepartureHandler
    implements DepartureHandler
{
    private static final String TAXI_MODE = "taxi";

    private MATSimVRPData data;
    private VRPSimEngine vrpEngine;


    public TaxiModeDepartureHandler(VRPSimEngine vrpEngine, MATSimVRPData data)
    {
        this.vrpEngine = vrpEngine;
        this.data = data;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id linkId)
    {
        if (agent.getMode().equals(TAXI_MODE)) {
            // mobsim.getEventsManager().processEvent(
            // new TaxiRequestEventImpl(now, agent.getId(), linkId, agent.getMode()));

            MATSimVRPGraph vrpGraph = data.getVrpGraph();

            MATSimVertex fromVertex = vrpGraph.getVertex(linkId);

            Id toLinkId = agent.getDestinationLinkId();
            MATSimVertex toVertex = vrpGraph.getVertex(toLinkId);

            // notify the DVRP Optimizer
            // agent -> customerId -> Customer
            Customer customer = new TaxiCustomer(0, fromVertex, agent);// TODO

            int duration = 120; // approx. 120 s for entering the taxi
            int t0 = (int)now;
            int t1 = t0 + 3600; // hardcoded values!
            Request request = new RequestImpl(0, customer, fromVertex, toVertex, 1, 1, duration,
                    t0, t1, false);

            data.getVrpData().getRequests().add(request);
            // call for a taxi -> means notify DVRPOptimizer
            vrpEngine.taxiRequestSubmitted(request);

            CustomerAction ca = new CustomerAction(CAType.REQ_SUBMIT, t0, request);// ??
            // notify listeners about this event?

            vrpEngine.getMobsim().registerAdditionalAgentOnLink(agent);

            return true;
        }
        else {
            return false;
        }
    }

}
