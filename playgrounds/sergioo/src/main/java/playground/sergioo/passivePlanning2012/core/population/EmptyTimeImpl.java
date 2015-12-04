package playground.sergioo.passivePlanning2012.core.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;

public class EmptyTimeImpl implements Leg,EmptyTime {
	private Leg delegate ;
	public EmptyTimeImpl(Id<Link> startLinkId, double duration) {
		delegate = new LegImpl("empty") ;
		delegate.setTravelTime(duration);
		delegate.setRoute(new GenericRouteImpl(startLinkId, startLinkId));
	}
	public EmptyTimeImpl(EmptyTime emptyTime) {
		delegate = new LegImpl("empty") ;
		delegate.setTravelTime(emptyTime.getTravelTime());
		delegate.setRoute(new GenericRouteImpl(emptyTime.getRoute().getStartLinkId(), emptyTime.getRoute().getEndLinkId()));
	}
	@Override
	public String getMode() {
		return this.delegate.getMode();
	}
	@Override
	public void setMode(String mode) {
		this.delegate.setMode(mode);
	}
	@Override
	public Route getRoute() {
		return this.delegate.getRoute();
	}
	@Override
	public void setRoute(Route route) {
		this.delegate.setRoute(route);
	}
	@Override
	public double getDepartureTime() {
		return this.delegate.getDepartureTime();
	}
	@Override
	public void setDepartureTime(double seconds) {
		this.delegate.setDepartureTime(seconds);
	}
	@Override
	public double getTravelTime() {
		return this.delegate.getTravelTime();
	}
	@Override
	public void setTravelTime(double seconds) {
		this.delegate.setTravelTime(seconds);
	}
}
