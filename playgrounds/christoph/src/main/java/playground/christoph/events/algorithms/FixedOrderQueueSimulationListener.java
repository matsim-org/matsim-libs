package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;

/*
 * To avoid problems with the order of some
 * QueueSimulationListeners we collect them and
 * execute them in a fixed, given order. Even
 * something like ParallelListenerHandling
 * should not able to break the order. 
 */
public class FixedOrderQueueSimulationListener implements SimulationAfterSimStepListener, 
		SimulationInitializedListener, SimulationBeforeSimStepListener {

	List<SimulationBeforeSimStepListener> simulationBeforeSimStepListener;
	List<SimulationAfterSimStepListener> simulationAfterSimStepListener;
	List<SimulationInitializedListener> simulationInitializedListener;
	
	public FixedOrderQueueSimulationListener()
	{
		simulationBeforeSimStepListener = new ArrayList<SimulationBeforeSimStepListener>();
		simulationAfterSimStepListener = new ArrayList<SimulationAfterSimStepListener>();
		simulationInitializedListener = new ArrayList<SimulationInitializedListener>();
	}
	
	public void addQueueSimulationBeforeSimStepListener(SimulationBeforeSimStepListener listener)
	{
		simulationBeforeSimStepListener.add(listener);
	}
	
	public void removeQueueSimulationBeforeSimStepListener(SimulationBeforeSimStepListener listener)
	{
		simulationBeforeSimStepListener.remove(listener);
	}
	
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e)
	{
		for(SimulationBeforeSimStepListener listener : simulationBeforeSimStepListener)
		{
			listener.notifySimulationBeforeSimStep(e);
		}
	}
	
	public void addQueueSimulationAfterSimStepListener(SimulationAfterSimStepListener listener)
	{
		simulationAfterSimStepListener.add(listener);
	}
	
	public void removeQueueSimulationAfterSimStepListener(SimulationAfterSimStepListener listener)
	{
		simulationAfterSimStepListener.remove(listener);
	}

	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e)
	{
		for(SimulationAfterSimStepListener listener : simulationAfterSimStepListener)
		{
			listener.notifySimulationAfterSimStep(e);
		}
	}
	
	public void addQueueSimulationInitializedListener(SimulationInitializedListener listener)
	{
		simulationInitializedListener.add(listener);
	}
	
	public void removeQueueSimulationInitializedListener(SimulationInitializedListener listener)
	{
		simulationInitializedListener.remove(listener);
	}

	public void notifySimulationInitialized(SimulationInitializedEvent e)
	{
		for(SimulationInitializedListener listener : simulationInitializedListener)
		{
			listener.notifySimulationInitialized(e);
		}
	}
	
}