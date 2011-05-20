package playground.michalm.vrp.demand;

import pl.poznan.put.vrp.dynamic.data.model.*;


public interface VRPCustomerAgentFactory
{
    VRPCustomerAgent createCustomerAgent(Customer vrpCustomer);
}
