package playground.michalm.vrp.events;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;

import pl.poznan.put.vrp.dynamic.monitoring.*;
import playground.michalm.vrp.supply.*;


public interface VRPVehicleEvent
    extends Event
{
    enum VRPVehicleEventType
    {
        VEH_DEPARTURED, VEH_ARRIVED;
    };


    Id getVehicleAgentId();


    VRPVehicleAgent getVehicleAgent();


    MonitoringEvent getMonitoringEvent();
    
    
    PersonEvent getCause();
}
