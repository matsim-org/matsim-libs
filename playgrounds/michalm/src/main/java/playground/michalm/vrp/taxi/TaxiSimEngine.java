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

import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer;
import playground.michalm.vrp.data.MatsimVrpData;


public class TaxiSimEngine
    implements MobsimEngine, MobsimBeforeSimStepListener
{
    private final VrpData vrpData;
    private final MobsimTimer simTimer;

    private final TaxiOptimizer optimizer;

    private final List<TaxiAgentLogic> agentLogics = new ArrayList<TaxiAgentLogic>();

    private InternalInterface internalInterface;


    public TaxiSimEngine(Netsim netsim, MatsimVrpData data, TaxiOptimizer optimizer)
    {
        this.simTimer = netsim.getSimTimer();
        this.optimizer = optimizer;
        this.vrpData = data.getVrpData();
        netsim.addQueueSimulationListeners(this);
    }


    /*package*/TaxiOptimizer getOptimizer()
    {
        return optimizer;
    }


    /*package*/InternalInterface getInternalInterface()
    {
        return internalInterface;
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
    }


    @Override
    public void onPrepareSim()
    {
        // important: in some cases one may need to decrease simTimer.time
        int time = (int)simTimer.getTimeOfDay();
        vrpData.setTime(time);

        optimizer.init();
        notifyAgentLogics();
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        vrpData.setTime((int)simTimer.getTimeOfDay());
    }


    public void taxiRequestSubmitted(Request request, double now)
    {
        optimizer.taxiRequestSubmitted(request);
        notifyAgentLogics();
    }


    public void nextTask(Vehicle vrpVehicle, int time)
    {
        optimizer.nextTask(vrpVehicle);
        notifyAgentLogics();
    }


    /**
     * This method is called just after a queue-simulation step
     */
    @Override
    public void doSimStep(double time)
    {}


    @Override
    public void afterSim()
    {}


    private void notifyAgentLogics()
    {
        for (TaxiAgentLogic a : agentLogics) {
            a.schedulePossiblyChanged();
        }
    }


    public void addAgentLogic(TaxiAgentLogic agentLogic)
    {
        agentLogics.add(agentLogic);
    }


    public void removeAgentLogic(TaxiAgentLogic agentLogic)
    {
        agentLogics.remove(agentLogic);
    }
}
