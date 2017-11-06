/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;

/**
 * @author kainagel
 *
 */
public interface NetsimEngine {

	void registerAdditionalAgentOnLink(MobsimAgent planAgent);

	MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId);

	void addParkedVehicle(MobsimVehicle veh, Id<Link> linkId);

	NetsimNetwork getNetsimNetwork();

}
