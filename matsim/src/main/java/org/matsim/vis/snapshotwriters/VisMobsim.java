/**
 * 
 */
package org.matsim.vis.snapshotwriters;


import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * @author nagel
 *
 */
public interface VisMobsim {

	VisNetwork getVisNetwork();

	Scenario getScenario();
	
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

	Collection<? extends AgentSnapshotInfo> getNonNetwokAgentSnapshots();

}
