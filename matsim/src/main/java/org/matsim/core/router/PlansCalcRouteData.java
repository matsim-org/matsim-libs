package org.matsim.core.router;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PlansCalcRouteData implements PlanAlgorithm {
	
	private final Map<String, LegRouter> legHandlers = new HashMap<String, LegRouter>();;

	public PlansCalcRouteData() {
	}

	public final void addLegHandler(String transportMode, LegRouter legHandler) {
		if (legHandlers.get(transportMode) != null) {
			PlansCalcRoute.log.warn("A LegHandler for " + transportMode + " legs is already registered - it is replaced!");
		}
		legHandlers.put(transportMode, legHandler);
	}
	
	@Override
	public void run(Plan plan) {
		routePlan(plan.getPerson(), plan);
	}

	void routePlan(Person person, final Plan plan) {
		double now = 0;
		// loop over all <act>s
		Activity fromAct = null;
		Activity toAct = null;
		Leg leg = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				leg = (Leg) pe;
			} else if (pe instanceof Activity) {
				if (fromAct == null) {
					fromAct = (Activity) pe;
				} else {
					toAct = (Activity) pe;
	
					double endTime = fromAct.getEndTime();
					double startTime = fromAct.getStartTime();
					double dur = (fromAct instanceof ActivityImpl ? ((ActivityImpl) fromAct).getMaximumDuration() : Time.UNDEFINED_TIME);
					if (endTime != Time.UNDEFINED_TIME) {
						// use fromAct.endTime as time for routing
						now = endTime;
					} else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
						// use fromAct.startTime + fromAct.duration as time for routing
						now = startTime + dur;
					} else if (dur != Time.UNDEFINED_TIME) {
						// use last used time + fromAct.duration as time for routing
						now += dur;
					} else {
						throw new RuntimeException("activity of plan of person " + plan.getPerson().getId().toString() + " has neither end-time nor duration." + fromAct.toString());
					}
	
					now += handleLeg(person, leg, fromAct, toAct, now);
	
					fromAct = toAct;
				}
			}
		}
	}

	double handleLeg(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		String legmode = leg.getMode();
		LegRouter legHandler = legHandlers.get(legmode);
		if (legHandler != null) {
			return legHandler.routeLeg(person, leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legmode + "'.");
		}
	}

}