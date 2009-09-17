package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationBeforeSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;

/*
 * To avoid problems with the order of some
 * QueueSimulationListeners we collect them and
 * execute them in a fixed, given order. Even
 * something like ParallelListenerHandling
 * should not able to break the order. 
 */
public class FixedOrderQueueSimulationListener implements QueueSimulationAfterSimStepListener, 
		QueueSimulationInitializedListener, QueueSimulationBeforeSimStepListener {

	List<QueueSimulationBeforeSimStepListener> queueSimulationBeforeSimStepListener;
	List<QueueSimulationAfterSimStepListener> queueSimulationAfterSimStepListener;
	List<QueueSimulationInitializedListener> queueSimulationInitializedListener;
	
	public FixedOrderQueueSimulationListener()
	{
		queueSimulationBeforeSimStepListener = new ArrayList<QueueSimulationBeforeSimStepListener>();
		queueSimulationAfterSimStepListener = new ArrayList<QueueSimulationAfterSimStepListener>();
		queueSimulationInitializedListener = new ArrayList<QueueSimulationInitializedListener>();
	}
	
	public void addQueueSimulationBeforeSimStepListener(QueueSimulationBeforeSimStepListener listener)
	{
		queueSimulationBeforeSimStepListener.add(listener);
	}
	
	public void removeQueueSimulationBeforeSimStepListener(QueueSimulationBeforeSimStepListener listener)
	{
		queueSimulationBeforeSimStepListener.remove(listener);
	}
	
	public void notifySimulationBeforeSimStep(QueueSimulationBeforeSimStepEvent e)
	{
		for(QueueSimulationBeforeSimStepListener listener : queueSimulationBeforeSimStepListener)
		{
			listener.notifySimulationBeforeSimStep(e);
		}
	}
	
	public void addQueueSimulationAfterSimStepListener(QueueSimulationAfterSimStepListener listener)
	{
		queueSimulationAfterSimStepListener.add(listener);
	}
	
	public void removeQueueSimulationAfterSimStepListener(QueueSimulationAfterSimStepListener listener)
	{
		queueSimulationAfterSimStepListener.remove(listener);
	}

	public void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e)
	{
		for(QueueSimulationAfterSimStepListener listener : queueSimulationAfterSimStepListener)
		{
			listener.notifySimulationAfterSimStep(e);
		}
	}
	
	public void addQueueSimulationInitializedListener(QueueSimulationInitializedListener listener)
	{
		queueSimulationInitializedListener.add(listener);
	}
	
	public void removeQueueSimulationInitializedListener(QueueSimulationInitializedListener listener)
	{
		queueSimulationInitializedListener.remove(listener);
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e)
	{
		for(QueueSimulationInitializedListener listener : queueSimulationInitializedListener)
		{
			listener.notifySimulationInitialized(e);
		}
	}
	
}