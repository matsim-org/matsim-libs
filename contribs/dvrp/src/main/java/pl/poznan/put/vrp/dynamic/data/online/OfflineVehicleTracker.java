package pl.poznan.put.vrp.dynamic.data.online;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


public class OfflineVehicleTracker
    implements VehicleTracker
{
    private final DriveTask driveTask;
    private final int initialEndTime;


    public OfflineVehicleTracker(DriveTask driveTask)
    {
        this.driveTask = driveTask;
        this.initialEndTime = driveTask.getEndTime();
    }


    @Override
    public DriveTask getDriveTask()
    {
        return driveTask;
    }


    @Override
    public Vertex getLastPosition()
    {
        return driveTask.getArc().getFromVertex();
    }


    @Override
    public int getLastPositionTime()
    {
        return driveTask.getBeginTime();
    }


    @Override
    public Vertex predictNextPosition(int currentTime)
    {
        return driveTask.getArc().getToVertex();
    }


    @Override
    public int predictNextPositionTime(int currentTime)
    {
        return predictEndTime(currentTime);
    }


    @Override
    public int calculateCurrentDelay(int currentTime)
    {
        return Math.max(0, currentTime - initialEndTime);
    }


    @Override
    public int predictEndTime(int currentTime)
    {
        return Math.max(initialEndTime, currentTime);
    }
    
    
    @Override
    public int getInitialEndTime()
    {
        return initialEndTime;
    }


    //Listener-functionality is missing since OfflineVehicleTracker does not follow vehicles
    //and hence does not notifyNextPosition
    @Override
    public void addListener(VehicleTrackerListener listener)
    {}


    @Override
    public void removeListener(VehicleTrackerListener listener)
    {}
}
