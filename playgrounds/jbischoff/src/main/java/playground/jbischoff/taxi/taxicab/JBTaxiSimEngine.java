package playground.jbischoff.taxi.taxicab;
import java.util.*;

import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
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

import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import playground.jbischoff.taxi.optimizer.RankTaxiOptimizer;
import playground.michalm.dynamic.DynAgentLogic;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.taxi.TaxiSimEngine;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentLogic;

public class JBTaxiSimEngine 
	extends TaxiSimEngine

	    implements MobsimEngine
	{
	    private final VrpData vrpData;
	    private final Netsim netsim;
	    private final MobsimTimer simTimer;

	    private RankTaxiOptimizer optimizer;
	    // ////////// begin: TO BE MOVED TO TAXI_OPTIMIZER?
	    private final TaxiOptimizationPolicy optimizationPolicy;
	    // ////////// end: TO BE MOVED TO TAXI_OPTIMIZER?

	    private final List<TaxiAgentLogic> agentLogics = new ArrayList<TaxiAgentLogic>();

	    private InternalInterface internalInterface;


	    public JBTaxiSimEngine(Netsim netsim, MatsimVrpData data, TaxiOptimizer optimizer,
	            TaxiOptimizationPolicy optimizationPolicy, RankTaxiOptimizer rto)
	    {
	    	super(netsim,data,optimizer,optimizationPolicy);
	        this.netsim = netsim;
	        this.simTimer = netsim.getSimTimer();
	        this.optimizer = rto;
	        this.optimizationPolicy = optimizationPolicy;

	        vrpData = data.getVrpData();
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
	        // important: in some cases one may need to decrease simTimer.time
	        vrpData.setTime((int)simTimer.getTimeOfDay());

	        optimize();
	    }


	    private void optimize()
	    {
	        optimizer.optimize();
	        notifyAgents();
	    }


	    public void updateAndOptimizeBeforeNextTask(Vehicle vrpVehicle, double now)
	    {
	        boolean scheduleChanged = optimizer.updateSchedule(vrpVehicle);

	        if (scheduleChanged && optimizationPolicy.shouldOptimize(vrpVehicle.getSchedule())) {
	            optimize();
	        }
	    }


	    public void taxiRequestSubmitted(Request request, double now)
	    {
	        optimize();
	    }


	    /**
	     * This method is called just after a queue-simulation step
	     */
	    @Override
	    public void doSimStep(double time)
	    {
	        // this is executed at the end of QSim.doSimStep() therefore "time+1"
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


	    public void removeAgentLogic(TaxiAgentLogic agentLogic)
	    {
	        agentLogics.remove(agentLogic);
	    }
	}

	

