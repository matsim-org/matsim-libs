package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.immediaterequest.IdleVehicleFinder;


public class NOSRankTaxiOptimizer
    extends RankTaxiOptimizer
{
    private final IdleRankVehicleFinder idleVehicleFinder;


    public NOSRankTaxiOptimizer(VrpData data, boolean destinationKnown, boolean straightLineDistance)
    {
        super(data, destinationKnown);
        idleVehicleFinder = new IdleRankVehicleFinder(data, straightLineDistance);
    }


    @Override
    protected VehicleDrive findBestVehicle(Request req, List<Vehicle> vehicles)
    {
        Vehicle veh = idleVehicleFinder.findClosestVehicle(req);

        if (veh == null) {
            return VehicleDrive.NO_VEHICLE_DRIVE_FOUND;
        }

        return super.findBestVehicle(req, Arrays.asList(veh));
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        Schedule schedule = vehicle.getSchedule();

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            return false;
        }

        boolean requestsInQueue = !unplannedRequestQueue.isEmpty();
        boolean vehicleAvailable = schedule.getCurrentTask().getType() == TaskType.WAIT;

        return vehicleAvailable && requestsInQueue;
    }
}
