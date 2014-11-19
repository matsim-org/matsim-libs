package playground.sergioo.passivePlanning2012.core.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;

public class EmptyTimeImpl extends LegImpl implements EmptyTime {

	//Attributes

	//Constructors
	public EmptyTimeImpl(Id<Link> startLinkId, double duration) {
		super("empty");
		setTravelTime(duration);
		setRoute(new GenericRouteImpl(startLinkId, startLinkId));
	}
	public EmptyTimeImpl(EmptyTime emptyTime) {
		super("empty");
		setTravelTime(emptyTime.getTravelTime());
		setRoute(new GenericRouteImpl(emptyTime.getRoute().getStartLinkId(), emptyTime.getRoute().getEndLinkId()));
	}

	//Methods

}
