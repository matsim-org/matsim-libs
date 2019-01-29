/**
 * 
 */
package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * @author kainagel
 *
 */
public interface AgentTracker {

	List<PTPassengerAgent> getAgentsAtFacility(Id<TransitStopFacility> stopId);

}
