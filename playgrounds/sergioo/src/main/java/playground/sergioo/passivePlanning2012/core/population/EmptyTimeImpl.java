package playground.sergioo.passivePlanning2012.core.population;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;

public class EmptyTimeImpl extends LegImpl implements EmptyTime {

	//Attributes

	//Constructors
	public EmptyTimeImpl(Id startLinkId) {
		super("empty");
		setRoute(new GenericRouteImpl(startLinkId, null));
	}

	//Methods

}
