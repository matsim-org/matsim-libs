package playground.michalm.vrp.events;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;

import pl.poznan.put.vrp.dynamic.monitoring.*;


public interface VRPVehicleEvent
    extends Event
{
    Id getVRPVehicleAgentId();


    MonitoringEvent getMonitoringEvent();
}
