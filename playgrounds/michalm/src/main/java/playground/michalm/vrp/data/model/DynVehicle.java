package playground.michalm.vrp.data.model;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.dynamic.DynAgentLogic;


public class DynVehicle
    extends VehicleImpl
{
    private DynAgentLogic agentLogic;


    public DynVehicle(int id, String name, Depot depot, int capacity, double cost, int t0, int t1,
            int timeLimit)
    {
        super(id, name, depot, capacity, cost, t0, t1, timeLimit);
    }


    public DynAgentLogic getAgentLogic()
    {
        return agentLogic;
    }


    public void setAgentLogic(DynAgentLogic agentLogic)
    {
        this.agentLogic = agentLogic;
    }
}
