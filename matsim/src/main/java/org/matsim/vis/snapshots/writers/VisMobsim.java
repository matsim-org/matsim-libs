/**
 * 
 */
package org.matsim.vis.snapshots.writers;


import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.MobsimAgent;
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
	
	void addSnapshotWriter(SnapshotWriter snapshotWriter) ;
	
	/**
	 * Returns mobsim agents, mostly for visualization (i.e. this may go to QSimI).  Open questions:<ul>
	 * <li>There is, at this point, no contract with respect to completeness
	 * of this function (i.e. what is contained and what not).</li>
	 * <li>This might be better as a Map.  However, as a Map all objects need to have an Id.  And the Id needs to be unique,
	 * since java does not have multi-maps.  Which means that we come back to the question of what should be in here and what not.
	 * For that reason, it is just a Collection.</li>
	 * </ul>  kai, aug'10
	 */
	Collection<MobsimAgent> getAgents() ;



}
