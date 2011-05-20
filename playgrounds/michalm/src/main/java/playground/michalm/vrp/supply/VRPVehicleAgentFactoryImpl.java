package playground.michalm.vrp.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.basic.v01.*;
import org.matsim.core.population.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;


public class VRPVehicleAgentFactoryImpl
    implements VRPVehicleAgentFactory
{
    private Netsim simulation;


    public VRPVehicleAgentFactoryImpl(Netsim simulation, Map<Id, Vehicle> vrpVehicles)
    {
        this.simulation = simulation;
    }


    @Override
    public VRPVehicleAgent createVehicleAgent(Vehicle vrpVehicle)
    {
        // create dummy person
        PersonImpl driver = new PersonImpl(new IdImpl("vrpDriver_" + vrpVehicle.id));
        VRPVehicleAgent agent = new VRPVehicleAgent(driver, this.simulation, vrpVehicle);
        return agent;
    }
}
