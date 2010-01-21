/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.utils.misc.Time;

/*
 * This Class implements a QueueSimulationBeforeSimStepListener.
 * 
 * Each time a ListenerEvent is created it is checked
 * whether a WithinDayReplanning of the Agents Plans should
 * be done and / or is necessary.
 */
public class ReplanningManager implements SimulationBeforeSimStepListener, SimulationInitializedListener{

	protected boolean initialReplanning = false;
	protected boolean actEndReplanning = false;
	protected boolean leaveLinkReplanning = false;

	protected InitialReplanningModule initialReplanningModule;
	protected DuringActivityReplanningModule actEndReplanningModule;
	protected DuringLegReplanningModule leaveLinkReplanningModule;
	
	public ReplanningManager()
	{
	}
	
	/*
	 * Using this Constructor is prefered. If the handed over
	 * Replanning Modules are not null, the replanning will be
	 * activated automatically!
	 */
	public ReplanningManager(InitialReplanningModule initialReplanningModule, DuringActivityReplanningModule actEndReplanningModule, DuringLegReplanningModule leaveLinkReplanningModule)
	{
		if (initialReplanningModule != null)
		{
			this.initialReplanningModule = initialReplanningModule;
			initialReplanning = true;
		}
		else
		{
			initialReplanning = false;
		}
		
		if (actEndReplanningModule != null)
		{
			this.actEndReplanningModule = actEndReplanningModule;
			actEndReplanning = true;
		}
		else
		{
			actEndReplanning = false;
		}
		
		if (leaveLinkReplanningModule != null)
		{
			this.leaveLinkReplanningModule = leaveLinkReplanningModule;
			leaveLinkReplanning = true;
		}
		else
		{
			leaveLinkReplanning = false;
		}
	}
	
	public void doInitialReplanning(boolean value)
	{
		initialReplanning = value;
	}
	
	public boolean isInitialReplanning()
	{
		return initialReplanning;
	}
	
	public void setInitialReplanningModule(InitialReplanningModule module)
	{
		this.initialReplanningModule = module;	
	}
	
	public InitialReplanningModule getInitialReplanningModule()
	{
		return this.initialReplanningModule;
	}
	
	public void doActEndReplanning(boolean value)
	{
		actEndReplanning = value;
	}
	
	public boolean isActEndReplanning()
	{
		return actEndReplanning;
	}
	
	public void setActEndReplanningModule(DuringActivityReplanningModule module)
	{
		this.actEndReplanningModule = module;	
	}
	
	public DuringActivityReplanningModule getActEndReplanningModule()
	{
		return this.actEndReplanningModule;
	}
	
	public void doLeaveLinkReplanning(boolean value)
	{
		leaveLinkReplanning = value;
	}
	
	public boolean isLeaveLinkReplanning()
	{
		return leaveLinkReplanning;
	}
	
	public void setLeaveLinkReplanningModule(DuringLegReplanningModule module)
	{
		this.leaveLinkReplanningModule = module;
	}
	
	public DuringLegReplanningModule getLeaveLinkReplanningModule()
	{
		return this.leaveLinkReplanningModule;
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e)
	{
		if (isInitialReplanning())
		{
			initialReplanningModule.doReplanning(Time.UNDEFINED_TIME);
		}
	}
	
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e)
	{
		if (isActEndReplanning())
		{
			actEndReplanningModule.doReplanning(e.getSimulationTime() + SimulationTimer.getSimTickTime());
		}
		
		if (isLeaveLinkReplanning())
		{
			leaveLinkReplanningModule.doReplanning(e.getSimulationTime() + SimulationTimer.getSimTickTime());
		}	
	}
}