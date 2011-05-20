package playground.michalm.vrp.demand;

import pl.poznan.put.vrp.dynamic.data.model.*;


// CAUTION!
// vehicles (QVehicleImpl) are created in QSim only for those agents
// which are instances of PersonDriverAgent
public interface VRPCustomerAgent
// extends ExperimentalBasicWithindayAgent//?
// implements PersonDriverAgent//?
{
    Customer getCustomer();
}
