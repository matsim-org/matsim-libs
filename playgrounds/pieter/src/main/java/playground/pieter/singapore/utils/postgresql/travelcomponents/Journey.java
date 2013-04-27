package playground.pieter.singapore.utils.postgresql.travelcomponents;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.matsim.api.core.v01.Coord;

public class Journey extends TravelComponent {
	private String mainmode;
	private Activity fromAct;
	private Activity toAct;
	private boolean carJourney = false;
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
	 double carDistance;
	 public void incrementCarDistance(double increment){
		 carDistance += increment;
	 }
	public String toString() {
		return String
				.format("JOURNEY: start: %6.0f end: %6.0f dur: %6.0f invehDist: %6.0f walkDist: %6.0f \n %s",
						getStartTime(), getEndTime(), getDuration(),
						getInVehDistance(), getWalkDistance(),
						planElements.toString());
	}

	 public double getInVehDistance() {
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
		if (!isCarJourney()) {
			return getWalks().getFirst().getDistance();
		}
		return 0;
	}

	public double getAccessWalkTime() {
		if (!isCarJourney()) {
			return getWalks().getFirst().getDuration();
		}
		return 0;
	}

	public double getAccessWaitTime() {
		if (!isCarJourney()) {
			try {
				return getWaits().getFirst().getDuration();

			} catch (NoSuchElementException e) {

			}
		}
		return 0;
	}

	public double getEgressWalkDistance() {
		if (!isCarJourney()) {
			for (Walk w : getWalks()) {
				if (w.isEgressWalk())
					return w.getDistance();
			}
		}
		return 0;
	}

	public double getEgressWalkTime() {
		if (!isCarJourney()) {
			for (Walk w : getWalks()) {
				if (w.isEgressWalk())
					return w.getDuration();
			}
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

	public String getMainmode() {
		return mainmode;
	}

	public void setMainmode(String mainmode) {
		this.mainmode = mainmode;
	}
}