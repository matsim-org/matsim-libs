package playground.michalm.vrp.model;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPVehicleAgentFactory
    implements AgentFactory
{
    private Netsim simulation;
    private Map<Id, Vehicle> vrpVehicles;// TaxiDriver.id->Vehicle


    public VRPVehicleAgentFactory(Netsim simulation, Map<Id, Vehicle> vrpVehicles)
    {
        this.simulation = simulation;
    }


    @Override
    public VRPVehicleAgent createPersonAgent(Person p)
    {
        Vehicle vrpVehicle = vrpVehicles.get(p.getId());

        VRPVehicleAgent agent = new VRPVehicleAgent(p, this.simulation, vrpVehicle);

        return null;
    }

}
