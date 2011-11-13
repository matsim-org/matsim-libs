package playground.michalm.vrp.sim;

import java.util.*;

import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.cvrp.data.*;
import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.monitoring.*;
import pl.poznan.put.vrp.dynamic.optimizer.*;
import pl.poznan.put.vrp.dynamic.simulator.*;
import playground.michalm.vrp.data.*;


public class VRPSimEngine
    implements MobsimEngine
{
    private Netsim netsim;

    private VRPData vrpData;

    private AlgorithmParams algParams;
    private VRPOptimizer optimizer;

    private static final int MAX_TIME_DIFFERENCE = 180; // in seconds
    private boolean supplyChanged;// set to FALSE before each simStep
    private boolean demandChanged;

    // private VRPVehicleAgentMonitoring vrpVehAgentMonitoring;
    private List<OptimizerListener> optimizerListeners = new ArrayList<OptimizerListener>();


    public VRPSimEngine(Netsim netsim, MATSimVRPData data, AlgorithmParams algParams)
    {
        this.netsim = netsim;
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
        // Reset schedules
        for (Vehicle v : vrpData.getVehicles()) {
            v.resetSchedule();
        }

        // remove all existing requests
        vrpData.getRequests().clear();

        optimizer = new VRPOptimizer(algParams);

        optimize();
    }


    private void optimize()
    {
        optimizer.optimize(vrpData);
        System.err.println("Simulation time = " + vrpData.getTime());

        // update: DVRPData -> MATSim ???
        // for each vehicle, starting from the current Request do update....

        // or
        // notify agents (vehAgents and customerAgents) of the results:
        // - for each "requesting" Customer -> accept/reject
        // - for each "modified" Vehicle -> schedule update

    }


    public void taxiRequestSubmitted(Request request)
    {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println(request);
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        demandChanged = true;
    }


    /**
     * This method is called just after a queue-simulation step
     */
    @Override
    public void doSimStep(double time)
    {
        vrpData.setTime((int)time + 1);// optimize for the next time step

        if (demandChanged || supplyChanged) {// reoptimize
            optimize();
            notifyOptimizerListeners(new OptimizerEvent((int)time, vrpData));
        }

        demandChanged = false;
        supplyChanged = false;
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


    private void notifyOptimizerListeners(OptimizerEvent event)
    {
        for (OptimizerListener l : optimizerListeners) {
            l.optimizationPerformed(event);
        }
    }


    public void addListener(OptimizerListener listener)
    {
        optimizerListeners.add(listener);
    }


    public void removeListener(OptimizerListener listener)
    {
        optimizerListeners.remove(listener);
    }

}
