package playground.andreas.intersection.sim;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author dstrippgen
 *
 * Comparator object, to sort the Vehicle objects in QueueLink.parkingList according
 * to their departure time
 */
public class VehicleDepartureTimeComparator implements Comparator<QVehicle>, Serializable {
	private static final long serialVersionUID = 1L;

	public int compare(final QVehicle veh1, final QVehicle veh2) {
		if (veh1.getDepartureTime_s() > veh2.getDepartureTime_s())
			return 1;
		if (veh1.getDepartureTime_s() < veh2.getDepartureTime_s())
			return -1;

		// Both depart at the same time -> let the one with the larger id be first
		if (veh1.getID() < veh2.getID())
			return 1;
		if (veh1.getID() > veh2.getID())
			return -1;
		return 0;
	}
}
