package playground.michalm.vrp.supply;

import pl.poznan.put.vrp.dynamic.data.model.*;

public interface VRPVehicleAgentFactory
{
    VRPVehicleAgent createVehicleAgent(Vehicle vrpVehicle);
}
