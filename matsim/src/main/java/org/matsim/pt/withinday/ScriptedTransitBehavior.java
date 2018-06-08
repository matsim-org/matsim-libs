package org.matsim.pt.withinday;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public interface ScriptedTransitBehavior {

	public boolean enterVehicle(Person p, double time, TransitLine currentLine, TransitRoute currentRoute, List<TransitRouteStop> remainingStops,
								Leg plannedLeg, TransitRoute plannedRoute);
	
	
	static ScriptedTransitBehavior greedyBehavior() {
		return (pt, t, cl, cr, stops, lg, rt) -> true;
	}
	
}
