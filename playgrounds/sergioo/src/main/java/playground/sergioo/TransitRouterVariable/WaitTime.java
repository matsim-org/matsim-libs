package playground.sergioo.TransitRouterVariable;

import org.matsim.api.core.v01.Id;

public interface WaitTime {

	//Methods
	public double getRouteStopWaitTime(Id lineId, Id routeId, Id stopId, double time);

}
