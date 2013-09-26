package pl.poznan.put.vrp.dynamic.data.online;

import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


public interface VehicleTracker
{
    DriveTask getDriveTask();


    Vertex getLastPosition();


    int getLastPositionTime();


    Vertex predictNextPosition(int currentTime);


    int predictNextPositionTime(int currentTime);


    int predictEndTime(int currentTime);


    int getInitialEndTime();


    /**
     * Delay relative to the initial driveTask.getEndTime() (the end time my be updated
     * periodically, thus driveTask.getEndTime() may return different results over time)
     */
    int calculateCurrentDelay(int currentTime);


    void addListener(VehicleTrackerListener listener);


    void removeListener(VehicleTrackerListener listener);
}
