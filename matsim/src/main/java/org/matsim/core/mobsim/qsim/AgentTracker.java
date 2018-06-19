/**
 * 
 */
package org.matsim.core.mobsim.qsim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author kainagel
 *
 */
public interface AgentTracker {

	List<PTPassengerAgent> getAgentsAtFacility(Id<? extends Facility> stopId);

}
