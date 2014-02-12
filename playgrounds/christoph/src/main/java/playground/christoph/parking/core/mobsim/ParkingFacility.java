package playground.christoph.parking.core.mobsim;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class ParkingFacility {

	public final static String WAITING = "waiting";
	public final static String WAITINGCAPACITY = "waitingCapacity";
	
	private final Id facilityId;
	private final Id linkId;
	private final String type;
	private final int parkingCapacity;
	private final int waitingCapacity;
	private final Set<Id> reservedWaiting = new LinkedHashSet<Id>();
	private final Set<Id> waiting = new LinkedHashSet<Id>();
	private final Set<Id> reservedParking = new LinkedHashSet<Id>();
	private final Set<Id> occupied = new LinkedHashSet<Id>();

	public ParkingFacility(Id facilityId, Id linkId, String type, int parkingCapacity, int waitingCapacity) {
		this.facilityId = facilityId;
		this.linkId = linkId;
		this.type = type;
		this.parkingCapacity = parkingCapacity;
		this.waitingCapacity = waitingCapacity;
	}

	public Id getFaciltyId() {
		return this.facilityId;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public String getParkingType() {
		return this.type;
	}

	public boolean reservedToOccupied(Id id) {
		if (this.reservedParking.remove(id)) {
			return this.occupied.add(id);
		} else
			return false;
	}
	
	public boolean reservedToWaiting(Id id) {
		if (this.reservedWaiting.remove(id)) {
			return this.waiting.add(id);
		} else
			return false;
	}
	
	public boolean waitingToOccupied(Id id) {
		if (this.waiting.remove(id)) {
			return this.occupied.add(id);
		} else
			return false;
	}

	/*
	 * Returns the id of the agent which was moved from waiting to occupied or
	 * null if no agent was moved.
	 */
	public Id nextWaitingToOccupied() {
		Iterator<Id> iter = this.waiting.iterator();
		if (iter.hasNext()) {
			Id id = iter.next(); 
			if (this.occupied.add(id)) {
				iter.remove();
				return id;
			} else return null;
		} else return null;
	}
	
	public boolean release(Id id) {
		return this.occupied.remove(id);
	}

	public boolean reserveParking(Id id) {
		if (this.reservedParking.size() + this.occupied.size() < this.parkingCapacity) {
			return this.reservedParking.add(id);
		} else
			return false;
	}

	public boolean unReserveParking(Id id) {
		return this.reservedParking.remove(id);
	}

	public boolean reserveWaiting(Id id) {
		if (this.reservedWaiting.size() + this.waiting.size() < this.waitingCapacity) {
			return this.reservedWaiting.add(id);
		} else
			return false;
	}

	public boolean unReserveWaiting(Id id) {
		return this.reservedWaiting.remove(id);
	}
	
	public boolean addWaiting(Id id) {
		return this.waiting.add(id);
	}
	
	public boolean removeWaiting(Id id) {
		return this.waiting.remove(id);
	}
		
	public int getFreeWaitingCapacity() {
		return this.waitingCapacity - (this.reservedWaiting.size() +  this.waiting.size());
	}
	
	public int getFreeParkingCapacity() {
		return this.parkingCapacity - (this.reservedParking.size() + this.occupied.size());
	}

	public void reset() {

		if (this.reservedParking.size() > 0) {
			ParkingInfrastructure.log.warn("Found parking spots which are still reserved at the end of the simulation!");				
		}
		if (this.reservedWaiting.size() > 0) {
			ParkingInfrastructure.log.warn("Found waiting spots which are still reserved at the end of the simulation!");
		}

		this.reservedWaiting.clear();
		this.waiting.clear();
		this.reservedParking.clear();
		this.occupied.clear();
	}

	public int getParkingCapacity() {
		return this.parkingCapacity;
	}
	
	public int getWaitingCapacity() {
		return this.waitingCapacity;
	}
}