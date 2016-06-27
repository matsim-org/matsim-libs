/**
 * 
 */
package org.matsim.vis.snapshotwriters;


import java.util.Collection;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.ObservableMobsim;

/**
 * @author nagel
 *
 */
public interface VisMobsim extends ObservableMobsim {

	VisNetwork getVisNetwork();

	/**
	 * Returns mobsim agents, for visualization. Open questions:<ul>
	 * <li>There is, at this point, no contract with respect to completeness
	 * of this function (i.e. what is contained and what not).</li>
	 * <li>This might be better as a Map.  However, as a Map all objects need to have an Id.  And the Id needs to be unique,
	 * since java does not have multi-maps.  Which means that we come back to the question of what should be in here and what not.
	 * For that reason, it is just a Collection.</li>
	 * </ul>  kai, aug'10
	 */
	Collection<MobsimAgent> getAgents();

	/**
	 * Returns a view of all agents to be visualized which are not on the VisNetwork.
	 * In the standard QSim, these are the teleporting agents.
	 */
	VisData getNonNetworkAgentSnapshots();

}
