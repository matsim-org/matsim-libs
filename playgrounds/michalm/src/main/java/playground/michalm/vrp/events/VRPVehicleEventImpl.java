package playground.michalm.vrp.events;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.events.*;

import pl.poznan.put.vrp.dynamic.monitoring.*;
import playground.michalm.vrp.supply.*;


public class VRPVehicleEventImpl
    extends EventImpl
    implements VRPVehicleEvent
{
    public static final String EVENT_TYPE = "VRP_vehicle";

    public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
    public static final String ATTRIBUTE_ROUTE_ID = "routeId";

    private MonitoringEvent monitoringEvent;


    public VRPVehicleEventImpl(double time, VRPVehicleAgent vrpVehicleAgent)
    {
        super(time);

    }


    @Override
    public Id getVRPVehicleAgentId()
    {
        // TODO Auto-generated method stub
        return null;
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
    public Map<String, String> getAttributes()
    {
        Map<String, String> attr = super.getAttributes();

        attr.put(ATTRIBUTE_VEHICLE_ID, Integer.toString(monitoringEvent.route.vehicle.id));
        attr.put(ATTRIBUTE_ROUTE_ID, Integer.toString(monitoringEvent.route.id));

        return attr;
    }
}
