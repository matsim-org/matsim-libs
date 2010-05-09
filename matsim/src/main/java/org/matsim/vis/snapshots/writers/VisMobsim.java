/**
 * 
 */
package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;

/**
 * @author nagel
 *
 */
public interface VisMobsim extends ObservableSimulation, IOSimulation {

	VisNetwork getVisNetwork() ;
	// yyyy one could, possibly, use "getMobsimNetwork", with a superclass return type, that then,
	// in fact, also works for the sub-types.  Same is true in the other VisXXX-types.  kai, may'10
	
	Scenario getScenario() ;
	
	EventsManager getEventsManager() ;

}
