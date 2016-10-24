package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class GenericLeg implements Leg {
	private String mode;
	private Route route;
	private double departureTime;
	private double travelTime;
	
	public GenericLeg(String mode) {
		this.mode = mode;
	}
	
	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public Route getRoute() {
		return route;
	}

	@Override
	public void setRoute(Route route) {
		this.route = route;
	}

	@Override
	public double getDepartureTime() {
		return departureTime;
	}

	@Override
	public void setDepartureTime(double departureTime) {
		this.departureTime = departureTime;
	}

	@Override
	public double getTravelTime() {
		return travelTime;
	}

	@Override
	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
