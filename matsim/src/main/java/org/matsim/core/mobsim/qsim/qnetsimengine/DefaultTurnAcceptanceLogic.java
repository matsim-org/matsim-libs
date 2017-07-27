/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author kainagel
 *
 */
final class DefaultTurnAcceptanceLogic implements TurnAcceptanceLogic {
	private static final Logger log = Logger.getLogger( DefaultTurnAcceptanceLogic.class) ;
	
	@Override
	public boolean isAcceptingTurn(Link currentLink, Id<Link> nextLinkId, QLinkI nextQLink, QVehicle veh){
		// we need nextLinkId and nextQLink, because the link lookup may have failed and then nextQLink is null
		if (nextQLink == null){
			log.warn("The link id " + nextLinkId + " is not available in the simulation network, but vehicle " + veh.getId() + 
					" plans to travel on that link from link " + veh.getCurrentLink().getId());
			return false ;
		}
		if (currentLink.getToNode() != nextQLink.getLink().getFromNode()) {
			log.warn("Cannot move vehicle " + veh.getId() + " from link " + currentLink.getId() + " to link " + nextQLink.getLink().getId());
			return false ;
		}
//		if ( !nextQLink.getLink().getAllowedModes().contains( veh.getDriver().getMode() ) ) {
//			final String message = "The link with id " + nextLinkId + " does not allow the current mode, which is " + veh.getDriver().getMode();
//			throw new RuntimeException( message ) ;
////			log.warn(message );
////			return false ;
//			// yyyy is rather nonsensical to get the mode from the driver, not from the vehicle.  However, this seems to be 
//			// how it currently works: network links are defined for modes, not for vehicle types.  kai, may'16
//		}
		// currently does not work, see MATSIM-533 
		
		return true ;
	}

}
