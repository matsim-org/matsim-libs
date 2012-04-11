package playground.michalm.vrp.taxi;

import java.util.*;

import javax.swing.SwingUtilities;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;

import pl.poznan.put.vrp.dynamic.data.VRPData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.optimizer.VRPOptimizerFactory;
import pl.poznan.put.vrp.dynamic.optimizer.listener.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import playground.michalm.vrp.data.MATSimVRPData;
import playground.michalm.vrp.data.jdbc.JDBCWriter;
import playground.michalm.vrp.otfvis.VRPOTFClientLive;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentLogic;


public class TaxiSimEngine
    implements MobsimEngine
{
    private MATSimVRPData matsimVrpData;
    private VRPData vrpData;

    private Netsim netsim;

    private VRPOptimizerFactory optimizerFactory;
    private TaxiOptimizer optimizer;
    private TaxiEvaluator taxiEvaluator = new TaxiEvaluator();

    private List<TaxiAgentLogic> agentLogics = new ArrayList<TaxiAgentLogic>();
    private List<OptimizerListener> optimizerListeners = new ArrayList<OptimizerListener>();

    /**
     * yyyyyy This should not be public. An easy fix would be to put vrp.taxi.taxicab and vrp.taxi
     * into the same package and reduce visibility to package level. Alternatively, the internal
     * interface can be passed to the taxicabs somehow (during initialization?). kai, dec'11
     */
    public InternalInterface internalInterface = null;


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
    }


    public TaxiSimEngine(Netsim netsim, MATSimVRPData data, VRPOptimizerFactory optimizerFactory)
    {
        this.netsim = netsim;
        this.optimizerFactory = optimizerFactory;

        matsimVrpData = data;
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

        optimizer = (TaxiOptimizer)optimizerFactory.create(vrpData);

        optimize(0);// "0" should not be hard-coded

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                if (VRPOTFClientLive.queryControl != null) {
                    for (Vehicle v : vrpData.getVehicles()) {
                        QueryAgentPlan query = new QueryAgentPlan();
                        query.setId(v.getName());
                        VRPOTFClientLive.queryControl.createQuery(query);
                    }
                }
            }
        });
    }


    public void optimize(double now)
    {
        optimizer.optimize();
        System.err.println("Optimization @simTime=" + vrpData.getTime());

        notifyAgents();
        notifyOptimizerListeners(new OptimizerEvent((int)now, vrpData,
                taxiEvaluator.evaluateVRP(vrpData)));
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
        vrpData.setTime((int)time + 1); // this can be moved to Before/AfterSimStepListener

        if (time == -3600) {
            System.err.println("************************ SQL &&&&&&&&&&&&&&&&&&&&&");
            JDBCWriter writer = new JDBCWriter(matsimVrpData);
            writer.simulationInitialized();
            writer.fillWithTaskForTesting();
            writer.close();
        }
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
            a.schedulePossiblyChanged();
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
