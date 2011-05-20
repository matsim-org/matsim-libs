package playground.michalm.vrp.engine;

import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.vrp.demand.*;
import playground.michalm.vrp.supply.*;


public class VRPSimEngine
    implements MobsimEngine
{
    private Netsim netsim;

    private VRPVehicleAgentFactory vehicleAgentFactory;
    private VRPCustomerAgentFactory customerAgentFactory;

    private VRPData vrpData;

    public VRPSimEngine(Netsim netsim, VRPData vrpData)
    {
        this.netsim = netsim;
        this.vrpData = vrpData;
    }


    @Override
    public Netsim getMobsim()
    {
        return netsim;
    }


    @Override
    public void onPrepareSim()
    {
        // create agents
        // CAUTION!
        // later on, vehicles (QVehicleImpl) are created in QSim only for agents that satisfy:
        // if (agent instanceof PersonDriverAgent)

        if (vehicleAgentFactory != null) {
            for (Vehicle v : vrpData.vehicles) {
                vehicleAgentFactory.createVehicleAgent(v);
            }
        }

//        if (customerAgentFactory != null) {
//            for (Customer c : vrpData.customers) {
//                customerAgentFactory.createCustomerAgent(c);
//            }
//        }

    }


    @Override
    public void doSimStep(double time)
    {
        //should look more/less like simulatedVRP (old class in my VRP project)
        
        //update: MATSim -> DVRPData (based on events?????)?????
        
        //reoptimize????
        
        //update: DVRPData -> MATSim ???
    }


    @Override
    public void afterSim()
    {
        //analyze requests that have not been served
        //
        //
        
        
    }

    
    public void setCustomerAgentFactory(VRPCustomerAgentFactory customerAgentFactory)
    {
        this.customerAgentFactory = customerAgentFactory;
    }


    public void setVehicleAgentFactory(VRPVehicleAgentFactory vehicleAgentFactory)
    {
        this.vehicleAgentFactory = vehicleAgentFactory;
    }
}
