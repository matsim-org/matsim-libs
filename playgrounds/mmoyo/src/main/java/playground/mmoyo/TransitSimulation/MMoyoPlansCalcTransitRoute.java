package playground.mmoyo.TransitSimulation;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.PTRouter.PTRouter;

/**copy of marcel.pt.router.PlansCalcTransitRoute.java to test the ptRouter in the simulation*/
public class MMoyoPlansCalcTransitRoute extends PlansCalcRoute {

	public final static String WAIT_ACTIVITY_TYPE = "wait pt";
	public final static String TRANSF_ACTIVITY_TYPE = "transf";
	public final static String ALIGHT_ACTIVITY_TYPE = "get off";
	public final static String TRANSIT_ACTIVITY_TYPE = "pt interaction";

	private final TransitActsRemover transitLegsRemover = new TransitActsRemover();
	private final TransitRouterConfig routerConfig = new TransitRouterConfig();
	private final PTRouter transitRouter;
	private final TransitConfigGroup transitConfig;
	private final TransitSchedule schedule;

	private Plan currentPlan = null;
	private final List<Tuple<Leg, List<Leg>>> legReplacements = new LinkedList<Tuple<Leg, List<Leg>>>();

	public MMoyoPlansCalcTransitRoute(final PlansCalcRouteConfigGroup config, final Network network,
			final TravelCost costCalculator, final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule,
			final TransitConfigGroup transitConfig) {
		super(config, network, costCalculator, timeCalculator, factory);

		this.schedule = schedule;
		this.transitConfig = transitConfig;
		this.transitRouter = new PTRouter(schedule, this.routerConfig);

		LeastCostPathCalculator routeAlgo = super.getLeastCostPathCalculator();
		if (routeAlgo instanceof Dijkstra) {
			((Dijkstra) routeAlgo).setModeRestriction(EnumSet.of(TransportMode.car));
		}
		routeAlgo = super.getPtFreeflowLeastCostPathCalculator();
		if (routeAlgo instanceof Dijkstra) {
			((Dijkstra) routeAlgo).setModeRestriction(EnumSet.of(TransportMode.car));
		}
	}

	@Override
	public void handlePlan(Person person, final Plan plan) {
		this.transitLegsRemover.run(plan);
		this.currentPlan = plan;
		this.legReplacements.clear();
		super.handlePlan(person, plan);
		this.replaceLegs();
		this.currentPlan = null;
	}

	@Override
	public double handleLeg(Person person, final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		if (this.transitConfig.getTransitModes().contains(leg.getMode())) {
			return this.handlePtPlan(leg, fromAct, toAct, depTime);
		}
		return super.handleLeg(person, leg, fromAct, toAct, depTime);
	}

	private double handlePtPlan(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		List<Leg> legs= this.transitRouter.calcRoute(fromAct, toAct, depTime);
		this.legReplacements.add(new Tuple<Leg, List<Leg>>(leg, legs));

		double travelTime = 0.0;
		if (legs != null) {
			for (Leg leg2 : legs) {
				travelTime += leg2.getTravelTime();
			}
		}
		return travelTime;
	}

	private void replaceLegs() {
		Iterator<Tuple<Leg, List<Leg>>> replacementIterator = this.legReplacements.iterator();
		if (!replacementIterator.hasNext()) {
			return;
		}
		List<PlanElement> planElements = this.currentPlan.getPlanElements();
		Tuple<Leg, List<Leg>> currentTuple = replacementIterator.next();
		for (int i = 0; i < this.currentPlan.getPlanElements().size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg == currentTuple.getFirst()) {
					// do the replacement
					if (currentTuple.getSecond() != null) {
						// first and last leg do not have the route set, as the start or end  link is unknown.
						Leg firstLeg = currentTuple.getSecond().get(0);
						Link fromLink = this.network.getLinks().get(((Activity) planElements.get(i-1)).getLinkId());
						Link lastActLink = fromLink;
						Link toLink = null;
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							toLink = this.network.getLinks().get(((RouteWRefs) currentTuple.getSecond().get(1).getRoute()).getStartLinkId());
						} else {
							toLink = this.network.getLinks().get(((Activity) planElements.get(i+1)).getLinkId());
						}
						Link nextPeLink = toLink;
						firstLeg.setRoute(new GenericRouteImpl(fromLink.getId(), toLink.getId()));

						Leg lastLeg = currentTuple.getSecond().get(currentTuple.getSecond().size() - 1);
						toLink = this.network.getLinks().get(((Activity) planElements.get(i+1)).getLinkId());
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							fromLink = this.network.getLinks().get(((RouteWRefs) currentTuple.getSecond().get(currentTuple.getSecond().size() - 2).getRoute()).getEndLinkId());   //fromLink es el ultimo link de los pt legs
						}

						//remove legs between a same link
						/*
						boolean sameLink = (lastActLink.getId().equals(nextPeLink.getId()));
						if (!sameLink){currentTuple.getSecond().get(0).setMode(TransportMode.transit_walk);}else{currentTuple.getSecond().remove(0);}
						if (fromLink.equals(toLink)) currentTuple.getSecond().remove(lastLeg);

						//PtRouter describe a transfer with a leg, it is also removed
						for (int ii=1; ii< currentTuple.getSecond().size()-1; ii++){
							if (!(currentTuple.getSecond().get(ii).getRoute() instanceof ExperimentalTransitRoute) && currentTuple.getSecond().get(ii-1).getRoute() instanceof ExperimentalTransitRoute && currentTuple.getSecond().get(ii+1).getRoute() instanceof ExperimentalTransitRoute) {
								currentTuple.getSecond().remove(ii);
								ii--;
							}
						}*/
						///////////////////

						lastLeg.setRoute(new GenericRouteImpl(fromLink.getId(), toLink.getId()));

						boolean isFirstLeg = true;
						Coord nextCoord = null;
						for (Leg leg2 : currentTuple.getSecond()) {
							if (isFirstLeg) {
								planElements.set(i, leg2);
								isFirstLeg = false;
							} else {
								i++;
								if (leg2.getRoute() instanceof ExperimentalTransitRoute) {

									ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg2.getRoute();   //
									ActivityImpl act = new ActivityImpl(TRANSIT_ACTIVITY_TYPE, this.schedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), tRoute.getStartLinkId());
									act.setDuration(0.0);
									planElements.add(i, act);
									nextCoord = this.schedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
								} else { // walk legs don't have a coord, use the coord from the last egress point
									ActivityImpl act = new ActivityImpl(TRANSIT_ACTIVITY_TYPE, nextCoord, leg2.getRoute().getStartLinkId());
									act.setDuration(0.0);
									planElements.add(i, act);
									leg2.setMode(TransportMode.transit_walk);
									//TODO:  set route distance
								}
								i++;
								planElements.add(i, leg2);
							}
						}
					}
					if (!replacementIterator.hasNext()) {
						return;
					}
					currentTuple = replacementIterator.next();
				}
			}
		}
	}
}
