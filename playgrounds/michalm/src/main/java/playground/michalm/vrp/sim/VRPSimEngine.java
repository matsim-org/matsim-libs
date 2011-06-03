package playground.michalm.vrp.sim;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.basic.v01.*;
import org.matsim.core.population.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.cvrp.data.*;
import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.monitoring.*;
import pl.poznan.put.vrp.dynamic.optimizer.*;
import pl.poznan.put.vrp.dynamic.simulator.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;


public class VRPSimEngine
    implements MobsimEngine, Monitoring
{
    private Netsim netsim;

    private MATSimVRPData data;
    private VRPData vrpData;

    private AlgorithmParams algParams;
    private VRPOptimizer optimizer;

    private SimulatedCustomerService customerService;// temporary solution

    private static final int MAX_TIME_DIFFERENCE = 180; // in seconds
    private boolean supplyChanged;// set to FALSE before each simStep
    private boolean demandChanged;

    // private VRPVehicleAgentMonitoring vrpVehAgentMonitoring;
    private List<MonitoringListener> monitoringListeners = new ArrayList<MonitoringListener>();


    public VRPSimEngine(Netsim netsim, MATSimVRPData data, AlgorithmParams algParams)
    {
        this.netsim = netsim;
        this.data = data;
        this.algParams = algParams;

        vrpData = data.getVrpData();
    }


    @Override
    public Netsim getMobsim()
    {
        return netsim;
    }


    @Override
    public void onPrepareSim()
    {
        // vrpVehAgentMonitoring = new VRPVehicleAgentMonitoring(netsim.getEventsManager(),
        // data.getIdToVehAgent(), this);

        customerService = new SimulatedCustomerService(vrpData);

        optimizer = new VRPOptimizer(algParams);
        optimize();
    }


    private void optimize()
    {
        optimizer.optimize(vrpData);
        System.err.println("Simulation time = " + vrpData.time);

        // update: DVRPData -> MATSim ???
        // for each vehicle, starting from the current Request do update....

        // or
        // notify agents (vehAgents and customerAgents) of the results:
        // - for each "requesting" Customer -> accept/reject
        // - for each "modified" Vehicle -> schedule update

    }


    private boolean doDemandSimStep(double time)
    {
        // temporary
        return customerService.updateData(vrpData);
    }


    /**
     * This method is called just after a queue-simulation step
     */
    @Override
    public void doSimStep(double time)
    {
        demandChanged = false;
        supplyChanged = false;

        demandChanged = doDemandSimStep(time);// customer side

        vrpData.time = (int)time + 1;// optimize for the next time step

        if (demandChanged || supplyChanged) {// reoptimize
            optimize();
        }
    }


    @Override
    public void afterSim()
    {
        // analyze requests that have not been served
        // calculate some scorings here
    }


    public void timeDifferenceOccurred(Vehicle vrpVeh, int timeDiff)
    {
        if (timeDiff > MAX_TIME_DIFFERENCE) {
            supplyChanged = true;
        }
    }


    void notifyMonitoringListeners(MonitoringEvent event)
    {
        for (MonitoringListener l : monitoringListeners) {
            l.eventOccured(event);
        }
    }


    public void addListener(MonitoringListener listener)
    {
        monitoringListeners.add(listener);
    }


    public void removeListener(MonitoringListener listener)
    {
        monitoringListeners.remove(listener);
    }

}
