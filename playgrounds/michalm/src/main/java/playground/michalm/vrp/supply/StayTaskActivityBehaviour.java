package playground.michalm.vrp.supply;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


class StayTaskActivityBehaviour
    extends VRPActivityBehaviour
{
    private StayTask stayTask;


    private StayTaskActivityBehaviour(String activityType, StayTask stayTask)
    {
        super(activityType);
        this.stayTask = stayTask;
    }


    @Override
    double getEndTime()
    {
        return stayTask.getEndTime() - 1;
    }

    // void fireMonitoringEvent(METype type)
    // {
    // // MonitoringEvent monitoringEvent = new MonitoringEvent(type, (int)cause.getTime(),
    // // vrpVehAgent.getVrpVehicle(), req);
    // //
    // // VRPVehicleEvent event = new VRPVehicleEventImpl(cause.getTime(), vrpVehAgent, cause,
    // // monitoringEvent);
    //
    // // first notify (internal to MATSim) VRPVehicleEventHandlers, then notify (external to
    // // MATSim) MonitoringListener
    // // eventsManager.processEvent(event);
    // // vrpSimEngine.notifyMonitoringListeners(monitoringEvent);
    // }
    
    
    static StayTaskActivityBehaviour createServeTaskActivityBehaviour(ServeTask serveTask)
    {
        return new StayTaskActivityBehaviour("ServeTask" + serveTask.getRequest().id, serveTask);
    }
    
    
    static StayTaskActivityBehaviour createWaitTaskActivityBehaviour(WaitTask waitTask)
    {
        return new StayTaskActivityBehaviour("WaitTask", waitTask);
    }
}
