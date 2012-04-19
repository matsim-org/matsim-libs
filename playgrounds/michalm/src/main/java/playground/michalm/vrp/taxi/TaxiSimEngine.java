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

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.optimizer.VrpOptimizerFactory;
import pl.poznan.put.vrp.dynamic.optimizer.listener.OptimizerEvent;
import pl.poznan.put.vrp.dynamic.optimizer.listener.OptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiEvaluator;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.jdbc.JdbcWriter;
import playground.michalm.vrp.otfvis.VrpOTFClientLive;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentLogic;


public class TaxiSimEngine
    implements MobsimEngine
{
    private VrpData vrpData;

    private Netsim netsim;

    private VrpOptimizerFactory optimizerFactory;
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


    public TaxiSimEngine(Netsim netsim, MatsimVrpData data, VrpOptimizerFactory optimizerFactory)
    {
        this.netsim = netsim;
        this.optimizerFactory = optimizerFactory;

        vrpData = data.getVrpData();
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


    public void optimize(double now)
    {
        optimizer.optimize();
        System.err.println("Optimization @simTime=" + vrpData.getTime());

        notifyAgents();
        notifyOptimizerListeners(new OptimizerEvent((int)now, vrpData,
                taxiEvaluator.evaluateVrp(vrpData)));
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
