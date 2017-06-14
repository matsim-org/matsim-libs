package opdytsintegration;

import org.matsim.core.events.handler.EventHandler;

import floetteroed.utilities.math.Vector;

public interface SimulationStateAnalyzer {

	public EventHandler newEventHandler();
	
	public Vector newStateVectorRepresentation();
	
	public void beforeIteration();
	
}
