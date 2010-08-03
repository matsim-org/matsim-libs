package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.interfaces.QSimI;

/*
 * To avoid problems with the order of some QueueSimulationListeners we 
 * collect them and execute them in a fixed, given order. Even something 
 * like ParallelListenerHandling should not able to break the order. 
 */
public class FixedOrderQueueSimulationListener implements SimulationInitializedListener<QSimI>,
	SimulationBeforeSimStepListener<QSimI>, SimulationAfterSimStepListener<QSimI>, 
	SimulationBeforeCleanupListener<QSimI> {

	List<SimulationInitializedListener<QSimI>> simulationInitializedListener;
	List<SimulationBeforeSimStepListener<QSimI>> simulationBeforeSimStepListener;
	List<SimulationAfterSimStepListener<QSimI>> simulationAfterSimStepListener;
	List<SimulationBeforeCleanupListener<QSimI>> simulationBeforeCleanupListener;
	
	public FixedOrderQueueSimulationListener() {
		simulationInitializedListener = new ArrayList<SimulationInitializedListener<QSimI>>();
		simulationBeforeSimStepListener = new ArrayList<SimulationBeforeSimStepListener<QSimI>>();
		simulationAfterSimStepListener = new ArrayList<SimulationAfterSimStepListener<QSimI>>();
		simulationBeforeCleanupListener = new ArrayList<SimulationBeforeCleanupListener<QSimI>>();
	}
	
	public void addQueueSimulationInitializedListener(SimulationInitializedListener<QSimI> listener) {
		simulationInitializedListener.add(listener);
	}
	
	public void removeQueueSimulationInitializedListener(SimulationInitializedListener<QSimI> listener) {
		simulationInitializedListener.remove(listener);
	}

	public void notifySimulationInitialized(SimulationInitializedEvent<QSimI> e) {
		for(SimulationInitializedListener<QSimI> listener : simulationInitializedListener) {
			listener.notifySimulationInitialized(e);
		}
	}
	
	public void addQueueSimulationBeforeSimStepListener(SimulationBeforeSimStepListener<QSimI> listener) {
		simulationBeforeSimStepListener.add(listener);
	}
	
	public void removeQueueSimulationBeforeSimStepListener(SimulationBeforeSimStepListener<QSimI> listener) {
		simulationBeforeSimStepListener.remove(listener);
	}
	
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent<QSimI> e) {
		for(SimulationBeforeSimStepListener<QSimI> listener : simulationBeforeSimStepListener) {
			listener.notifySimulationBeforeSimStep(e);
		}
	}
	
	public void addQueueSimulationAfterSimStepListener(SimulationAfterSimStepListener<QSimI> listener) {
		simulationAfterSimStepListener.add(listener);
	}
	
	public void removeQueueSimulationAfterSimStepListener(SimulationAfterSimStepListener<QSimI> listener) {
		simulationAfterSimStepListener.remove(listener);
	}

	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent<QSimI> e) {
		for(SimulationAfterSimStepListener<QSimI> listener : simulationAfterSimStepListener) {
			listener.notifySimulationAfterSimStep(e);
		}
	}

	public void addQueueSimulationBeforeCleanupListener(SimulationBeforeCleanupListener<QSimI> listener) {
		simulationBeforeCleanupListener.add(listener);
	}
	
	public void removeQueueSimulationBeforeCleanupListener(SimulationBeforeCleanupListener<QSimI> listener) {
		simulationBeforeCleanupListener.remove(listener);
	}
	
	public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSimI> e) {
		for(SimulationBeforeCleanupListener<QSimI> listener : simulationBeforeCleanupListener) {
			listener.notifySimulationBeforeCleanup(e);
		}
	}
	
}