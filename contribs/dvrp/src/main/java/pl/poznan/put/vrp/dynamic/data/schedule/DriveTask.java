package pl.poznan.put.vrp.dynamic.data.schedule;

import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.online.VehicleTracker;


public interface DriveTask
    extends Task
{
    Arc getArc();


    VehicleTracker getVehicleTracker();


    void setVehicleTracker(VehicleTracker vehicleTracker);
}
