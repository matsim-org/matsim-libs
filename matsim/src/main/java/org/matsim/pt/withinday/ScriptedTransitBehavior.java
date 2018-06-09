package org.matsim.pt.withinday;

import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


public interface ScriptedTransitBehavior {

	public boolean enterVehicle(Person p, double time, TransitLine currentLine, TransitRoute currentRoute, List<TransitRouteStop> remainingStops,
								Id<TransitStopFacility> goal, Leg plannedLeg, TransitRoute plannedRoute);
	
	
	static ScriptedTransitBehavior greedyBehavior() {
		return (agent, time, line, route, stops, goal, leg, plannedRoute) -> true;
	}
	
	static ScriptedTransitBehavior thresholdBehavior(double maxDeviation) {
		return (agent, time, line, route, stops, goal, leg, plannedRoute) -> {
			TransitRouteStop origin = route.getStops().get(route.getStops().size()-stops.size()-1);
			TransitRouteStop dest = stops.stream().filter(trs -> trs.getStopFacility().getId().equals(goal))
					                     .findFirst()
					                     .get();
			double plannedTime = dest.getArrivalOffset() - origin.getArrivalOffset();
			return plannedTime <= leg.getTravelTime() + maxDeviation;
		};
	}
	
	static ScriptedTransitBehavior javascriptBehavior(String script) {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
			engine.eval(script);
			
			if (engine instanceof Invocable) {
				Invocable inv = (Invocable) engine;
				return (agent, time, line, route, stops, goal, leg, plannedRoute) -> {
					try {
						Object res = inv.invokeFunction("enterVehicle", agent, time, line, route, stops, goal, leg, plannedRoute);
						if (res == null) {
							return false;
						}
						else if (res instanceof Boolean) {
							return (Boolean) res;
						}
						else {
							throw new IllegalStateException("Behavior script function 'enterVehicle' did not return a boolean, but an "+res.getClass());
						}
					}
					catch (NoSuchMethodException | ScriptException re) {
						throw new RuntimeException("Error while executing behavior script function 'enterVehicle'.", re);
					}
				};
			}
			else {
				throw new IllegalStateException("This is unexpected: the ScriptEngine is not of type Invocable, although it should be.");
			}
		}
		catch (ScriptException se) {
			throw new IllegalArgumentException("Could not evaluate javascript based ScriptedTransitBehavior");
		}
	}
	
}
