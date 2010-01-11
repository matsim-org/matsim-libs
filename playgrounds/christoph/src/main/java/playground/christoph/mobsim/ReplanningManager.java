package playground.christoph.mobsim;

import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.mobsim.queuesim.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.queuesim.listener.SimulationBeforeSimStepListener;

/*
 * This Class implements a QueueSimulationBeforeSimStepListener.
 * 
 * Each time a ListenerEvent is created it is checked
 * whether a WithinDayReplanning of the Agents Plans should
 * be done and / or is necessary.
 */
public class ReplanningManager implements SimulationBeforeSimStepListener{

	protected boolean actEndReplanning = false;
	protected boolean leaveLinkReplanning = false;

	protected ActEndReplanningModule actEndReplanningModule;
	protected LeaveLinkReplanningModule leaveLinkReplanningModule;
	
	public ReplanningManager()
	{
	}
	
	/*
	 * Using this Constructor is prefered. If the handed over
	 * Replanning Modules are not null, the replanning will be
	 * activated automatically!
	 */
	public ReplanningManager(ActEndReplanningModule actEndReplanningModule, LeaveLinkReplanningModule leaveLinkReplanningModule)
	{
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
	
	public void doActEndReplanning(boolean value)
	{
		actEndReplanning = value;
	}
	
	public boolean isActEndReplanning()
	{
		return actEndReplanning;
	}
	
	public void setActEndReplanningModule(ActEndReplanningModule module)
	{
		this.actEndReplanningModule = module;	
	}
	
	public ActEndReplanningModule getActEndReplanningModule()
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
	
	public void setLeaveLinkReplanningModule(LeaveLinkReplanningModule module)
	{
		this.leaveLinkReplanningModule = module;
	}
	
	public LeaveLinkReplanningModule getLeaveLinkReplanningModule()
	{
		return this.leaveLinkReplanningModule;
	}
	
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e)
	{
		if (isActEndReplanning())
		{
			actEndReplanningModule.doActEndReplanning(e.getSimulationTime() + SimulationTimer.getSimTickTime());
		}
		
		if (isLeaveLinkReplanning())
		{
			leaveLinkReplanningModule.doLeaveLinkReplanning(e.getSimulationTime() + SimulationTimer.getSimTickTime());
		}	
	}
}