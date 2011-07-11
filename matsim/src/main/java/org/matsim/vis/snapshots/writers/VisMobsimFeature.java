/**
 * 
 */
package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AdditionalTeleportationDepartureEventHandler;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;

/**This is a combination of capabilities that is needed to that the otfvis live mode can connect with the mobsim.  Historically,
 * this used to be the MobsimFeature.  kai, aug'10
 * 
 * @author nagel
 *
 */
public interface VisMobsimFeature 
extends 
SimulationInitializedListener, SimulationAfterSimStepListener, SimulationBeforeCleanupListener,
AgentArrivalEventHandler, AdditionalTeleportationDepartureEventHandler
{
	VisMobsim getVisMobsim() ;

	Plan findPlan(Id agentId);

	void addTrackedAgent(Id agentId);

	void removeTrackedAgent(Id agentId);
}
