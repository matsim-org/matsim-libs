package org.matsim.contrib.parking.parkingsearch.evaluation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class ParkingSlotManager {
	private List<Tuple<Coord, Double>> freeSlots = new ArrayList<Tuple<Coord, Double>>();
	private final Map<Id<Vehicle>, Tuple<Coord, Double>> occupiedSlots = new HashMap<Id<Vehicle>, Tuple<Coord, Double>>();
	private final Random r = MatsimRandom.getLocalInstance();
	private final Id<Link> linkID;

	public ParkingSlotManager(Link link, int numberOfSlotsOnLink) {
		for (Coord c : ParkingUtils.getEvenlyDistributedCoordsAlongLink(link, numberOfSlotsOnLink)) {
			this.freeSlots.add(new Tuple<Coord, Double>(c, 0.0));
		}
		this.linkID = link.getId();
	}

	/**
	 * @param timeOfParking
	 * @return tuple of the (now) occupied slot's coord and time point since it's been free
	 */
	public Tuple<Coord, Double> processParking(double timeOfParking, Id<Vehicle> vehID) {
		if (this.freeSlots.isEmpty())
			throw new RuntimeException(
				"all slots at link " + linkID.toString() + " already occupied. cannot display another occupied slot (" + vehID.toString() + ")");
		Tuple<Coord, Double> parkingTuple = this.freeSlots.remove(r.nextInt(this.freeSlots.size()));
		this.occupiedSlots.put(vehID, new Tuple<Coord, Double>(parkingTuple.getFirst(), timeOfParking));
		return parkingTuple;
	}

	public void setAllParkingTimesToZero() {
		List<Tuple<Coord, Double>> newFreeSlots = new ArrayList<Tuple<Coord, Double>>();
		for (Tuple<Coord, Double> t : this.freeSlots) {
			Coord c = t.getFirst();
			newFreeSlots.add(new Tuple<Coord, Double>(c, 0.0));
		}
		this.freeSlots = newFreeSlots;
		for (Id<Vehicle> id : this.occupiedSlots.keySet()) {
			Coord c = this.occupiedSlots.get(id).getFirst();
			this.occupiedSlots.put(id, new Tuple<Coord, Double>(c, 0.0));
		}
	}

	/**
	 * @param timeOfUnparking
	 * @return tuple of the (now) free slot's coord and time point since it's been occupied
	 */
	public Tuple<Coord, Double> processUnParking(double timeOfUnparking, Id<Vehicle> vehID) {
		Tuple<Coord, Double> parkingTuple;
		if (this.occupiedSlots.isEmpty() || !(this.occupiedSlots.containsKey(vehID))) {
//			throw new RuntimeException("or all slots already free or vehicle wasn't parked here.");
			if (freeSlots.isEmpty()) {
				return null;
			}
			parkingTuple = this.freeSlots.remove(r.nextInt(this.freeSlots.size()));
		} else {
			parkingTuple = this.occupiedSlots.remove(vehID);
		}
		this.freeSlots.add(new Tuple<Coord, Double>(parkingTuple.getFirst(), timeOfUnparking));
		return parkingTuple;
	}

	public Id<Link> getLinkId() {
		return this.linkID;
	}

	public List<Tuple<Coord, Double>> getFreeSlots() {
		return this.freeSlots;
	}

	public Map<Id<Vehicle>, Tuple<Coord, Double>> getOccupiedSlots() {
		return this.occupiedSlots;
	}
}
