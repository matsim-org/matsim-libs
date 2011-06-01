package playground.michalm.vrp.events;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.events.*;

import pl.poznan.put.vrp.dynamic.monitoring.*;
import playground.michalm.vrp.supply.*;


public class VRPVehicleEventImpl
    extends EventImpl
    implements VRPVehicleEvent
{
    public static final String EVENT_TYPE = "VRP_vehicle";

    public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";

    private final VRPVehicleAgent vehicleAgent;
    private final MonitoringEvent monitoringEvent;
    private final PersonEvent cause;


    public VRPVehicleEventImpl(double time, VRPVehicleAgent vrpVehicleAgent, PersonEvent cause,
            MonitoringEvent monitoringEvent)
    {
        super(time);
        this.vehicleAgent = vrpVehicleAgent;
        this.monitoringEvent = monitoringEvent;
        this.cause = cause;
    }


    @Override
    public Id getVehicleAgentId()
    {
        return vehicleAgent.getId();
    }


    @Override
    public VRPVehicleAgent getVehicleAgent()
    {
        return vehicleAgent;
    }


    @Override
    public MonitoringEvent getMonitoringEvent()
    {
        return monitoringEvent;
    }


    @Override
    public String getEventType()
    {
        return EVENT_TYPE;
    }


    @Override
    public PersonEvent getCause()
    {
        return cause;
    }


    @Override
    public Map<String, String> getAttributes()
    {
        Map<String, String> attr = super.getAttributes();

        attr.put(ATTRIBUTE_VEHICLE_ID, vehicleAgent.getId().toString());

        return attr;
    }
}
