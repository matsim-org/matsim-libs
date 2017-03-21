/**
 * 
 */
package playground.tschlenther.parkingSearch.utilityBased;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.DynAgent.ParkingDynLeg;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * @author Work
 *
 */
public class UtilityBasedParkingDynLeg extends ParkingDynLeg {

	/**
	 * @param mode
	 * @param route
	 * @param logic
	 * @param parkingManager
	 * @param vehicleId
	 * @param timer
	 * @param events
	 */
	public UtilityBasedParkingDynLeg(String mode, NetworkRoute route, ParkingSearchLogic logic,
			ParkingSearchManager parkingManager, Id<Vehicle> vehicleId, MobsimTimer timer, EventsManager events) {
		super(mode, route, logic, parkingManager, vehicleId, timer, events);
		// TODO Auto-generated constructor stub
	}
	
	
	
	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		// TODO Auto-generated method stub
		super.movedOverNode(newLinkId);
	}
	
	@Override
	public Id<Link> getNextLinkId() {
		if (!parkingMode) {
			List<Id<Link>> linkIds = route.getLinkIds();

			if (currentLinkIdx == linkIds.size() - 1) {
				return route.getEndLinkId();
			}
			return linkIds.get(currentLinkIdx + 1);
		} else {
			if (hasFoundParking) {
				// easy, we can just park where at our destination link
				return null;
			} else {
				if (this.currentAndNextParkLink != null) {
					if (currentAndNextParkLink.getFirst().equals(currentLinkId)) {
						// we already calculated this
						return currentAndNextParkLink.getSecond();
					}
				}
				// need to find the next link
				Id<Link> nextLinkId = ((UtilityBasedParkingSearchLogic)this.logic).getNextLink(currentLinkId, route.getEndLinkId(), timer.getTimeOfDay());
				currentAndNextParkLink = new Tuple<Id<Link>, Id<Link>>(currentLinkId, nextLinkId);
				return nextLinkId;

			}
		}
	}

}
