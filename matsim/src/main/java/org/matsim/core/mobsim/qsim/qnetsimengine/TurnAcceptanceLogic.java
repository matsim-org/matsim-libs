/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author kainagel
 *
 */
interface TurnAcceptanceLogic {

	boolean isAcceptingTurn(Link currentLink, Id<Link> nextLinkId, QLinkI nextQLink, QVehicle veh);

}
