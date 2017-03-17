/**
 * 
 */
package playground.tschlenther.parkingSearch.memoryBased;

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
public class MemoryBasedParkingDynLeg extends ParkingDynLeg {

	/**
	 * @param mode
	 * @param route
	 * @param logic
	 * @param parkingManager
	 * @param vehicleId
	 * @param timer
	 * @param events
	 */
	public MemoryBasedParkingDynLeg(String mode, NetworkRoute route, ParkingSearchLogic logic,
			ParkingSearchManager parkingManager, Id<Vehicle> vehicleId, MobsimTimer timer, EventsManager events) {
		super(mode, route, logic, parkingManager, vehicleId, timer, events);
		// TODO Auto-generated constructor stub
	}
	
	
	


}
