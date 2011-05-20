package playground.michalm.vrp.demand;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class ExternalVRPCustomerAgent
    implements VRPCustomerAgent
{
    private Customer customer;


    public ExternalVRPCustomerAgent(Customer customer)
    {
        this.customer = customer;
    }


    @Override
    public Customer getCustomer()
    {
        return customer;
    }
}
