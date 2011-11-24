package playground.michalm.vrp.sim;

import java.util.*;

import javax.swing.*;

import org.matsim.ptproject.qsim.interfaces.*;
import org.matsim.vis.otfvis.opengl.queries.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.optimizer.*;
import playground.michalm.visualization.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.supply.*;


public class VRPSimEngine
    implements MobsimEngine
{
    private Netsim netsim;

    private VRPData vrpData;

    private VRPOptimizerFactory optimizerFactory;
    private VRPOptimizer optimizer;

    private static final int MAX_TIME_DIFFERENCE = 180; // in seconds
    private boolean demandChanged;

    private List<TaxiAgentLogic> agentLogics = new ArrayList<TaxiAgentLogic>();

    private List<OptimizerListener> optimizerListeners = new ArrayList<OptimizerListener>();


    public VRPSimEngine(Netsim netsim, MATSimVRPData data, VRPOptimizerFactory optimizerFactory)
    {
        this.netsim = netsim;
        this.optimizerFactory = optimizerFactory;

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

        optimizer = optimizerFactory.create();

        optimize(-1);//?? -1 ??

        if (VRPOTFClientLive.queryControl != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    for (Vehicle v : vrpData.getVehicles()) {
                        QueryAgentPlan query = new QueryAgentPlan();
                        query.setId(v.getName());
                        VRPOTFClientLive.queryControl.createQuery(query);
                    }
                }
            });
        }
    }


    private void optimize(double time)
    {
        optimizer.optimize(vrpData);
        System.err.println("Simulation time = " + vrpData.getTime());

        // update: DVRPData -> MATSim ???
        // for each vehicle, starting from the current Request do update....

        // or
        // notify agents (vehAgents and customerAgents) of the results:
        // - for each "requesting" Customer -> accept/reject
        // - for each "modified" Vehicle -> schedule update

        notifyAgents();
        
        notifyOptimizerListeners(new OptimizerEvent((int)time, vrpData));
    }


    public void taxiRequestSubmitted(Request request)
    {
        // System.err.println("Req: " + request + " has been submitted");
        demandChanged = true;
    }


    /**
     * This method is called just after a queue-simulation step
     */
    @Override
    public void doSimStep(double time)
    {
        vrpData.setTime((int)time + 1);// optimize for the next time step

        if (demandChanged) {// reoptimize
            optimize(time);
        }

        demandChanged = false;
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
            // reoptimize!!!
        }
    }
    
    
    private void notifyAgents()
    {
        for (TaxiAgentLogic a : agentLogics) {
            a.scheduleUpdated();
        }
    }


    public void addAgentLogic(TaxiAgentLogic agentLogic)
    {
        agentLogics.add(agentLogic);
    }


    public void removeAgent(TaxiAgentLogic agent)
    {
        agentLogics.remove(agent);
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
