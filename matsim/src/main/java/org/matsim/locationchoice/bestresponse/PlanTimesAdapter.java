package org.matsim.locationchoice.bestresponse;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.geometry.CoordImpl;

public class PlanTimesAdapter {
	
	// 0: complete routing
	// 1: local routing
	// 2: no routing (distance-based approximation)
	private int approximationLevel = 0; 
	private LeastCostPathCalculator leastCostPathCalculatorForward = null;
	private LeastCostPathCalculator leastCostPathCalculatorBackward = null;
	private Network network;
	private Config config;
	
	private static final Logger log = Logger.getLogger(PlanTimesAdapter.class);
		
	public PlanTimesAdapter(int approximationLevel, LeastCostPathCalculator leastCostPathCalculatorForward, LeastCostPathCalculator leastCostPathCalculatorBackward, Network network, Config config) {
		this.approximationLevel = approximationLevel;
		this.leastCostPathCalculatorForward = leastCostPathCalculatorForward;
		this.leastCostPathCalculatorBackward = leastCostPathCalculatorBackward;
		this.network = network;
		this.config = config;
	}
	
	public void adaptAndScoreTimes(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction,
			PlansCalcRoute router) {	
		
		Activity actToMove = (Activity) plan.getPlanElements().get(actlegIndex);
		
		// iterate through plan and adapt travel and activity times
		int planElementIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			planElementIndex++;
			if (pe instanceof Activity) {
				if (planElementIndex == 0) {
					scoringFunction.startActivity(0.0, (ActivityImpl)pe);
					scoringFunction.endActivity(((ActivityImpl)pe).getEndTime());
					continue; 
				}
				else {
					double legTravelTime = 0.0;
					if (approximationLevel == 0) {
						legTravelTime = this.computeTravelTime(plan.getPerson(),
							((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).getPreviousLeg((ActivityImpl)pe)), 
							(ActivityImpl)pe, router);
					}
					else if (approximationLevel == 1){
						if (planElementIndex == actlegIndex) {
							// adapt travel times: actPre -> actToMove
							Activity previousActivity = ((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).
									getPreviousLeg((ActivityImpl)pe));
							
							Node fromNode = network.getLinks().get(previousActivity.getLinkId()).getToNode();
							Node toNode = network.getLinks().get(actToMove.getLinkId()).getToNode();
							
							Path p = this.leastCostPathCalculatorForward.calcLeastCostPath(fromNode, toNode, previousActivity.getEndTime());
							legTravelTime = p.travelTime;
							
							//log.info("planElementIndex: " + planElementIndex + "------------");
							//log.info("	Forward travel time: " + legTravelTime / 60.0);
							
						}
						else if (planElementIndex == (actlegIndex + 2)) {
							//adapt travel times: actToMove -> actPost
							// replaced approximation: legTravelTime = getTravelTimeApproximation((PlanImpl)plan, (ActivityImpl)pe);
							
							Node fromNode = network.getLinks().get(((ActivityImpl)pe).getLinkId()).getToNode();
							Node toNode = network.getLinks().get(actToMove.getLinkId()).getToNode();
																					
							Path p = this.leastCostPathCalculatorBackward.calcLeastCostPath(fromNode, toNode, -1.0);
							legTravelTime = p.travelTime;
							//log.info("	Backward travel time: " + legTravelTime / 60.0);
						}
						else {
							//use travel times from last iteration for efficiency reasons
							legTravelTime = ((PlanImpl)plan).getPreviousLeg((Activity)pe).getTravelTime();
						}
					}
					else if (approximationLevel == 2) {
						if (planElementIndex ==  actlegIndex || planElementIndex == (actlegIndex + 2)) {
							legTravelTime = getTravelTimeApproximation((PlanImpl)plan, (ActivityImpl)pe);
						}
						else {
							//use travel times from last iteration for efficiency reasons
							legTravelTime = ((PlanImpl)plan).getPreviousLeg((Activity)pe).getTravelTime();
						}
					}
					
					LegImpl previousLegPlan = (LegImpl) ((PlanImpl)plan).getPreviousLeg((ActivityImpl)pe);
					double actDur = ((ActivityImpl)pe).getEndTime() - (previousLegPlan.getDepartureTime() + previousLegPlan.getTravelTime());
										
					// set new leg travel time, departure time and arrival time
					LegImpl previousLegPlanTmp = (LegImpl)planTmp.getPlanElements().get(planElementIndex -1);
					previousLegPlanTmp.setTravelTime(legTravelTime);
					double departureTime = ((ActivityImpl)planTmp.getPlanElements().get(planElementIndex -2)).getEndTime();
					previousLegPlanTmp.setDepartureTime(departureTime);
					double arrivalTime = departureTime + legTravelTime;
					previousLegPlanTmp.setArrivalTime(arrivalTime);
					
					// set new activity end time
					ActivityImpl act = (ActivityImpl) planTmp.getPlanElements().get(planElementIndex);
					act.setEndTime(arrivalTime + actDur);
										
					scoringFunction.startActivity(arrivalTime, act);
					scoringFunction.endActivity(arrivalTime + actDur);
				}				
			}
		}
	}
	
	private double getTravelTimeApproximation(PlanImpl plan, ActivityImpl pe) {
		Activity actPre = plan.getPreviousActivity(plan.getPreviousLeg(pe));
		double distance = 1.5 * ((CoordImpl)actPre.getCoord()).calcDistance(pe.getCoord());
		// TODO: get better speed estimation
		double speed = Double.parseDouble(this.config.locationchoice().getRecursionTravelSpeed());
		return distance / speed;
	}
	
	private double computeTravelTime(Person person, Activity fromAct, Activity toAct, PlansCalcRoute router) {
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);
		router.handleLeg(person, leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravelTime();
	}

	public LeastCostPathCalculator getLeastCostPathCalculator() {
		return leastCostPathCalculatorForward;
	}

	public void setLeastCostPathCalculator(
			LeastCostPathCalculator leastCostPathCalculator) {
		this.leastCostPathCalculatorForward = leastCostPathCalculator;
	}

	public LeastCostPathCalculator getLeastCostPathCalculatorBackward() {
		return leastCostPathCalculatorBackward;
	}

	public void setLeastCostPathCalculatorBackward(
			LeastCostPathCalculator leastCostPathCalculatorBackward) {
		this.leastCostPathCalculatorBackward = leastCostPathCalculatorBackward;
	}
}
