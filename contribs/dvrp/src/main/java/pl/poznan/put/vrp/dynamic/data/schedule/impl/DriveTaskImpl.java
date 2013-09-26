package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.online.*;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


public class DriveTaskImpl
    extends AbstractTask
    implements DriveTask
{
    private final Arc arc;
    private VehicleTracker vehicleTracker;


    public DriveTaskImpl(int beginTime, int endTime, Arc arc)
    {
        super(beginTime, endTime);
        this.arc = arc;
        vehicleTracker = new OfflineVehicleTracker(this);//by default; can be changed later
    }


    @Override
    public Arc getArc()
    {
        return arc;
    };


    @Override
    public TaskType getType()
    {
        return TaskType.DRIVE;
    }


    @Override
    public VehicleTracker getVehicleTracker()
    {
        return vehicleTracker;
    }


    @Override
    public void setVehicleTracker(VehicleTracker vehicleTracker)
    {
        this.vehicleTracker = vehicleTracker;
    }


    @Override
    public String toString()
    {
        return "D(@" + arc.getFromVertex().getId() + "->@" + arc.getToVertex().getId() + ")"
                + commonToString();
    }
}