package pl.poznan.put.vrp.dynamic.data.model;

import pl.poznan.put.vrp.dynamic.data.schedule.Schedule;


/**
 * @author michalm
 */
public interface Vehicle
{
    int getId();


    String getName();


    Depot getDepot();


    int getCapacity();


    double getCost();


    // vehicle's time window [T0, T1) (from T0 inclusive to T1 exclusive)
    int getT0();


    int getT1();


    // max time outside the depot
    int getTimeLimit();


    Schedule getSchedule();


    void resetSchedule();
}
