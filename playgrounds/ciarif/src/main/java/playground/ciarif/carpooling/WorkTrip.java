package playground.ciarif.carpooling;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;

public class WorkTrip {
	private Id tripId;
	private Id personId;
	private Coord homeCoord;
	private Coord workCoord;
	private double departureTime;
	private double travelTime;
	private double travelDistance;
	private boolean homeWork;

	public WorkTrip (Integer tripNumber, Id personId, Coord homeCoord, Coord workCoord, Leg homeWorkLeg, boolean homework){

		this.tripId = new IdImpl(tripNumber.toString());
		this.personId = personId;
		this.homeCoord = homeCoord;
		this.workCoord = workCoord;
		this.departureTime = homeWorkLeg.getDepartureTime();
		this.travelTime = homeWorkLeg.getTravelTime();
		this.travelDistance = homeWorkLeg.getRoute().getDistance();
		this.homeWork = homework;
	}

	public Id getPersonId() {
		return this.personId;
	}

	public Coord getHomeCoord() {
		return this.homeCoord;
	}

	public Coord getWorkCoord() {
		return this.workCoord;
	}

	public double getDepartureTime() {
		return this.departureTime;
	}
	public double getTravelTime() {
		return this.travelTime;
	}
	public double getTravelDistance() {
		return this.travelDistance;
	}
	public Id getTripId() {
		return this.tripId;
	}

	public boolean getHomeWork () {
		return this.homeWork;
	}
}
