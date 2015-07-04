package org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class Journey extends TravelComponent {
	private String trip_idx;
	private String mainmode = null;
	private Activity fromAct;
	private Activity toAct;
	private boolean carJourney = false;
	private boolean teleportJourney = false;
	private LinkedList<Trip> trips = new LinkedList<Trip>();
	private LinkedList<Transfer> transfers = new LinkedList<Transfer>();
	private LinkedList<Wait> waits = new LinkedList<Wait>();
	private LinkedList<Walk> walks = new LinkedList<Walk>();
	LinkedList<TravelComponent> planElements = new LinkedList<TravelComponent>();

	public Trip addTrip() {
		Trip trip = new Trip();
		trip.journey = this;
		getTrips().add(trip);
		planElements.add(trip);
		return trip;
	}

	public Wait addWait() {
		Wait wait = new Wait();
		wait.journey = this;
		getWaits().add(wait);
		planElements.add(wait);
		return wait;
	}

	public Walk addWalk() {
		Walk walk = new Walk();
		walk.journey = this;
		getWalks().add(walk);
		planElements.add(walk);
		return walk;
	}

	public void addTransfer(Transfer xfer) {
		xfer.journey = this;
		getTransfers().add(xfer);
		planElements.add(xfer);
	}

	private Coord orig;
	private Coord dest;
	private Transfer possibleTransfer;
	private double carDistance;

	public void incrementCarDistance(double increment) {
		carDistance += increment;
	}

	public String toString() {
		return String.format("JOURNEY: start: %6.0f end: %6.0f dur: %6.0f invehDist: %6.0f walkDist: %6.0f \n %s",
				getStartTime(), getEndTime(), getDuration(), getInVehDistance(), getWalkDistance(),
				planElements.toString());
	}

	public double getInVehDistance() {
		if(getMainMode().equals("walk"))
			return 0;
		if (!isCarJourney()) {
			double distance = 0;
			for (Trip t : getTrips()) {
				distance += t.getDistance();
			}
			return distance;
		}
		return carDistance;
	}

	double getWalkDistance() {
		if(getMainMode().equals("walk"))
			return walkSpeed * getDuration();
		if (!isCarJourney()) {
			double distance = 0;
			for (Walk w : getWalks()) {
				distance += w.getDistance();
			}
			return distance;
		}
		return 0;
	}

	public double getInVehTime() {
		if(getMainMode().equals("walk"))
			return 0;
		if (!isCarJourney()) {
			double time = 0;
			for (Trip t : getTrips()) {
				time += t.getDuration();
			}
			return time;
		}
		return getDuration();
	}

	double getWalkTime() {
		if (!isCarJourney()) {
			double time = 0;
			for (Walk w : getWalks()) {
				time += w.getDuration();
			}
			return time;
		}
		return 0;
	}

	double getWaitTime() {
		if (!isCarJourney()) {
			double time = 0;
			for (Wait w : getWaits()) {
				time += w.getDuration();
			}
			return time;
		}
		return 0;
	}

	public String getMainMode() {
		if (!(mainmode == null)) {
			return mainmode;
		}
		if (isCarJourney()) {
			return "car";
		}
		try {
			Trip longestTrip = getTrips().getFirst();
			if (getTrips().size() > 1) {
				for (int i = 1; i < getTrips().size(); i++) {
					if (getTrips().get(i).getDistance() > longestTrip.getDistance()) {
						longestTrip = getTrips().get(i);
					}
				}
			}
			return longestTrip.getMode();

		} catch (NoSuchElementException e) {
			return "walk";

		}
	}

	public double getDistance() {

		return getInVehDistance() + getWalkDistance();
	}

	public double getAccessWalkDistance() {

		try {
			return getWalks().getFirst().getDistance();
		} catch (NoSuchElementException e) {
			return 0;
		}
	}

	public double getAccessWalkTime() {

		try {
			return getWalks().getFirst().getDuration();
		} catch (NoSuchElementException e) {
			return 0;
		}

	}

	public double getAccessWaitTime() {

			try {
				return getWaits().getFirst().getDuration();

			} catch (NoSuchElementException e) {
				return 0;
			}

	}

	public double getEgressWalkDistance() {
		try{
			for (Walk w : getWalks()) {
				if (w.isEgressWalk())
					return w.getDistance();
			}
		}catch(Exception e){
			return 0;
		}
		return 0;
		
	}

	public double getEgressWalkTime() {
		try{
			for (Walk w : getWalks()) {
				if (w.isEgressWalk())
					return w.getDuration();
			}
		}catch(Exception e){
			return 0;
		}
		return 0;
	}

	public Activity getFromAct() {
		return fromAct;
	}

	public void setFromAct(Activity fromAct) {
		this.fromAct = fromAct;
	}

	public Activity getToAct() {
		return toAct;
	}

	public void setToAct(Activity toAct) {
		this.toAct = toAct;
	}

	public boolean isCarJourney() {
		return carJourney;
	}

	public void setCarJourney(boolean carJourney) {
		this.carJourney = carJourney;
	}

	public LinkedList<Trip> getTrips() {
		return trips;
	}

	public void setTrips(LinkedList<Trip> trips) {
		this.trips = trips;
	}

	public LinkedList<Transfer> getTransfers() {
		return transfers;
	}

	public void setTransfers(LinkedList<Transfer> transfers) {
		this.transfers = transfers;
	}

	public LinkedList<Walk> getWalks() {
		return walks;
	}

	public void setWalks(LinkedList<Walk> walks) {
		this.walks = walks;
	}

	public Coord getDest() {
		return dest;
	}

	public void setDest(Coord dest) {
		this.dest = dest;
	}

	public Coord getOrig() {
		return orig;
	}

	public void setOrig(Coord orig) {
		this.orig = orig;
	}

	public Transfer getPossibleTransfer() {
		return possibleTransfer;
	}

	public void setPossibleTransfer(Transfer possibleTransfer) {
		this.possibleTransfer = possibleTransfer;
	}

	public LinkedList<Wait> getWaits() {
		return waits;
	}

	public void setWaits(LinkedList<Wait> waits) {
		this.waits = waits;
	}

	public void setMainmode(String mainmode) {
		this.mainmode = mainmode;
	}

	public String getTrip_idx() {
		return trip_idx;
	}

	public void setTrip_idx(String trip_idx) {
		this.trip_idx = trip_idx;
	}

	public double getCarDistance() {
		return carDistance;
	}

	public void setCarDistance(double carDistance) {
		this.carDistance = carDistance;
	}

	public double getTransferWalkDistance() {
		if (!isCarJourney()) {
			double walkDistance = 0;
			for (Transfer t : this.getTransfers()) {
				walkDistance += t.getWalkDistance();
			}
			return walkDistance;
		}
		return 0;
	}

	public double getTransferWalkTime() {
		if (!isCarJourney()) {
			double walkTime = 0;
			for (Transfer t : this.getTransfers()) {
				walkTime += t.getWalkTime();
			}
			return walkTime;
		}
		return 0;
	}

	public double getTransferWaitTime() {
		if (!isCarJourney()) {
			double waitTime = 0;
			for (Transfer t : this.getTransfers()) {
				waitTime += t.getWaitTime();
			}
			return waitTime;
		}
		return 0;
	}

	public Id getFirstBoardingStop() {
		if (!isCarJourney() && this.getTrips().size() > 0) {
			return this.getTrips().getFirst().getBoardingStop();
		}
		return null;
	}

	public Id getLastAlightingStop() {
		if (!isCarJourney() && this.getTrips().size() > 0) {
			return this.getTrips().getLast().getAlightingStop();
		}
		return null;
	}

	public boolean isTeleportJourney() {
		return teleportJourney;
	}

	public void setTeleportJourney(boolean teleportJourney) {
		this.teleportJourney = teleportJourney;
	}

	public static void setWalkSpeed(double walkSpeed) {
		Journey.walkSpeed = walkSpeed;
	}
}