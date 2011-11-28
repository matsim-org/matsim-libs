package playground.michalm.vrp.sim;

import java.util.*;

import javax.swing.*;

import org.matsim.ptproject.qsim.interfaces.*;
import org.matsim.vis.otfvis.opengl.queries.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.optimizer.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import playground.michalm.visualization.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.supply.*;


public class VRPSimEngine
    implements MobsimEngine
{
    private VRPData vrpData;

    private Netsim netsim;

    private VRPOptimizerFactory optimizerFactory;
    private TaxiVRPOptimizer optimizer;

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
        vrpData.setTime(0);
        // Reset schedules
        for (Vehicle v : vrpData.getVehicles()) {
            v.resetSchedule();
        }

        // remove all existing requests
        vrpData.getRequests().clear();

        optimizer = (TaxiVRPOptimizer)optimizerFactory.create(vrpData);

        optimize(0);// "0" should not be hard-coded

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


    public void optimize(double now)
    {
        optimizer.optimize();
        System.err.println("Optimization @simTime=" + vrpData.getTime());

        notifyAgents();
        notifyOptimizerListeners(new OptimizerEvent((int)now, vrpData));
    }


    public void updateScheduleBeforeNextTask(Vehicle vrpVehicle, double now)
    {
        optimizer.updateSchedule(vrpVehicle);
    }


    public void taxiRequestSubmitted(Request request, double now)
    {
        optimize(now);
    }


    /**
     * This method is called just after a queue-simulation step
     */
    @Override
    public void doSimStep(double time)
    {
        // this happens at the end of QSim.doSimStep() therefore "time+1"
        // this value will be used throughout the next QSim.doSimStep()
        vrpData.setTime((int)time + 1);
    }


    @Override
    public void afterSim()
    {
        // analyze requests that have not been served
        // calculate some scorings here
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
