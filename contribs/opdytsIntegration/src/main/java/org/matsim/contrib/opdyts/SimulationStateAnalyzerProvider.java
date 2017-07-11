package org.matsim.contrib.opdyts;

import org.matsim.core.events.handler.EventHandler;

import floetteroed.utilities.math.Vector;

/**
 * TODO rename this into ...Factory
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface SimulationStateAnalyzerProvider {

	// for plotting etc
	public String getStringIdentifier();
	
	public EventHandler newEventHandler();
	
	public Vector newStateVectorRepresentation();
	
	public void beforeIteration();
	
}
