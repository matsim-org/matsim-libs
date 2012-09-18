/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.vrp.taxi;

import java.util.*;

import javax.swing.SwingUtilities;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.optimizer.VrpOptimizerFactory;
import pl.poznan.put.vrp.dynamic.optimizer.listener.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.otfvis.VrpOTFClientLive;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentLogic;


public class TaxiSimEngine
    implements MobsimEngine
{
    private final VrpData vrpData;
    private final Netsim netsim;
    private final VrpOptimizerFactory optimizerFactory;

    // ////////// begin: TO BE MOVED TO TAXI_OPTIMIZER?
    private TaxiOptimizer optimizer;
    private final TaxiOptimizationPolicy optimizePolicy;
    private final TaxiEvaluator taxiEvaluator = new TaxiEvaluator();
    private final List<OptimizerListener> optimizerListeners = new ArrayList<OptimizerListener>();
    // ////////// end: TO BE MOVED TO TAXI_OPTIMIZER?

    private final List<TaxiAgentLogic> agentLogics = new ArrayList<TaxiAgentLogic>();

    private InternalInterface internalInterface;


    public TaxiSimEngine(Netsim netsim, MatsimVrpData data, TaxiOptimizerFactory optimizerFactory)
    {
        this.netsim = netsim;
        this.optimizerFactory = optimizerFactory;

        vrpData = data.getVrpData();
        optimizePolicy = optimizerFactory.getOptimizationPolicy();
    }


    // TODO should not be PUBLIC!!!
    // probably all actions on InternalInterface should be carried out via TaxiSimEngine...
    // but for now, let it be public...
    //
    // yyyy yes, definitely should not be public. One compromise is to make it "default", and have
    // everything that
    // needs access inside the same package. kai, sep'12
    //
    // Currently, all objects that need access to InternalInterface and only these objects
    // have access to TaxiSimEngine, therefore I will keep TaxiSimEngine.getInternalInterface()
    // public to avoid passing too many arguments through the code. michalm, sep'12
    public InternalInterface getInternalInterface()
    {
        return internalInterface;
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
    }


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
                if (VrpOTFClientLive.queryControl != null) {
                    for (Vehicle v : vrpData.getVehicles()) {
                        QueryAgentPlan query = new QueryAgentPlan();
                        query.setId(v.getName());
                        VrpOTFClientLive.queryControl.createQuery(query);
                    }
                }
            }
        });
    }


    protected boolean update(Vehicle vrpVehicle)
    {
        return optimizer.updateSchedule(vrpVehicle);
    }


    protected void optimize(double now)
    {
        optimizer.optimize();
        // System.err.println("Optimization @simTime=" + vrpData.getTime());

        notifyAgents();
        notifyOptimizerListeners(new OptimizerEvent((int)now, vrpData,
                taxiEvaluator.evaluateVrp(vrpData)));
    }


    public void updateAndOptimizeBeforeNextTask(Vehicle vrpVehicle, double now)
    {
        boolean scheduleChanged = update(vrpVehicle);

        if (scheduleChanged && optimizePolicy.shouldOptimize(vrpVehicle.getSchedule())) {
            optimize(now);
        }
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
        // this value will be used throughout the next QSim.doSimStep() call
        vrpData.setTime((int)time + 1); // this can be moved to Before/AfterSimStepListener
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
